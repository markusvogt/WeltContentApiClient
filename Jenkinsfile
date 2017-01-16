node() {

    try {
        if (env.BRANCH_NAME == 'master' || env.BRANCH_NAME.startsWith("hotfix/")) {

            stage('Git') {
                sh """echo "building branch: ${env.BRANCH_NAME}" """
                checkout([$class                           : 'GitSCM',
                          branches                         : [[name: "*/${env.BRANCH_NAME}"]],
                          doGenerateSubmoduleConfigurations: false,
                          extensions                       : [],
                          submoduleCfg                     : [],
                          userRemoteConfigs                : [[
                                                                      url: 'git@github.com:WeltN24/WeltContentApiClient.git'
                                                              ]]
                ])
            }

            stage('test') {
                try {
                    wrap([$class: 'AnsiColorBuildWrapper', colorMapName: 'xterm']) {
                        sh './activator scalastyle'
                    }
                } finally {
                    step([$class: 'CheckStylePublisher', pattern: '**/scalastyle-result.xml'])
                }
                try {
                    wrap([$class: 'AnsiColorBuildWrapper', colorMapName: 'xterm']) {
                        sh './activator clean coverage test'
                    }
                } finally {
                    step([$class: 'JUnitResultArchiver', testResults: '**/target/test-reports/*.xml'])
                }
                try {
                    wrap([$class: 'AnsiColorBuildWrapper', colorMapName: 'xterm']) {
                        sh './activator coverageReport'
                        sh './activator coverageAggregate'
                    }
                } finally {
                    step([
                            $class    : 'ScoveragePublisher',
                            reportDir : 'target/scala-2.11/scoverage-report',
                            reportFile: 'scoverage.xml'
                    ])
                }
            }


            stage('Credential Setup Bintray') {
                withCredentials([[$class: 'StringBinding', credentialsId: 'BINTRAY_API_KEY_CI_WELTN24', variable: 'BINTRAY_API_KEY_CI_WELTN24']]) {
                    sh """
                    rm -f ~/.bintray/.credentials || true
                    mkdir -p ~/.bintray
                    touch ~/.bintray/.credentials
                    echo -e 'realm = Bintray API Realm' >> ~/.bintray/.credentials
                    echo -e 'host = api.bintray.com' >> ~/.bintray/.credentials
                    echo -e 'user = ci-weltn24' >> ~/.bintray/.credentials
                    echo -n 'password = ' >> ~/.bintray/.credentials
                """
                    sh "echo -e  " + env.BINTRAY_API_KEY_CI_WELTN24 + " >> ~/.bintray/.credentials"
                }
            }

            stage('Publish to bintray.com') {
                wrap([$class: 'AnsiColorBuildWrapper', colorMapName: 'xterm']) {
                    sh './activator publish'
                    sh 'PLAY24=true ./activator publish'
                }
                slackSend channel: 'section-tool-2', message: "Successfully published a new WeltContentApiClient version: ${env.BUILD_URL}"
            }

        } else {

            stage('Git') {
                echo """" Checking out "origin/pr/${env.CHANGE_ID}/merge". """

                checkout changelog: true, poll: false, scm: [
                        $class                           : 'GitSCM',
                        branches                         : [[ name: "origin/pr/${env.CHANGE_ID}/merge" ]],
                        doGenerateSubmoduleConfigurations: false,
                        extensions                       : [],
                        submoduleCfg                     : [],
                        userRemoteConfigs                : [[
                                                                    refspec: '+refs/heads/*:refs/remotes/origin/* +refs/pull/*:refs/remotes/origin/pr/*',
                                                                    url: 'git@github.com:WeltN24/WeltContentApiClient.git'
                                                            ]]
                ]
            }


            stage('Check') {
                wrap([$class: 'AnsiColorBuildWrapper', colorMapName: 'xterm']) {
                    sh """#!/bin/bash
                    ./activator test
                """
                }
            }

        }

    } catch (Exception e) {
        slackSend channel: 'section-tool-2', message: "Build failed: ${env.BUILD_URL}"
        throw e
    }
}
