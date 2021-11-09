import groovy.json.*

properties[
        [$class: 'JiraProjectProperty'],
        [$class: 'RebuildSettings', autoRebuild: false, rebuildDisabled: false],
]
OSS_TOOL = "/home/testuser/ossutil64"
TOKEN = "ghp_KI10VDceecSlImTXHnhK0cWm7prLUc0oFsU" + "S"
RELEASE_MAP = [:]
CHECKSUM_MAP = [:]

def tagName4Docker = params.GITHUBTAG
if (params.RELEASE == "17")
    tagName4Docker = tagName4Docker.replace("+", ".") // + is not allowed is docker image


DOCKER_IMAGES_TEMPLATE1 = "| registry.cn-hangzhou.aliyuncs.com/dragonwell/dragonwell:dragonwell-VERSION | x86_64 | centos | No |"
DOCKER_IMAGES_TEMPLATE2 = "| registry.cn-hangzhou.aliyuncs.com/dragonwell/dragonwell:dragonwell-VERSION | aarch64 | centos | No |"
DOCKER_IMAGES_TEMPLATE3 = "| registry.cn-hangzhou.aliyuncs.com/dragonwell/dragonwell:dragonwell-VERSION_slim | x86_64 | centos | Yes |"
DOCKER_IMAGES_TEMPLATE4 = "| registry.cn-hangzhou.aliyuncs.com/dragonwell/dragonwell:dragonwell-VERSION_slim | aarch64 | centos | Yes |"
DOCKER_IMAGES_TEMPLATE5 = "| registry.cn-hangzhou.aliyuncs.com/dragonwell/dragonwell:dragonwell-VERSION-alpine | x86_64 | alpine | No |"


MIRROS_DOWNLOAD_TEMPLATE = """

# ${params.VERSION}

| File name | China mainland | United States |
|---|---|---|
| Alibaba_Dragonwell_jdk-${params.VERSION}_aarch64_linux.tar.gz | [download](https://dragonwell.oss-cn-shanghai.aliyuncs.com/${params.VERSION}/Alibaba_Dragonwell_${params.VERSION}_aarch64_linux.tar.gz) | [download](https://github.com/alibaba/dragonwell${params.RELEASE}/download/${params.GITHUBTAG}/Alibaba_Dragonwell_${params.VERSION}_aarch64_linux.tar.gz) |
| Alibaba_Dragonwell_jdk-${params.VERSION}_x64_alpine-linux.tar.gz | [download](https://dragonwell.oss-cn-shanghai.aliyuncs.com/${params.VERSION}/Alibaba_Dragonwell_${params.VERSION}_x64_alpine-linux.tar.gz) | [download](https://github.com/alibaba/dragonwell${params.RELEASE}/releases/download/${params.GITHUBTAG}/Alibaba_Dragonwell_${params.VERSION}_x64_alpine-linux.tar.gz) |
| Alibaba_Dragonwell_jdk-${params.VERSION}_x64-linux.tar.gz | [download](https://dragonwell.oss-cn-shanghai.aliyuncs.com/${params.VERSION}/Alibaba_Dragonwell_${params.VERSION}_x64_linux.tar.gz) | [download](https://github.com/alibaba/dragonwell${params.RELEASE}/releases/download/${params.GITHUBTAG}/Alibaba_Dragonwell_${params.VERSION}_x64_linux.tar.gz) |
| Alibaba_Dragonwell_jdk-${params.VERSION}_x86_windows.zip | [download](https://dragonwell.oss-cn-shanghai.aliyuncs.com/${params.VERSION}/Alibaba_Dragonwell_${params.VERSION}_x86_windows.zip) | [download](https://github.com/alibaba/dragonwell${params.RELEASE}/releases/download/${params.GITHUBTAG}/Alibaba_Dragonwell_${params.VERSION}_x86_windows.zip) |
"""

if (params.RELEASE == "17") {
    MIRROS_DOWNLOAD_TEMPLATE = MIRROS_DOWNLOAD_TEMPLATE.replace("+", "%2B")
}

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
                    sh "sh build.sh ${url} ${tagName4Docker} ${urlAlpine}"
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
                    sh "sh build.sh ${url} ${tagName4Docker} ${urlAlpine}"
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
                    dir("/repo/dragonwell${params.RELEASE}") {
                        sh "git fetch origin"
                        sh "git reset --hard origin/master"
                    }
                    dir("/root/wiki/dragonwell${params.RELEASE}.wiki") {
                        print "更新ReleaseNotes"
                        sh "git fetch origin && git reset --hard origin/master"
                        sh(script: "docker run  registry.cn-hangzhou.aliyuncs.com/dragonwell/dragonwell:${tagName4Docker}_slim java -version 2> tmpt")
                        def fullVersionOutput = sh(script: "cat tmpt", returnStdout: true).replace(" ", "")
                        print "fullversion is ${fullVersionOutput}"
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
                            def gitLogReport = sh(script: "python3 driller.py --repo /repo/dragonwell${params.RELEASE} ${fromTag} --totag master", returnStdout: true)
                            def newReleasenotes = """
# ${params.VERSION}
 ```
${fullVersionOutput}
 ```
${gitLogReport}
                            """ + releasenots
                            writeFile file: "Alibaba-Dragonwell-${params.RELEASE}-Release-Notes.md", text: newReleasenotes
                            sh "git add Alibaba-Dragonwell-${params.RELEASE}-Release-Notes.md"
                            sh "git commit -m \" update Alibaba-Dragonwell-${params.RELEASE}-Release-Notes.md \""
                            sh "git push origin HEAD:master"
                        }
                        print "更新docker镜像"
                        def dockerimages = sh(script: "cat Use-Dragonwell-${params.RELEASE}-docker-images.md", returnStdout: true).trim()

                        if (!dockerimages.contains("${tagName4Docker}")) {
                            print "更新 ${tagName4Docker} 到 Use-Dragonwell-${params.RELEASE}-docker-images.md"
                            ArrayList l = new ArrayList(Arrays.asList(dockerimages.split("\n")))
                            for (int i = 0; i < l.size(); i++) {
                                if (l.get(i).contains("---")) {
                                    l.add(i + 1, DOCKER_IMAGES_TEMPLATE1.replace("VERSION", tagName4Docker));
                                    l.add(i + 1, DOCKER_IMAGES_TEMPLATE2.replace("VERSION", tagName4Docker));
                                    l.add(i + 1, DOCKER_IMAGES_TEMPLATE3.replace("VERSION", tagName4Docker));
                                    l.add(i + 1, DOCKER_IMAGES_TEMPLATE4.replace("VERSION", tagName4Docker));
                                    if (params.RELEASE != "8") {
                                        l.add(i + 1, DOCKER_IMAGES_TEMPLATE5.replace("VERSION", tagName4Docker));
                                    }
                                    break;
                                }
                            }
                            writeFile file: "Use-Dragonwell-${params.RELEASE}-docker-images.md", text: l.join("\n")
                            sh "git add Use-Dragonwell-${params.RELEASE}-docker-images.md"
                            sh "git commit -m \" update Use-Dragonwell-${params.RELEASE}-docker-images.md \""
                            sh "git push origin HEAD:master"
                        }
                        print "更新OSS下载链接"
                        def fineName = "\'Mirrors-for-download-(下载镜像).md\'"
                        def osslinks = sh(script: "cat $fineName", returnStdout: true).trim()
                        print "oss links ${osslinks}"
                        if (!osslinks.contains("${params.VERSION}")) {
                            print "更新 ${params.VERSION} 到下载镜像"
                            writeFile file: 'Mirrors-for-download-(下载镜像).md', text: (MIRROS_DOWNLOAD_TEMPLATE + osslinks)
                        }
                        sh "git add 'Mirrors-for-download-(下载镜像).md'"
                        sh "git commit -m \" update Mirrors for download\""
                        sh "git push origin HEAD:master"
                    }
                }
            }
        }
    }
}
