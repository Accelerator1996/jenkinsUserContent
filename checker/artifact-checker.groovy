import net.sf.json.groovy.JsonSlurper
import groovy.json.*


def pkgs = []
@groovy.transform.Field def githubtag = ""
@groovy.transform.Field def publishtag = ""
@groovy.transform.Field def platforms = []
if ("${params.RELEASE}" == "17") {
    platforms = ["aarch64_linux","x64_alpine-linux","x64_linux","x86_windows"]
} else {
    platforms = ["aarch64_linux","x64_alpine-linux","x64_linux","x64_windows"] 
}
@groovy.transform.Field def pkg_list = []
@groovy.transform.Field def txt_list = []

@groovy.transform.Field def results = [:]


def addResult(test, result, msg) {
    results["${test}"] = [
        "result" : true,
        "message" : msg
    ]
}

def checkName(name) {
    def tag = githubtag.split( "dragonwell-")[1].split("_jdk")[0]
    def reg_tag = tag.replaceAll('\\+',  '\\\\\\+')
    def ret_value = false
    for(platform in platforms) {
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
        addResult("checkGithubReleaseArtifactsName", true, name)
    }
    if (pkg_list.size() != platforms.size() || txt_list.size() != platforms.size()) {
        error "missing publish package/text"
    } else {
        addResult("checkGithubRleaseArtifactsSum", true, platforms.size())
    }
   return assets
}

def validateFile(pkg_name, cmp_file) {
    def sha_val = sh returnStdout:true,script: "sha256sum ${pkg_name} | cut -d ' ' -f 1"
    def check_res = sh returnStdout:true,script: "cat ${cmp_file} | grep ${sha_val}"
    if (check_res == false) {
        error "sha256 is wrong"
    }
}

DIR=""
GITHUBTAG=""
VERSION=""
HTML=""

/**
 * Check the latest release
 */
pipeline {
    agent none
    parameters {
            choice(name: 'RELEASE', choices: '17\n11\n8\n',   description: 'Use which Multiplexing')
    }
    stages {
        stage('Check Github Artifact format') {
            agent {
                label 'artifact.checker'
            }
            steps {
                script {
                    URL apiUrl = new URL("https://api.github.com/repos/alibaba/dragonwell${params.RELEASE}/releases")
                    def card = new JsonSlurper().parse(apiUrl)
                    def arr = []
                    githubtag = card[0].get("tag_name")
                    publishtag = githubtag.split("-")[1].split("_jdk")[0]
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
        stage('Parallel Job' ) {
            parallel {
                stage("Test On Windows") {
                    agent {
                        label "windows"
                    }
                    steps {
                        script {
                            sh "rm -rf test || mkdir -p test"
                            dir("test") {
                                sh "wget -q https://raw.githubusercontent.com/dragonwell-releng/jenkinsUserContent/master/utils/check_tag.sh"
                                for (pkg in pkgs) {
                                    def pkg_name = pkg.get("name")
                                    if (pkg_name.matches(".*windows.*")) {
                                        echo "${pkg_name}"
                                        def suffix = pkg_name.tokenize("\\.").pop()
                                        sh "wget -q https://github.com/alibaba/dragonwell${params.RELEASE}/releases/download/${githubtag}/${pkg_name} -O jdk.${suffix}"
                                        if ("${suffix}" == "zip") {
                                            sh "unzip jdk.zip"
                                            def java_home = sh returnStdout: true, script: "ls . | grep jdk | grep -v ${suffix}"
                                            def check_dirname = java_home.contains(publishtag)
                                            if (check_dirname == false) {
                                                error "compress package dirname is wrong"
                                            }
                                            //sh "bash check_tag.sh ${publishtag} ${params.RELEASE} ${java_home}"
                                            echo "package on windows check PASS"
                                            sh "rm -rf ${java_home}"
                                        } else if ("${suffix}" == "txt") {
                                            validateFile("jdk.zip", "jdk.txt")
                                            addResult("WindowsCheckSumValidate", true, pkg)
                                            echo "text on windows check PASS"
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
                                sh "wget -q https://raw.githubusercontent.com/dragonwell-releng/jenkinsUserContent/master/utils/check_tag.sh"
                                for (pkg in pkgs) {
                                    def pkg_name = pkg.get("name")
                                    if (pkg_name.matches(".*x64_linux.*")) {
                                        echo "${pkg_name}"
                                        def suffix = pkg_name.tokenize("\\.").pop()
                                        if ("${suffix}" == "gz") suffix = "tar.gz"
                                        sh "wget -q https://github.com/alibaba/dragonwell${params.RELEASE}/releases/download/${githubtag}/${pkg_name} -O jdk.${suffix}"
                                        if ("${suffix}" == "zip") {
                                            sh "unzip jdk.zip"
                                            def java_home = sh returnStdout: true, script: "ls . | grep jdk | grep -v ${suffix}"
                                            def check_dirname = java_home.contains(publishtag)
                                            if (check_dirname == false) {
                                                error "compress package dirname is wrong"
                                            }
                                            //sh "bash check_tag.sh ${publishtag} ${params.RELEASE} ${java_home}"
                                            echo "package on x64_linux check PASS"
                                            sh "rm -rf ${java_home}"
                                        } else if ("${suffix}" == "txt") {
                                            validateFile("jdk.tar.gz", "jdk.txt")
                                            addResult("LinuxX64CheckSumValidate", true, pkg)
                                            echo "text on x64_linux check PASS"
                                            sh "rm -rf jdk.txt jdk.tar.gz"
                                        } else if ("${suffix}" == "tar.gz") {
                                            sh "tar xf jdk.tar.gz"
                                            def java_home = sh returnStdout: true, script: "ls . | grep jdk | grep -v ${suffix}"
                                            def check_dirname = java_home.contains(publishtag)
                                            if (check_dirname == false) {
                                                error "compress package dirname is wrong"
                                            }
                                            echo "package on x64_linux check PASS"
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
                                sh "wget -q https://raw.githubusercontent.com/dragonwell-releng/jenkinsUserContent/master/utils/check_tag.sh"
                                for (pkg in pkgs) {
                                    def pkg_name = pkg.get("name")
                                    if (pkg_name.matches(".*aarch64_linux.*")) {
                                        echo "${pkg_name}"
                                        def suffix = pkg_name.tokenize("\\.").pop()
                                        if ("${suffix}" == "gz") suffix = "tar.gz"
                                        sh "wget -q https://github.com/alibaba/dragonwell${params.RELEASE}/releases/download/${githubtag}/${pkg_name} -O jdk.${suffix}"
                                        if ("${suffix}" == "zip") {
                                            sh "unzip -q jdk.zip"
                                            def java_home = sh returnStdout: true, script: "ls . | grep jdk | grep -v ${suffix}"
                                            def check_dirname = java_home.contains(publishtag)
                                            if (check_dirname == false) {
                                                error "compress package dirname is wrong"
                                            }
                                            //sh "bash check_tag.sh ${publishtag} ${params.RELEASE} ${java_home}"
                                            echo "package on aarch64_linux check PASS"
                                            sh "rm -rf ${java_home}"
                                        } else if ("${suffix}" == "txt") {
                                            validateFile("jdk.tar.gz", "jdk.txt")
                                            addResult("LinuxAarch64CheckSumValidate", true, pkg)
                                            echo "text on aarch64_linux check PASS"
                                            sh "rm -rf jdk.txt jdk.tar.gz"
                                        } else if ("${suffix}" == "tar.gz") {
                                            sh "tar xf jdk.tar.gz"
                                            def java_home = sh returnStdout: true, script: "ls . | grep jdk | grep -v ${suffix}"
                                            def check_dirname = java_home.contains(publishtag)
                                            if (check_dirname == false) {
                                                error "compress package dirname is wrong"
                                            }
                                            echo "package on aarch64_linux check PASS"
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
                                sh "wget -q https://raw.githubusercontent.com/dragonwell-releng/jenkinsUserContent/master/utils/check_tag.sh"
                                for (pkg in pkgs) {
                                    def pkg_name = pkg.get("name")
                                    if (pkg_name.matches(".*x64_alpine-linux.*")) {
                                        echo "${pkg_name}"
                                        def suffix = pkg_name.tokenize("\\.").pop()
                                        if ("${suffix}" == "gz") suffix = "tar.gz"
                                        sh "wget -q https://github.com/alibaba/dragonwell${params.RELEASE}/releases/download/${githubtag}/${pkg_name} -O jdk.${suffix}"
                                        if ("${suffix}" == "zip") {
                                            sh "unzip jdk.zip"
                                            def java_home = sh returnStdout: true, script: "ls . | grep jdk | grep -v ${suffix}"
                                            def check_dirname = java_home.contains(publishtag)
                                            if (check_dirname == false) {
                                                error "compress package dirname is wrong"
                                            }
                                            //sh "bash check_tag.sh ${publishtag} ${params.RELEASE} ${java_home}"
                                            echo "package on x64_apline_linux check PASS"
                                            sh "rm -rf ${java_home}"
                                        } else if ("${suffix}" == "txt") {
                                            validateFile("jdk.tar.gz", "jdk.txt")
                                            addResult("LinuxX64AlpineCheckSumValidate", true, pkg)
                                            echo "text on x64_apline_linux check PASS"
                                            sh "rm -rf jdk.txt jdk.tar.gz"
                                        } else if ("${suffix}" == "tar.gz") {
                                            sh "tar xf jdk.tar.gz"
                                            def java_home = sh returnStdout: true, script: "ls . | grep jdk | grep -v ${suffix}"
                                            def check_dirname = java_home.contains(publishtag)
                                            if (check_dirname == false) {
                                                error "compress package dirname is wrong"
                                            }
                                            echo "package on x64_apline_linux check PASS"
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
                label "linux&&x64"
            }
            steps {
                script {
                    writeFile file: 'release.json', text: groovy.json.JsonOutput.toJson(results)
                }
            }
            post {
                always {
                    archiveArtifacts artifacts: "release.json"
                }
            }
        }
    }
}
