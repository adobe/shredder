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
