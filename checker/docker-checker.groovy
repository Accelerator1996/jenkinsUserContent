import net.sf.json.groovy.JsonSlurper
import groovy.json.*

GITHUBTAG = ""


pipeline {
    agent none
    parameters {
        choice(name: 'RELEASE', choices: '17\n11\n8\n', description: 'Use which Multiplexing')
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
                    GITHUBTAG = card[0].get("tag_name")
                    if (params.RELEASE == "17") {
                        // docker 不允许用+
                        GITHUBTAG = GITHUBTAG.replace("+", ".")
                    }
                }
            }
        }
        stage('Parallel Job') {
            parallel {
                stage("Test On Linux x64") {
                    agent {
                        label "linux&&x64"
                    }
                    steps {
                        script {
                            sh "docker pull registry.cn-hangzhou.aliyuncs.com/dragonwell/dragonwell:${GITHUBTAG}"
                            def version = sh(script: "docker run  registry.cn-hangzhou.aliyuncs.com/dragonwell/dragonwell:${GITHUBTAG}  /opt/alibaba/dragonwell${params.RELEASE}/bin/java -version", returnStdout: true).split()
                            print version
                        }
                    }
                }
                stage("Test On Linux aarch64") {
                    agent {
                        label "linux&&aarch64"
                    }
                    steps {
                        script {
                            sh "docker pull registry.cn-hangzhou.aliyuncs.com/dragonwell/dragonwell:${GITHUBTAG}"
                            def version = sh(script: "docker run  registry.cn-hangzhou.aliyuncs.com/dragonwell/dragonwell:${GITHUBTAG}  /opt/alibaba/dragonwell${params.RELEASE}/bin/java -version", returnStdout: true).split()
                            print version
                        }
                    }
                }
                stage("Test On Linux x64 alpine") {
                    agent {
                        label "linux&&x64"
                    }
                    steps {
                        script {
                            sh "docker pull registry.cn-hangzhou.aliyuncs.com/dragonwell/dragonwell:${GITHUBTAG}-alpine"
                            def version = sh(script: "docker run  registry.cn-hangzhou.aliyuncs.com/dragonwell/dragonwell:${GITHUBTAG} /opt/alibaba/dragonwell${params.RELEASE}/bin/java -version", returnStdout: true).split()
                            print version
                        }
                    }
                }
            }
        }
    }
}
