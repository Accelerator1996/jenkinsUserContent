import groovy.json.JsonBuilder

properties[
        [$class: 'JiraProjectProperty'],
        [$class: 'RebuildSettings', autoRebuild: false, rebuildDisabled: false],
]
OSS_TOOL = "/home/testuser/ossutil64"
TOKEN = "d42e8b7b84e091709eacce0a4d2c97621eda800c"
RELEASE_MAP = [:]
CHECKSUM_MAP = [:]

if (params.RELEASE == "8") {
    PARENT_JOB_NAME = ""
    JDK_NAME = ""
    PLATFORMS = ["x64_linux", "x64_windows", "aarch64_linux"]
    REPO = "dragonwell8"
    HEAD = "OpenJDK8U-jdk_"
    BUILDER = "http://ci.dragonwell-jdk.io/userContent/utils/build.sh"
} else if (params.RELEASE == "11") {
    PARENT_JOB_NAME = ""
    JDK_NAME = ""
    PLATFORMS = ["x64_linux", "x64_windows", "x64_alpine-linux", "aarch64_linux"]
    REPO = "dragonwell11"
    HEAD = "OpenJDK11U-jdk_"
    BUILDER = "http://ci.dragonwell-jdk.io/userContent/utils/build11.sh"
} else {
    PARENT_JOB_NAME = ""
    JDK_NAME = ""
    PLATFORMS = ["x64_linux", "x64_windows", "x64_alpine-linux", "aarch64_linux"]
    REPO = "dragonwell17"
    HEAD = "OpenJDK17-jdk"
    BUILDER = "http://ci.dragonwell-jdk.io/userContent/utils/build17.sh"
}

pipeline {
    options {
        durabilityHint('PERFORMANCE_OPTIMIZED')
        buildDiscarder(logRotator(numToKeepStr: '15', artifactDaysToKeepStr: '15'))
    }
    agent {
        label 'master'
    }
    parameters {
        choice(name: 'RELEASE', choices: '17\n11\n8\n',
                description: 'Use which Multiplexing')
        string(name: 'VERSION',
                defaultValue: "11.0.10.5",
                description: 'Pipeline build Number')
        string(name: 'GITHUBTAG',
                defaultValue: "11.0.10.5-GA",
                description: 'Github tag')
        string(name: 'BUILDNUMBER',
                defaultValue: "latest",
                description: 'Build number')
        booleanParam(defaultValue: false,
                description: 'copy release oss',
                name: 'OSS')
        booleanParam(defaultValue: false,
                description: 'release on github',
                name: 'GITHUB')
        booleanParam(defaultValue: true,
                description: 'release on docker',
                name: 'DOCKER')
        booleanParam(defaultValue: true,
                description: 'clean workspace',
                name: 'CLEAN')

    }
    environment {
        HSF_HOME = "/home/admin/hsf/benchmark"
    }
    stages {
        stage('publishOssGithub') {
            when {
                // Only say hello if a "greeting" is requested
                expression { params.OSS == true }
            }
            agent {
                label 'ossworker'
            }
            steps {
                script {
                    if (params.CLEAN) {
                        sh "rm -rf /home/testuser/jenkins/workspace/dragonwell-oss-installer/workspace/target/"
                        copyArtifacts(
                                projectName: "build-scripts/openjdk${params.RELEASE}-pipeline",
                                filter: "**/${HEAD}*dragonwell*tar.gz*",
                                selector: specific("${params.BUILDNUMBER}"),
                                fingerprintArtifacts: true,
                                target: "workspace/target/",
                                flatten: true)
                        copyArtifacts(
                                projectName: "build-scripts/openjdk${params.RELEASE}-pipeline",
                                filter: "**/${HEAD}*dragonwell*zip*",
                                selector: specific("${params.BUILDNUMBER}"),
                                fingerprintArtifacts: true,
                                target: "workspace/target/",
                                flatten: true)
                    }
                    dir("workspace/target/") {
                        files = sh(script: 'ls', returnStdout: true).split()
                        for (String platform : PLATFORMS) {
                            def tailPattern = (platform != "x64_windows") ? "tar.gz" : "zip"
                            def tarPattern = ".+${platform}.+${tailPattern}"
                            def checkPattern = ".+${platform}.+sha256.txt"
                            for (String file : files) {
                                final p = file =~ /${tarPattern}/
                                final checksum = file =~ /${checkPattern}/
                                if (p.matches()) {
                                    if (platform == "x64_windows") {
                                        platform = "x86_windows"
                                    }
                                    def releaseFile = "Alibaba_Dragonwell_${params.VERSION}_${platform}.${tailPattern}"
                                    sh "mv ${file} ${releaseFile}"
                                    sh "${OSS_TOOL} cp -f ${releaseFile} oss://dragonwell/${params.VERSION}/${releaseFile}"
                                    print "https://dragonwell.oss-cn-shanghai.aliyuncs.com/${params.VERSION}/${releaseFile}"
                                    RELEASE_MAP["${releaseFile}"] = "https://dragonwell.oss-cn-shanghai.aliyuncs.com/${params.VERSION}/${releaseFile}"
                                } else if (checksum.matches()) {
                                    if (platform == "x64_windows") {
                                        platform = "x86_windows"
                                    }
                                    def releaseFile = "Alibaba_Dragonwell_${params.VERSION}_${platform}.${tailPattern}.sha256.txt"
                                    sh "mv ${file} ${releaseFile}"
                                    sh "${OSS_TOOL} cp -f ${releaseFile} oss://dragonwell/${params.VERSION}/${releaseFile}"
                                    print "https://dragonwell.oss-cn-shanghai.aliyuncs.com/${params.VERSION}/${releaseFile}"
                                    CHECKSUM_MAP["${releaseFile}"] = "https://dragonwell.oss-cn-shanghai.aliyuncs.com/${params.VERSION}/${releaseFile}"
                                }
                            }
                        }
                        if (params.GITHUB) {
                            def releaseJson = new JsonBuilder([
                                    "tag_name"        : "${params.GITHUBTAG}",
                                    "target_commitish": "master",
                                    "name"            : "Alibaba_Dragonwell_${params.VERSION}",
                                    "body"            : "",
                                    "draft"           : true,
                                    "prerelease"      : true
                            ])
                            writeFile file: 'release.json', text: releaseJson.toPrettyString()
                            def release = sh(script: "curl -XPOST -H \"Authorization:token ${TOKEN}\" --data @release.json https://api.github.com/repos/alibaba/${REPO}/releases", returnStdout: true)
                            writeFile file: 'result.json', text: "${release}"
                            def id = sh(script: "cat result.json | grep id | head -n 1 | awk -F\"[:,]\"  '{ print \$2 }' | awk '{print \$1}'", returnStdout: true)
                            id = id.trim()
                            for (file in RELEASE_MAP) {
                                def assetName = file.key
                                sh "curl -Ss -XPOST -H \"Authorization:token ${TOKEN}\" -H \"Content-Type:application/zip\" --data-binary @${assetName} https://uploads.github.com/repos/alibaba/${REPO}/releases/${id}/assets?name=${assetName}"
                            }
                            for (file in CHECKSUM_MAP) {
                                def assetName = file.key
                                sh "curl -Ss -XPOST -H \"Authorization:token ${TOKEN}\" -H \"Content-Type:text/plain\" --data-binary @${assetName} https://uploads.github.com/repos/alibaba/${REPO}/releases/${id}/assets?name=${assetName}"
                            }
                        }
                    }
                }
            }
        }
        stage('publishDocker-x64') {
            when {
                // Only say hello if a "greeting" is requested
                expression { params.DOCKER == true }
            }
            agent {
                label 'docker:x64'
            }
            steps {
                script {
                    sh "docker login"
                    def url = "https://github.com/alibaba/dragonwell17/releases/download/dragonwell-17.0.0%2B35_jdk-17-ga/Alibaba_Dragonwell_17.0.0%2B35_x64_linux.tar.gz"
                    def urlAlpine = "https://github.com/alibaba/dragonwell17/releases/download/dragonwell-17.0.0%2B35_jdk-17-ga/Alibaba_Dragonwell_17.0.0%2B35_x64_alpine-linux.tar.gz"
                    sh "wget ${BUILDER} -O build.sh"
                    sh "sh build.sh ${url} ${params.GITHUBTAG} ${urlAlpine}"
                }
            }
        }
        stage('publishDocker-aarch64') {
            when {
                // Only say hello if a "greeting" is requested
                expression { params.DOCKER == true }
            }
            agent {
                label 'docker:aarch64'
            }
            steps {
                script {
                    sh "docker login"
                    def url = "https://github.com/alibaba/dragonwell17/releases/download/dragonwell-17.0.0%2B35_jdk-17-ga/Alibaba_Dragonwell_17.0.0%2B35_aarch64_linux.tar.gz"
                    def urlAlpine = ""
                    sh "wget ${BUILDER} -O build.sh"
                    sh "sh build.sh ${url} ${params.GITHUBTAG} ${urlAlpine}"
                }
            }
        }
    }
}