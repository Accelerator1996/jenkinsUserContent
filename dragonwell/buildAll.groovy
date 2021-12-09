pipeline {
    options {
        durabilityHint('PERFORMANCE_OPTIMIZED')
        buildDiscarder(logRotator(numToKeepStr: '15', artifactDaysToKeepStr: '15'))
    }
    agent {
        label 'binaries'
    }
    parameters {
        choice(name: 'RELEASE', choices: '17\n11\n8\n',
                description: 'Version')
    }
    stages {
        stage('iterateHistory') {
            agent {
                label 'binaries'
            }
            steps {
                script {
                    dir("/data/dragonwell${params.RELEASE}") {
                        sh "git pull origin"
                        sh "wget http://ci.dragonwell-jdk.io/userContent/utils/rebuildTrigger.py -O rebuildTrigger.py"
                        def list = sh returnStdout: true, script: "python rebuildTrigger.py --repo /data/dragonwell${params.RELEASE}"
                        for (hash in list) {
                            print "let me retrigger ${hash} see what happens"
                            /*
                            http://ci.dragonwell-jdk.io/job/github-trigger-pipelines/job/dragonwell17-github-commit-trigger/buildWithParameters?token=asnb
                            */
                            def jobUrl = "http://ci.dragonwell-jdk.io/job/github-trigger-pipelines/job/dragonwell${params.RELEASE}-github-commit-trigger/buildWithParameters?token=asnb"
                            sh "curl ${jobUrl} --SCM ${hash}"
                            sleep 10
                        }
                    }
                }
            }
        }
    }
}