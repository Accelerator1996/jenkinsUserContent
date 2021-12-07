import net.sf.json.groovy.JsonSlurper
import groovy.json.*

@groovy.transform.Field def pkgs = []
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
    if (mode == "version") {
        if (publishtag.split("\\+").size() > 2 && openjdktag.split("\\+").size() > 2 && publishtag.split("\\.") > 4) {
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
        }
    } else if (mode == "checksum") {
        // mode 2: output message is about validate text
        msg = """
sha256 value: ${input[0]}</br>
checksum result: ${input[1]}
"""
    }
    return msg
}

def checkGithubArtifactName(name) {
    def tag = githubtag.split("dragonwell-")[1].split("_jdk")[0]
    def reg_tag = tag.replaceAll('\\+', '\\\\\\+')
    def ret_value = false
    for (platform in platforms) {
        def res = name.matches("Alibaba_Dragonwell_${reg_tag}_${platform}.*")
        if (res == true) {
            ret_value = res
            if (name.matches(".*\\.txt")) {
                putIfAbsent(platform, txt_list)
            } else {
                putIfAbsent(platform, pkg_list)
            }
            break
        }
    }
    return "${ret_value}"
}

def putIfAbsent(ele, list) {
    if (list.contains(ele)) {
        echo "repeat package/text"
    } else {
        list.add(ele)
    }
}

def checkGithubLatestRelease(release) {
    HTML = release.get("html_url")
    echo "GITHUB is released at ${HTML}"
    def assets = release.get("assets")
    def assetsNum = assets.size()
    echo "GITHUB release artifacts ${assetsNum}"
    for (asset in assets) {
        def name = asset.get("name")
        echo "GITHUB released ${name}"
        if (!checkGithubArtifactName(name)) {
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

def validateCheckSum(pkg_name, cmp_file) {
    def sha_val = sh returnStdout: true, script: "sha256sum ${pkg_name} | cut -d ' ' -f 1"
    def check_res = sh returnStdout: true, script: "cat ${cmp_file} | grep ${sha_val}"
    if (check_res == false) {
        error "sha256 is wrong"
        return [false, sha_val]
    }
    return [true, sha_val]
}

def checkArtifactContent(platform) {
    sh "wget -q https://raw.githubusercontent.com/dragonwell-releng/jenkinsUserContent/master/utils/check_tag.sh"
    for (pkg in pkgs) {
        def pkg_name = pkg.get("name")
        if (pkg_name.matches(".*${platform}.*")) {
            def suffix = pkg_name.tokenize("\\.").pop()
            if ("${suffix}" == "txt") {
                echo "checksum file is ${pkg_name}"
                def originFile = pkg_name.replace(".sha256.txt", "")
                sh "wget -q https://github.com/alibaba/dragonwell${params.RELEASE}/releases/download/${githubtag}/${pkg_name} -O ${pkg_name}"
                sh "wget -q https://github.com/alibaba/dragonwell${params.RELEASE}/releases/download/${githubtag}/${originFile} -O ${originFile}"
                def (res, val) = validateCheckSum("${originFile}", "${pkg_name}")
                addResult("Check${platform}Text", res, resultMsg("checksum", [val, res]))
                sh "rm -rf ${originFile} ${pkg_name}"
                continue
            } else if ("${suffix}" == "gz") {
                suffix = "tar.gz"
            }

            sh "wget -q https://github.com/alibaba/dragonwell${params.RELEASE}/releases/download/${githubtag}/${pkg_name} -O jdk.${suffix}"
            def unzipCommand = suffix == "tar.gz" ? "tar xf" : "unzip"
            sh "${unzipCommand} jdk.${suffix}"

            def java_home = sh returnStdout: true, script: "ls . | grep jdk | grep -v ${suffix}"
            java_home = java_home.trim()
            unzippedDirCheck(java_home)
            if (platform != "alpine") {
                def res = sh script: "bash check_tag.sh ${publishtag} ${params.RELEASE} ${java_home}"
                addResult("Check${platform}CompressedPackage", res, resultMsg("version", ""))
            }
            sh "rm -rf ${java_home}"
        }
    }
}


def unzippedDirCheck(java_home) {
    def check_dirname = false;
    if (params.RELEASE == "17") {
        check_dirname = java_home.contains(publishtag)
    } else if (params.RELEASE == "8") {
        check_dirname = java_home.contains(strippedOpenJDKTagWithoutBuildNumber(openjdktag))
    } else if (params.RELEASE == "11" ) {
        check_dirname = java_home.contains(strippedOpenJDKTagWithoutBuildNumber(openjdktag))
    }
    if (check_dirname == false) {
        error "compress package dirname is wrong"
    }

}


def strippedOpenJDKTagWithoutBuildNumber(ot) {
    echo "striping ${ot}"
    // 11
    if (ot.contains("+")) {
        def result = ot.split("\\+")[0]
        echo "stripped ${result}"
        return result
    } else {
        //8
        def result = ot.split("-")[0]
        echo "stripped ${result}"
        return result
    }
}

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
                    echo "https://api.adoptium.net/v3/assets/latest/${params.RELEASE}/hotspot?vendor=eclipse"
                    URL openjdkUrl = new URL("https://api.adoptium.net/v3/assets/latest/${params.RELEASE}/hotspot?vendor=eclipse")
                    def card = new JsonSlurper().parseText(apiUrl.text.trim())
                    def openjdk_card = new JsonSlurper().parseText(openjdkUrl.text.trim())
                    openjdktag = openjdk_card[0].get("release_name")
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
                    arr = checkGithubLatestRelease(card[0])
                    if (card.size() > 1) {
                        print "PREVIOUS RELEASE"
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
                                checkArtifactContent("windows")
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
                                checkArtifactContent("x64_linux")
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
                                checkArtifactContent("aarch64_linux")
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
                                if (platforms.contains("x64_alpine-linux")) {
                                    checkArtifactContent("alpine")
                                } else {
                                    echo "skip \"x64_alpine-linux\""
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
