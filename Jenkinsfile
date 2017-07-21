node('medium') {

    stage('Git') {
        checkout scm
    }

    if (env.BRANCH_NAME.startsWith('PR-')) {
        stage('Check') {
            wrap([$class: 'AnsiColorBuildWrapper', colorMapName: 'xterm']) {
                sh "./sbt clean test"
            }
        }
    }

    try {
        if (env.BRANCH_NAME == 'play_2_6') {

            stage('Test') {
                try {
                    wrap([$class: 'AnsiColorBuildWrapper', colorMapName: 'xterm']) {
                        sh './sbt scalastyle'
                    }
                } finally {
                    step([$class: 'CheckStylePublisher', pattern: '**/scalastyle-result.xml'])
                }
                try {
                    wrap([$class: 'AnsiColorBuildWrapper', colorMapName: 'xterm']) {
                        sh './sbt clean coverage test'
                    }
                } finally {
                    junit '**/target/test-reports/*.xml'
                }
                try {
                    wrap([$class: 'AnsiColorBuildWrapper', colorMapName: 'xterm']) {
                        sh './sbt coverageReport'
                        sh './sbt coverageAggregate'
                    }
                } finally {
                    step([
                            $class    : 'ScoveragePublisher',
                            reportDir : 'target/scala-2.12/scoverage-report',
                            reportFile: 'scoverage.xml'
                    ])
                }
            }



            stage('Publish') {
                withCredentials([[$class: 'StringBinding', credentialsId: 'BINTRAY_API_KEY_CI_WELTN24', variable: 'BINTRAY_PASS']]) {
                    // provide BINTRAY_{USER,PASS} as of https://github.com/sbt/sbt-bintray/blob/master/notes/0.5.0.markdown
                    env.BINTRAY_USER = "ci-weltn24"
                    wrap([$class: 'AnsiColorBuildWrapper', colorMapName: 'xterm']) {
                        sh './sbt publish'
                    }
                    slackSend channel: 'section-tool-2', message: "Successfully published a new WeltContentApiClient version: ${env.BUILD_URL}"
                }
            }
        }
    } catch (Exception e) {
        slackSend channel: 'section-tool-2', message: "Build failed: ${env.BUILD_URL}"
        throw e
    }
}
