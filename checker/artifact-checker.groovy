import net.sf.json.groovy.JsonSlurper
import groovy.json.*


def pkgs = []
@groovy.transform.Field def githubtag = ""
@groovy.transform.Field def publishtag = ""
@groovy.transform.Field def openjdktag = ""
@groovy.transform.Field def platforms = []
if ("${params.RELEASE}" == "17") {
    platforms = ["aarch64_linux", "x64_alpine-linux", "x64_linux", "x86_windows"]
} else if ("${params.RELEASE}" == "11") {
    platforms = ["aarch64_linux", "x64_alpine-linux", "x64_linux", "x64_windows"]
} else {
    platforms = ["aarch64_linux", "x64_linux", "x64_windows"]
}
@groovy.transform.Field def pkg_list = []
@groovy.transform.Field def txt_list = []
@groovy.transform.Field def debug = false

@groovy.transform.Field def results = [:]


def addResult(test, result, msg) {
    results["${test}"] = [
            "result" : true,
            "message": msg
    ]
}

def resultMsg(mode, input) {
    def msg = ""
    if (mode == 1) {
        // mode 1: output msg is about publish tag
        def build_num = publishtag.split("\\+")[1]
        def arr = publishtag.split("\\.")
        def ups_tag = openjdktag.split("jdk-")[1]
        msg = """
upstream tag: ${ups_tag}</br>
current tag: ${publishtag}</br>
feature-release counter:${arr[0]}  interim-release counter:${arr[1]}</br>
update-release counter:${arr[2]}  emergency patch-release counter:${arr[3]}</br>
build number:${build_num}
"""
    } else if (mode == 2) {
        // mode 2: output message is about validate text
        msg = """
sha256 value: ${input[0]}</br>
checksum result: ${input[1]}
"""
    }
    return msg
}

def checkName(name) {
    def tag = githubtag.split("dragonwell-")[1].split("_jdk")[0]
    def reg_tag = tag.replaceAll('\\+', '\\\\\\+')
    def ret_value = false
    for (platform in platforms) {
        def res = name.matches("Alibaba_Dragonwell_${reg_tag}_${platform}.*")
        if (res == true) {
            ret_value = res
            if (name.matches(".*\\.txt")) {
                queryList(platform, txt_list)
            } else {
                queryList(platform, pkg_list)
            }
            break
        }
    }
    return "${ret_value}"
}

def queryList(ele, list) {
    if (list.contains(ele)) {
        echo "repeat package/text"
    } else {
        list.add(ele)
    }
}

def releaseDisplay(release) {
    HTML = release.get("html_url")
    echo "GITHUB is released at ${HTML}"
    def assets = release.get("assets")
    def assetsNum = assets.size()
    echo "GITHUB release artifacts ${assetsNum}"
    for (asset in assets) {
        def name = asset.get("name")
        echo "GITHUB released ${name}"
        if (checkName(name) == "false") {
            echo "${name} is invalid package name"
            return []
        }
    }
    if (pkg_list.size() != platforms.size() || txt_list.size() != platforms.size()) {
        error "missing publish package/text"
    } else {
        addResult("CheckGithubReleaseArtifactsName", true, platforms.join("<br>"))
        addResult("CheckGithubRleaseArtifactsSum", true, platforms.size())
    }
    return assets
}

def validateFile(pkg_name, cmp_file) {
    def sha_val = sh returnStdout: true, script: "sha256sum ${pkg_name} | cut -d ' ' -f 1"
    def check_res = sh returnStdout: true, script: "cat ${cmp_file} | grep ${sha_val}"
    if (check_res == false) {
        error "sha256 is wrong"
        return [false, sha_val]
    }
    return [true, sha_val]
}

DIR = ""
GITHUBTAG = ""
VERSION = ""
HTML = ""

/**
 * Check the latest release
 */
pipeline {
    agent none
    parameters {
        choice(name: 'RELEASE', choices: '17\n11\n8\n', description: 'Use which Multiplexing')
        booleanParam(defaultValue: false,
                description: 'Use Dragonwell check rule',
                name: 'DRAGONWELL')
    }
    stages {
        stage('Check Github Artifact format') {
            agent {
                label 'artifact.checker'
            }
            steps {
                script {
                    URL apiUrl = new URL("https://api.github.com/repos/alibaba/dragonwell${params.RELEASE}/releases")
                    URL openjdkUrl = new URL("https://api.github.com/repos/adoptium/temurin${params.RELEASE}-binaries/releases/latest")
                    def card = new JsonSlurper().parse(apiUrl)
                    def openjdk_card = new JsonSlurper().parse(openjdkUrl)
                    openjdktag = openjdk_card.get("tag_name")
                    def arr = []
                    githubtag = card[0].get("tag_name")
                    publishtag = githubtag.split("-")[1].split("_jdk")[0]

                    echo "publish tags ${publishtag}"
                    echo "openjdktag tags ${openjdktag}"
                    def prerelease = card[0].get("prerelease")
                    def draft = card[0].get("draft")
                    if (draft == true || prerelease == false) {
                        echo "please check prerelease status!"
                    }
                    echo "GITHUB TAG is ${githubtag}"
                    arr = releaseDisplay(card[0])
                    if (card.size() > 1) {
                        print "PREVIOUS RELEASE"
                        //arr = releaseDisplay(card[1])
                    }
                    pkgs = arr
                    if (arr == []) {
                        error "exist invalid package name"
                    }
                    echo "package/text name check PASS"
                }
            }
        }
        stage('Parallel Job') {
            parallel {
                stage("Test On Windows") {
                    agent {
                        label "windows"
                    }
                    steps {
                        script {
                            sh "rm -rf test || mkdir -p test"
                            dir("test") {
                                if (debug) {
                                  sh "wget -q https://raw.githubusercontent.com/Accelerator1996/jenkinsUserContent/master/utils/check_tag.sh"
                                } else {
                                  sh "wget -q https://raw.githubusercontent.com/dragonwell-releng/jenkinsUserContent/master/utils/check_tag.sh"
                                }
                                for (pkg in pkgs) {
                                    def pkg_name = pkg.get("name")
                                    if (pkg_name.matches(".*windows.*")) {
                                        echo "${pkg_name}"
                                        def suffix = pkg_name.tokenize("\\.").pop()
                                        sh "wget -q https://github.com/alibaba/dragonwell${params.RELEASE}/releases/download/${githubtag}/${pkg_name} -O jdk.${suffix}"
                                        if ("${suffix}" == "zip") {
                                            sh "unzip jdk.zip"
                                            if (params.DRAGONWELL == false) {
                                                def java_home = sh returnStdout: true, script: "ls . | grep jdk | grep -v ${suffix}"
                                                def check_dirname = java_home.contains(publishtag)
                                                if (check_dirname == false) {
                                                    error "compress package dirname is wrong"
                                                }
                                                def res = sh returnStdout: true, script: "bash check_tag.sh ${publishtag} ${params.RELEASE} ${java_home}"
                                                if (res != true) res = false
                                                addResult("CheckWindowsCompressedPackage", res, resultMsg(1, ""))
                                                sh "rm -rf ${java_home}"
                                            }
                                        } else if ("${suffix}" == "txt") {
                                            def (res, val) = validateFile("jdk.zip", "jdk.txt")
                                            addResult("CheckWindowsValidateText", res, resultMsg(2, [val, res]))
                                            sh "rm -rf jdk.txt jdk.zip"
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                stage("Test On Linux x64") {
                    agent {
                        label "linux&&x64"
                    }
                    steps {
                        script {
                            sh "rm -rf test || mkdir -p test"
                            dir("test") {
                                if (debug) {
                                  sh "wget -q https://raw.githubusercontent.com/Accelerator1996/jenkinsUserContent/master/utils/check_tag.sh"
                                } else {
                                  sh "wget -q https://raw.githubusercontent.com/dragonwell-releng/jenkinsUserContent/master/utils/check_tag.sh"
                                }
                                for (pkg in pkgs) {
                                    def pkg_name = pkg.get("name")
                                    if (pkg_name.matches(".*x64_linux.*")) {
                                        echo "${pkg_name}"
                                        def suffix = pkg_name.tokenize("\\.").pop()
                                        if ("${suffix}" == "gz") suffix = "tar.gz"
                                        sh "wget -q https://github.com/alibaba/dragonwell${params.RELEASE}/releases/download/${githubtag}/${pkg_name} -O jdk.${suffix}"
                                        if ("${suffix}" == "zip") {
                                            sh "unzip jdk.zip"
                                            if (params.DRAGONWELL == false) {
                                                def java_home = sh returnStdout: true, script: "ls . | grep jdk | grep -v ${suffix}"
                                                def check_dirname = java_home.contains(publishtag)
                                                if (check_dirname == false) {
                                                    error "compress package dirname is wrong"
                                                }
                                                def res = sh returnStdout: true, script: "bash check_tag.sh ${publishtag} ${params.RELEASE} ${java_home}"
                                                if (res != true) res = false
                                                addResult("CheckLinuxX64CompressedPackage", res, resultMsg(1, ""))
                                                sh "rm -rf ${java_home}"
                                            }
                                        } else if ("${suffix}" == "txt") {
                                            def (res, val) = validateFile("jdk.tar.gz", "jdk.txt")
                                            addResult("CheckLinuxX64ValidateText", res, resultMsg(2, [val, res]))
                                            sh "rm -rf jdk.txt jdk.tar.gz"
                                        } else if ("${suffix}" == "tar.gz") {
                                            sh "tar xf jdk.tar.gz"
                                            def java_home = sh returnStdout: true, script: "ls . | grep jdk | grep -v ${suffix}"
                                            def check_dirname = java_home.contains(publishtag)
                                            if (check_dirname == false) {
                                                addResult("CheckLinuxX64CompressedPackage", false, resultMsg(1, ""))
                                                error "compress package dirname is wrong"
                                            }
                                            addResult("CheckLinuxX64CompressedPackage", true, resultMsg(1, ""))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                stage("Test On Linux aarch64") {
                    agent {
                        label "linux&&aarch64"
                    }
                    steps {
                        script {
                            sh "rm -rf test || mkdir -p test"
                            dir("test") {
                                if (debug) {
                                  sh "wget -q https://raw.githubusercontent.com/Accelerator1996/jenkinsUserContent/master/utils/check_tag.sh"
                                } else {
                                  sh "wget -q https://raw.githubusercontent.com/dragonwell-releng/jenkinsUserContent/master/utils/check_tag.sh"
                                }
                                for (pkg in pkgs) {
                                    def pkg_name = pkg.get("name")
                                    if (pkg_name.matches(".*aarch64_linux.*")) {
                                        echo "${pkg_name}"
                                        def suffix = pkg_name.tokenize("\\.").pop()
                                        if ("${suffix}" == "gz") suffix = "tar.gz"
                                        sh "wget -q https://github.com/alibaba/dragonwell${params.RELEASE}/releases/download/${githubtag}/${pkg_name} -O jdk.${suffix}"
                                        if ("${suffix}" == "zip") {
                                            sh "unzip -q jdk.zip"
                                            if (params.DRAGONWELL == false) {
                                                def java_home = sh returnStdout: true, script: "ls . | grep jdk | grep -v ${suffix}"
                                                def check_dirname = java_home.contains(publishtag)
                                                if (check_dirname == false) {
                                                    error "compress package dirname is wrong"
                                                }
                                                def res = sh returnStdout: true, script: "bash check_tag.sh ${publishtag} ${params.RELEASE} ${java_home}"
                                                if (res != true) res = false
                                                addResult("CheckLinuxAarch64CompressedPackage", res, resultMsg(1, ""))
                                                sh "rm -rf ${java_home}"
                                            }
                                        } else if ("${suffix}" == "txt") {
                                            def (res, val) = validateFile("jdk.tar.gz", "jdk.txt")
                                            addResult("CheckLinuxAarch64ValidateText", res, resultMsg(2, [val, res]))
                                            sh "rm -rf jdk.txt jdk.tar.gz"
                                        } else if ("${suffix}" == "tar.gz") {
                                            sh "tar xf jdk.tar.gz"
                                            def java_home = sh returnStdout: true, script: "ls . | grep jdk | grep -v ${suffix}"
                                            def check_dirname = java_home.contains(publishtag)
                                            if (check_dirname == false) {
                                                addResult("CheckLinuxAarch64CompressedPackage", false, resultMsg(1, ""))
                                                error "compress package dirname is wrong"
                                            }
                                            addResult("CheckLinuxAarch64CompressedPackage", true, resultMsg(1, ""))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                stage("Test On Linux x64 alpine") {
                    agent {
                        label "linux&&x64"
                    }
                    steps {
                        script {
                            sh "rm -rf test || mkdir -p test"
                            dir("test") {
                                if (debug) {
                                  sh "wget -q https://raw.githubusercontent.com/Accelerator1996/jenkinsUserContent/master/utils/check_tag.sh"
                                } else {
                                  sh "wget -q https://raw.githubusercontent.com/dragonwell-releng/jenkinsUserContent/master/utils/check_tag.sh"
                                }
                                for (pkg in pkgs) {
                                    def pkg_name = pkg.get("name")
                                    if (pkg_name.matches(".*x64_alpine-linux.*")) {
                                        echo "${pkg_name}"
                                        def suffix = pkg_name.tokenize("\\.").pop()
                                        if ("${suffix}" == "gz") suffix = "tar.gz"
                                        sh "wget -q https://github.com/alibaba/dragonwell${params.RELEASE}/releases/download/${githubtag}/${pkg_name} -O jdk.${suffix}"
                                        if ("${suffix}" == "zip") {
                                            sh "unzip jdk.zip"
                                            if (params.DRAGONWELL == false) {
                                                def java_home = sh returnStdout: true, script: "ls . | grep jdk | grep -v ${suffix}"
                                                def check_dirname = java_home.contains(publishtag)
                                                if (check_dirname == false) {
                                                    error "compress package dirname is wrong"
                                                }
                                                def res = sh returnStdout: true, script: "bash check_tag.sh ${publishtag} ${params.RELEASE} ${java_home}"
                                                if (res != true) res = false
                                                addResult("CheckLinuxX64AlpineCompressedPackage", res, resultMsg(1, ""))
                                                sh "rm -rf ${java_home}"
                                            }
                                        } else if ("${suffix}" == "txt") {
                                            def (res, val) = validateFile("jdk.tar.gz", "jdk.txt")
                                            addResult("CheckLinuxX64AlpineValidateText", res, resultMsg(2, [val, res]))
                                            sh "rm -rf jdk.txt jdk.tar.gz"
                                        } else if ("${suffix}" == "tar.gz") {
                                            sh "tar xf jdk.tar.gz"
                                            def java_home = sh returnStdout: true, script: "ls . | grep jdk | grep -v ${suffix}"
                                            def check_dirname = java_home.contains(publishtag)
                                            if (check_dirname == false) {
                                                addResult("CheckLinuxX64AlpineCompressedPackage", false, resultMsg(1, ""))
                                                error "compress package dirname is wrong"
                                            }
                                            addResult("CheckLinuxX64AlpineCompressedPackage", true, resultMsg(1, ""))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        stage('result') {
            agent {
                label "artifact.checker"
            }
            steps {
                script {
                    writeFile file: 'release.json', text: groovy.json.JsonOutput.toJson(results)
                    sh "rm -rf reports || true"
                    sh "wget -q https://raw.githubusercontent.com/dragonwell-releng/jenkinsUserContent/master/checker/htmlReporter.py -O htmlReporter.py"
                    sh "python htmlReporter.py"
                }
            }
            post {
                always {
                    publishHTML(target: [allowMissing         : false,
                                         alwaysLinkToLastBuild: true,
                                         keepAll              : true,
                                         reportDir            : 'reports',
                                         reportFiles          : 'TestResults*.html',
                                         reportName           : 'Test Reports',
                                         reportTitles         : 'Test Report'])
                }
            }
        }
    }
}
