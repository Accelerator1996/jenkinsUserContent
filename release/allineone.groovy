def skipRemainingStages = false, skipApprove = false, Exec = false, timeout_mins = 4320

pipeline {
    agent any
    options {
        ansiColor('xterm')
    }
    stages {
        stage("等待审批") {
            when {
                expression { !skipRemainingStages }
                expression { !skipApprove }
            }
            steps {
                script {
                    //等待审批人审批，并通过timeout设置任务过期时间，防止任务永远挂起
                    def userInput
                    timeout(timeout_mins) {
                        try {
                            userInput = input(
                                    id: 'inputap', message: "第一次审批", ok: "", submitter: "", parameters: [
                                    [$class: 'BooleanParameterDefinition', defaultValue: true, description: '<a href="http://ci.dragonwell-jdk.io/job/Test_openjdk17_dragonwell_sanity.system_x86-64_linux/23/tapTestReport/">测试报告</a> ', name: '测试结果检查'],
                            ])
                        } catch (err) { // input false
                            def user = err.getCauses()[0].getUser()
                            userInput = false
                            echo "\033[31m 任务已被审批人 ${user} 拒绝。 \033[0m"
                            currentBuild.result = 'ABORTED'
                        }
                    }
                    if (userInput == true) {
                        //发邮件待系统管理员执行任务
                        echo " 已审批完成，待系统管理员执行"

                        def userInput2
                        timeout(timeout_mins) {
                            try {
                                userInput2 = input(
                                        id: 'inputop', message: "第二次审批", ok: "执行", submitter: "", parameters: [
                                        [$class: 'BooleanParameterDefinition', defaultValue: true, description: '<a href="http://ci.dragonwell-jdk.io/job/dragonwell-artifact-checker/25/console">Github报告</a>', name: "Github check"],
                                        [$class: 'BooleanParameterDefinition', defaultValue: true, description: '<a href="http://ci.dragonwell-jdk.io/job/dragonwell-artifact-checker/25/console">Docker报告</a>', name: "Docker check"],
                                ])
                            } catch (err) { // input false
                                def user = err.getCauses()[0].getUser()
                                userInput2 = false
                                echo "\033[31m 任务已被系统管理员 ${user} 拒绝。 \033[0m"
                                currentBuild.result = 'ABORTED'
                            }
                        }

                        if (userInput2 == true) {
                            Exec = true
                        }

                    }
                }


            }
        }
        stage("任务执行") {
            when {
                expression { Exec }
            }
            steps {
                script {
                    print "发布"
                }
            }
        }
    }

}