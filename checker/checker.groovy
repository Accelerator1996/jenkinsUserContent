import net.sf.json.groovy.JsonSlurper

def releaseDisplay(release) {
    GITHUBTAG = release.get("tag_name")
    echo "GITHUB TAG is ${GITHUBTAG}"
    HTML = release.get("html_url")
    echo "GITHUB is released at ${HTML}"
    def assets = release.get("assets")
    def assetsNum = assets.size()
    echo "GITHUB release artifacts ${assetsNum}"
    for (asset in assets) {
        def name = asset.get("name")
        echo "GITHUB released ${name}"
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
        choice(name: 'RELEASE', choices: '17\n11\n8\n',
                description: 'Use which Multiplexing')
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
                    releaseDisplay(card[0])
                    if (card.size() > 1) {
                        print "PREVIOUS RELEASE"
                        releaseDisplay(card[1])
                    }
                }
            }
        }
//
//        stage('Check OSS Artifact format') {
//
//        }
//
//        stage('Check Docker container format and version') {
//
//        }

//        stage('Run Version output Tests') {
//            parallel {
//                stage('Test On Windows') {
//                    agent {
//                        label "windows&&x64"
//                    }
//                    steps {
//                        script {
//                            sh "rm -rf test || true"
//                            dir ("test") {
//                                sh "wget -q https://github.com/alibaba/dragonwell${params.RELEASE}/releases/download/${params.GITHUBTAG}/Alibaba_Dragonwell_${params.VERSION}_x86_windows.zip -O jdk.zip"
//                                sh "unzip jdk.zip"
//                                sh "${params.DIR}/bin/java -version"
//                            }
//                        }
//                    }
//                }
//                stage('Test On Linux x64') {
//                    agent {
//                        label "linux&&x64"
//                    }
//                    steps {
//                        script {
//                            sh "rm -rf test || true"
//                            dir ("test") {
//                                sh "wget -q https://github.com/alibaba/dragonwell${params.RELEASE}/releases/download/${params.GITHUBTAG}/Alibaba_Dragonwell_${params.VERSION}_x64_linux.tar.gz -O jdk.tar.gz"
//                                sh "tar xf jdk.tar.gz"
//                                sh "${params.DIR}/bin/java -version"
//                            }
//                        }
//                    }
//                }
//                stage('Test On Linux aarch64') {
//                    agent {
//                        label "linux&&aarch64"
//                    }
//                    steps {
//                        script {
//                            sh "rm -rf test || true"
//                            dir ("test") {
//                                sh "wget -q https://github.com/alibaba/dragonwell${params.RELEASE}/releases/download/${params.GITHUBTAG}/Alibaba_Dragonwell_${params.VERSION}_aarch64_linux.tar.gz -O jdk.tar.gz"
//                                sh "tar xf jdk.tar.gz"
//                                sh "${params.DIR}/bin/java -version"
//                            }
//                        }
//                    }
//                }
//            }
//        }
    }
}