pipeline {
    agent {
        node { label 'pipelinepost' }
    }
    // not consider master right now
    stages {
        stage ('Invoke pipeline') {
            steps {
                copyArtifacts(
                        projectName: "${params.UPSTREAM_JOB_NAME}",
                        selector: specific("${params.UPSTREAM_JOB_NUMBER}"),
                        filter: "*",
                        fingerprintArtifacts: true,
                        target: "workspace/target/",
                        flatten: true)
                script {
                    dir("workspace/target") {
                        print "I am archiving " + params.RELEASE_TAG
                        def FILES_LIST = sh (script: "ls", returnStdout: true).trim()
                        echo "FILES_LIST : ${FILES_LIST}"
                        for(String ele : FILES_LIST.split("\\r?\\n")) {
                            sh "osscmd --host=oss-cn-hangzhou-zmf.aliyuncs.com --id=LTAI4FzxWYJNGoNLd9XJB42p    --key=4GGrozq5zH8lMkJnZO46ryCXnljnbZ put ${ele} oss://joeylee97/${ele}"
                        }
                        sh "mkdir -p /vmfarm/${params.RELEASE_TAG}/${params.RELEASE_TAG}"
                        sh "cp -r workspace/target/* /vmfarm/${params.VERSION}/${params.RELEASE_TAG}"
                    }
                    sh "rm -rf workspace/target/"
                }
            }
        }
    }
}