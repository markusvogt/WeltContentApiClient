node('large') {

    stage('Git') {
        checkout scm
    }

    stage('Check') {
        wrap([$class: 'AnsiColorBuildWrapper', colorMapName: 'xterm']) {
            sh "./sbt clean test"
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
                            reportDir : 'target/scala-2.11/scoverage-report',
                            reportFile: 'scoverage.xml'
                    ])
                }
            }



            stage('Publish') {
                withCredentials([[$class: 'StringBinding', credentialsId: 'BINTRAY_API_KEY_CI_WELTN24', variable: 'BINTRAY_API_KEY_CI_WELTN24']]) {
                    writeFile file: '~/.bintray/.credentials', text: """realm = Bintray API Realm
host = api.bintray.com
user = ci-weltn24
password = ${env.BINTRAY_API_KEY_CI_WELTN24}"""
                }

                wrap([$class: 'AnsiColorBuildWrapper', colorMapName: 'xterm']) {
                    sh './sbt publish'
                }
                slackSend channel: 'section-tool-2', message: "Successfully published a new WeltContentApiClient version: ${env.BUILD_URL}"
            }

        }
    } catch (Exception e) {
        slackSend channel: 'section-tool-2', message: "Build failed: ${env.BUILD_URL}"
        throw e
    }
}
