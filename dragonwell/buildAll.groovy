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
            when {
                // Only say hello if a "greeting" is requested
                expression { params.OSS == true }
            }
            agent {
                label 'binaries'
            }
            steps {
                script {

                }
            }
        }
    }
}