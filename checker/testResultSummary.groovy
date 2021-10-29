import net.sf.json.groovy.JsonSlurper
import groovy.json.*


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

                }
            }
        }
    }
}
