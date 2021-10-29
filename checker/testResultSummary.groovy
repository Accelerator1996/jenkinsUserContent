import net.sf.json.groovy.JsonSlurper
import groovy.json.*


pipeline {
    agent none
    parameters {
        choice(name: 'RELEASE', choices: '17\n11\n8\n', description: 'Use which Multiplexing')
    }
    stages {
        stage('TestReport') {
            agent {
                label 'artifact.checker'
            }
            steps {
                script {
                    sh "wget https://raw.githubusercontent.com/dragonwell-releng/jenkinsUserContent/master/checker/resultReporter${params.RELEASE}.py -O resultReporter.py"
                    sh "python resultReporter.py"
                    sh "wget -q https://raw.githubusercontent.com/dragonwell-releng/jenkinsUserContent/master/checker/htmlReporter.py -O htmlReporter.py"
                    sh "python htmlReporter.py || true"
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
