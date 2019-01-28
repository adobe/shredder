/*
 * Copyright 2019 Adobe Systems Incorporated. All rights reserved.
 * This file is licensed to you under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License. You may obtain a copy
 * of the License at http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under
 * the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR REPRESENTATIONS
 * OF ANY KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */

package com.adobe.aam.shredder.ec2.service

import com.adobe.aam.shredder.ec2.log.ShredderLogUploader
import com.adobe.aam.shredder.ec2.log.StartupResultPersist
import com.adobe.aam.shredder.ec2.notifier.Notifier
import com.adobe.aam.shredder.ec2.runner.StartupCommandsRunner
import com.adobe.aam.shredder.ec2.service.startup.StartupScriptRunner
import spock.lang.Specification
import spock.lang.Unroll
import spock.util.concurrent.BlockingVariable

import static com.adobe.aam.shredder.ec2.log.StartupResultPersist.Result.STARTUP_NOT_RUN

class StartupServiceTest extends Specification {

    @Unroll("test startup service. startupScriptsResult=#startupScriptsResult")
    def "test startup service"(startupScriptsResult, expectedStartupSuccessful, expectedAsgNotified, expectedLogsUploaded) {
        given:
        def logsWereUploaded = new BlockingVariable<Boolean>()
        def asgWasNotified = new BlockingVariable<Boolean>()

        def startupCommandsRunner = Mock(StartupCommandsRunner) {
            getRunStartupScriptsResult() >> startupScriptsResult
        }
        def shredderLogUploader = Mock(ShredderLogUploader) {
            uploadStartupLogs(_) >> {
                logsWereUploaded.set(true)
            }
        }
        def startupNotifier = Mock(Notifier) {
            notifyAutoScaleGroup(_) >> {
                asgWasNotified.set(true)
            }

            notifyMonitoringServiceAboutStartup(_) >> {

            }
        }
        def startupResultPersist = Mock(StartupResultPersist) {
            getPreviousStartupResult() >> STARTUP_NOT_RUN
        }

        def runner = new StartupScriptRunner(startupCommandsRunner)
        def startupService = new StartupService(runner, shredderLogUploader, startupNotifier, startupResultPersist)

        when:
        def startupSuccessful = startupService.getStartupResult()
        then:
        startupSuccessful == expectedStartupSuccessful
        and:
        logsWereUploaded.get() == expectedLogsUploaded
        and:
        asgWasNotified.get() == expectedLogsUploaded

        where:
        startupScriptsResult | expectedStartupSuccessful | expectedAsgNotified | expectedLogsUploaded
        true                 | true                      | true                | true
        false                | false                     | true                | true
    }
}
