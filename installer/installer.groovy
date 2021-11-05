import groovy.json.*

properties[
        [$class: 'JiraProjectProperty'],
        [$class: 'RebuildSettings', autoRebuild: false, rebuildDisabled: false],
]
OSS_TOOL = "/home/testuser/ossutil64"
TOKEN = "ghp_KI10VDceecSlImTXHnhK0cWm7prLUc0oFsU" + "S"
RELEASE_MAP = [:]
CHECKSUM_MAP = [:]

RELEASE_NOTES_TEMPLATE= """
# VERSION

## version
``` 
VERSION_OUTPUT
```
"""

DOCKER_IMAGES_TEMPLATE="""
# VERSION

| Image:Tag | arch | OS | slim? |
|---|---|---|---|
| registry.cn-hangzhou.aliyuncs.com/dragonwell/dragonwell:dragonwell-VERSION | x86_64 | centos | No |
| registry.cn-hangzhou.aliyuncs.com/dragonwell/dragonwell:dragonwell-VERSION-alpine | x86_64 | alpine | No |
| registry.cn-hangzhou.aliyuncs.com/dragonwell/dragonwell:dragonwell-VERSION | aarch64 | centos |
"""
MIRROS_DOWNLOAD_17_TEMPLATE="""

# VERSION

| File name | China mainland | United States |
|---|---|---|
| Alibaba_Dragonwell_jdk-VERSION_aarch64_linux.tar.gz | [download](https://dragonwell.oss-cn-shanghai.aliyuncs.com/OSS_VERSION/Alibaba_Dragonwell_OSS_VERSION_aarch64_linux.tar.gz) | [download](https://github.com/alibaba/dragonwell${params.RELEASE}/download/dragonwell-OSS_VERSION_jdk-17-ga/Alibaba_Dragonwell_VERSION_aarch64_linux.tar.gz) |
| Alibaba_Dragonwell_jdk-VERSION_x64_alpine-linux.tar.gz | [download](https://dragonwell.oss-cn-shanghai.aliyuncs.com/OSS_VERSION/Alibaba_Dragonwell_OSS_VERSION_x64_alpine-linux.tar.gz) | [download](https://github.com/alibaba/dragonwell${params.RELEASE}/releases/download/dragonwell-OSS_VERSION_jdk-17-ga/Alibaba_Dragonwell_VERSION_x64_alpine-linux.tar.gz) |
| Alibaba_Dragonwell_jdk-VERSION_x64-linux.tar.gz | [download](https://dragonwell.oss-cn-shanghai.aliyuncs.com/OSS_VERSION/Alibaba_Dragonwell_OSS_VERSION_x64_linux.tar.gz) | [download](https://github.com/alibaba/dragonwell${params.RELEASE}/releases/download/dragonwell-OSS_VERSION_jdk-17-ga/Alibaba_Dragonwell_VERSION_x64_linux.tar.gz) |
| Alibaba_Dragonwell_jdk-VERSION_x86_windows.zip | [download](https://dragonwell.oss-cn-shanghai.aliyuncs.com/OSS_VERSION/Alibaba_Dragonwell_OSS_VERSION_x86_windows.zip) | [download](https://github.com/alibaba/dragonwell${params.RELEASE}/releases/download/dragonwell-OSS_VERSION_jdk-17-ga/Alibaba_Dragonwell_VERSION_x86_windows.zip) |
"""

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
        booleanParam(defaultValue: false,
                description: 'update wiki',
                name: 'WIKI')
        string(name: 'DOCKER_URL',
                defaultValue: "latest",
                description: 'Build number')
        string(name: 'DOCKER_ALPINE_URL',
                defaultValue: "latest",
                description: 'Build number')
        string(name: 'DOCKER_ARM_URL',
                defaultValue: "latest",
                description: 'Build number')

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
                    def url = "${params.DOCKER_URL}"
                    def urlAlpine = "${params.DOCKER_ALPINE_URL}"
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
                    def url = "${params.DOCKER_ARM_URL}"
                    def urlAlpine = ""
                    sh "wget ${BUILDER} -O build.sh"
                    sh "sh build.sh ${url} ${params.GITHUBTAG} ${urlAlpine}"
                }
            }
        }

        stage('wiki-update') {
            when {
                // Only say hello if a "greeting" is requested
                expression { params.WIKI == true }
            }
            agent {
                label 'ossworker'
            }
            steps {
                script {
                    sh "rm -rf workspace/target/ || true"
                    copyArtifacts(
                            projectName: "build-scripts/openjdk${params.RELEASE}-pipeline",
                            filter: "**/${HEAD}*x64_linux*dragonwell*tar.gz*json",
                            selector: specific("${params.BUILDNUMBER}"),
                            fingerprintArtifacts: true,
                            target: "workspace/target/",
                            flatten: true)
                    dir ("/root/wiki/dragonwell${params.RELEASE}.wiki") {
                        print "更新ReleaseNotes"
                        def fullVersionOutput = sh(script: "docker run  registry.cn-hangzhou.aliyuncs.com/dragonwell/dragonwell:${params.GITHUBTAG}_slim java -version", returnStdout: true).split()
                        def releasenots = sh(script: "cat Alibaba-Dragonwell-${params.RELEASE}-Release-Notes.md", returnStdout: true).trim()
                        if (!releasenots.contains("${params.VERSION}")) {
                            print "更新 ${params.VERSION} 到 Alibaba-Dragonwell-${params.RELEASE}-Release-Notes.md"
                            URL apiUrl = new URL("https://api.github.com/repos/alibaba/dragonwell${params.RELEASE}/releases")
                            def card = new JsonSlurper().parse(apiUrl)
                            def fromTag = ""
                            if (card.size() > 1) {
                                def lastRelease = card[1].get("tag_name")
                                fromTag = "--fromtag ${lastRelease}"
                            }
                            sh "wget https://raw.githubusercontent.com/dragonwell-releng/jenkinsUserContent/master/utils/driller.py -O driller.py"
                            def gitLogReport =  files = sh(script: "python driller.py --repo /repo/dragonwell${params.RELEASE} ${fromTag} --totag master", returnStdout: true)
                            print "#${params.RELEASE}"
                            print """
                            ```
                            ${fullVersionOutput}
                            ```
                            """
                            print gitLogReport
                        }
                        print "更新发布说明"
                        print "更新docker镜像"
                        print "更新OSS下载链接"
                    }
                }
            }
        }
    }
}
