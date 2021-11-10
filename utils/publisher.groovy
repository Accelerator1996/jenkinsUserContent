
pipeline {
    agent any
    stages {
        stage('text'){
            steps {
                echo '测试 TEXT 消息...'
            }
            post {
                success {
                    dingtalk (
                            robot: '2',
                            type: 'MARKDOWN',
                            title: 'Dragonwell Weekly Report',
                            text: [
                                    '# Github 状态',
                                    '[Dragonwell8下载报告](https://qii404.me/github-release-statistics/?repo=https://github.com/alibaba/dragonwell8.git)',
                                    '[Dragonwell11下载报告](https://qii404.me/github-release-statistics/?repo=https://github.com/alibaba/dragonwell11.git)',
                                    '[Dragonwell17下载报告](https://qii404.me/github-release-statistics/?repo=https://github.com/alibaba/dragonwell17.git)'
                            ]
                    )
                }
            }
        }
    }
}