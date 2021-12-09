def RELEASE = 17
def SKIP_DUPLICATE = false
def BN
//OSS_TOOL = "/home/testuser/ossutil64"
TOKEN = "ghp_KI10VDceecSlImTXHnhK0cWm7prLUc0oFsU" + "S"
REPO = "dragonwell${params.RELEASE}-binaries"

pipeline {
    options {
        durabilityHint('PERFORMANCE_OPTIMIZED')
        buildDiscarder(logRotator(numToKeepStr: '15', artifactDaysToKeepStr: '15'))
    }
    agent {
        label 'binaries'
    }
    parameters {
        string(name: 'SCM',
                defaultValue: "",
                description: "scm hash value")
    }
    stages {
        stage('check') {
            agent {
                label 'binaries'
            }
            steps {
                script {
                    dir("/data/dragonwell${RELEASE}-binaries") {
                        def hash = sh returnStdout: true, script: "git rev-parse HEAD"
                        def wwh
                        dir("/data/dragonwell${RELEASE}") {
                            www = sh(script: "cat META", returnStdout: true).trim()
                        }
                        if (wwh.contains("${hash}")) {
                            SKIP_DUPLICATE = true
                        }
                    }
                }
            }

            stage('build') {
                when {
                    expression { (!SKIP_DUPLICATE) }
                }
                agent {
                    label 'binaries'
                }
                steps {
                    script {
                        build quietPeriod: 10, job: 'github-trigger-pipelines/openjdk17-pipeline-commit', parameters: [text(name: 'targetConfigurations', value: '''{
        "x64Linux"    : [
                "dragonwell"
        ],
        "x64Windows"  : [
                "dragonwell"
        ],
        "aarch64Linux": [
                "dragonwell"
        ],
        "x64AlpineLinux": [
                "dragonwell"
        ]
}'''), string(name: 'activeNodeTimeout', value: '0'), string(name: 'jdkVersion', value: '17'), string(name: 'dockerExcludes', value: ''), string(name: 'libraryPath', value: ''), string(name: 'baseFilePath', value: ''), string(name: 'buildConfigFilePath', value: ''), string(name: 'releaseType', value: 'Release'), string(name: 'overridePublishName', value: ''), string(name: 'scmReference', value: "${params.SCM}"), booleanParam(name: 'enableTests', value: false), booleanParam(name: 'enableInstallers', value: false), booleanParam(name: 'enableSigner', value: false), string(name: 'additionalConfigureArgs', value: ''), string(name: 'additionalBuildArgs', value: ''), string(name: 'overrideFileNameVersion', value: ''), booleanParam(name: 'useAdoptBashScripts', value: false), booleanParam(name: 'cleanWorkspaceBeforeBuild', value: false), booleanParam(name: 'cleanWorkspaceAfterBuild', value: false), booleanParam(name: 'cleanWorkspaceBuildOutputAfterBuild', value: true), booleanParam(name: 'propagateFailures', value: false), booleanParam(name: 'keepTestReportDir', value: false), booleanParam(name: 'keepReleaseLogs', value: true), string(name: 'adoptBuildNumber', value: ''), text(name: 'defaultsJson', value: '''{
    "repository": {
        "build_url": "https://github.com.cnpmjs.org/dragonwell-releng/openjdk-build.git",
        "build_branch": "master",
        "pipeline_url": "https://github.com.cnpmjs.org/dragonwell-releng/ci-jenkins-pipelines.git",
        "pipeline_branch": "master"
    },
    "jenkinsDetails": {
        "rootUrl": "http://ci.dragonwell-jdk.io/",
        "rootDirectory": "build-scripts"
    },
    "templateDirectories": {
        "downstream": "pipelines/build/common/create_job_from_template.groovy",
        "upstream": "pipelines/jobs/pipeline_job_template.groovy",
        "weekly": "pipelines/jobs/weekly_release_pipeline_job_template.groovy"
    },
    "configDirectories": {
        "build": "pipelines/jobs/dragonwell_configurations",
        "nightly": "pipelines/jobs/dragonwell_configurations",
        "platform": "build-farm/platform-specific-configurations"
    },
    "scriptDirectories": {
        "upstream": "pipelines/build",
        "downstream": "pipelines/build/common/kick_off_build.groovy",
        "weekly": "pipelines/build/common/weekly_release_pipeline.groovy",
        "regeneration": "pipelines/build/common/config_regeneration.groovy",
        "tester": "pipelines/build/prTester/pr_test_pipeline.groovy",
        "buildfarm": "build-farm/make-adopt-build-farm.sh"
    },
    "baseFileDirectories": {
        "upstream": "pipelines/build/common/build_base_file.groovy",
        "downstream": "pipelines/build/common/openjdk_build_pipeline.groovy"
    },
    "importLibraryScript": "pipelines/build/common/import_lib.groovy",
    "defaultsUrl": "http://ci.dragonwell-jdk.io/userContent/config/defaults.json"
}'''), text(name: 'adoptDefaultsJson', value: '''{
    "repository": {
        "build_url": "https://github.com.cnpmjs.org/dragonwell-releng/openjdk-build.git",
        "build_branch": "master",
        "pipeline_url": "https://github.com.cnpmjs.org/dragonwell-releng/ci-jenkins-pipelines.git",
        "pipeline_branch": "master"
    },
    "jenkinsDetails": {
        "rootUrl": "http://ci.dragonwell-jdk.io/",
        "rootDirectory": "build-scripts"
    },
    "templateDirectories": {
        "downstream": "pipelines/build/common/create_job_from_template.groovy",
        "upstream": "pipelines/jobs/pipeline_job_template.groovy",
        "weekly": "pipelines/jobs/weekly_release_pipeline_job_template.groovy"
    },
    "configDirectories": {
        "build": "pipelines/jobs/dragonwell_configurations",
        "nightly": "pipelines/jobs/dragonwell_configurations",
        "platform": "build-farm/platform-specific-configurations"
    },
    "scriptDirectories": {
        "upstream": "pipelines/build",
        "downstream": "pipelines/build/common/kick_off_build.groovy",
        "weekly": "pipelines/build/common/weekly_release_pipeline.groovy",
        "regeneration": "pipelines/build/common/config_regeneration.groovy",
        "tester": "pipelines/build/prTester/pr_test_pipeline.groovy",
        "buildfarm": "build-farm/make-adopt-build-farm.sh"
    },
    "baseFileDirectories": {
        "upstream": "pipelines/build/common/build_base_file.groovy",
        "downstream": "pipelines/build/common/openjdk_build_pipeline.groovy"
    },
    "importLibraryScript": "pipelines/build/common/import_lib.groovy",
    "defaultsUrl": "http://ci.dragonwell-jdk.io/userContent/config/defaults.json"
}''')]
                    }
                }
            }
            stage('copyToData') {
                agent {
                    label 'binaries'
                }
                steps {
                    script {
                        dir("/data/dragonwell${RELEASE}-binaries") {
                            int data = 0
                            def hash = sh returnStdout: true, script: "git rev-parse HEAD"
                            if (fileExists("BUILD")) {
                                data = Integer.parseInt(readFile(file: "BUILD").split("\\r?\\n")[0])
                                data = data + 1
                            }
                            BN = data
                            writeFile file: 'BUILD', text: "${data}"
                            sh "echo \"${data} ${hash}\" >> META"
                            sh "git add META BUILD  && git commit -m \"update build\" && git push origin"

                            copyArtifacts filter: '*', flatten: true, projectName: 'github-trigger-pipelines/openjdk17-pipeline-commit', selector: lastSuccessful(), target: "/data/17/${data}"
                        }
                    }
                }
            }

            stage('githubUpload') {
                when {
                    expression { (!SKIP_DUPLICATE) }
                }
                agent {
                    label 'binaries'
                }
                steps {
                    script {
                        dir("/data/dragonwell${RELEASE}-binaries") {
                            sh "git tag HEAD ${BN} && git push orgin ${BN} || true"
                            def files = sh(script: "ls /data/${params.RELEASE}/${BN}", returnStdout: true).split()
                            def releaseJson = new JsonBuilder([
                                    "tag_name"        : "${BN}",
                                    "target_commitish": "master",
                                    "name"            : "Alibaba_Dragonwell_${params.RELEASE}_${BN}",
                                    "body"            : "",
                                    "draft"           : false,
                                    "prerelease"      : false
                            ])
                            writeFile file: 'release.json', text: releaseJson.toPrettyString()
                            def release = sh(script: "curl -XPOST -H \"Authorization:token ${TOKEN}\" --data @release.json https://api.github.com/repos/alibaba/${ragonwell${RELEASE}-binaries}/releases", returnStdout: true)
                            writeFile file: 'result.json', text: "${release}"
                            def id = sh(script: "cat result.json | grep id | head -n 1 | awk -F\"[:,]\"  '{ print \$2 }' | awk '{print \$1}'", returnStdout: true)
                            id = id.trim()
                            for (file in files) {
                                sh "curl -Ss -XPOST -H \"Authorization:token ${TOKEN}\" -H \"Content-Type:application/zip\" --data-binary @${assetName} https://uploads.github.com/repos/dragonwell-releng/${REPO}/releases/${id}/assets?name=${file}"
                            }
                        }
                    }
                }
            }
        }
    }
}