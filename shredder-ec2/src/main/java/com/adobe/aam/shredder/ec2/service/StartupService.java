package com.adobe.aam.shredder.ec2.service;

import com.adobe.aam.shredder.ec2.log.ShredderLogUploader;
import com.adobe.aam.shredder.ec2.log.StartupResultPersist;
import com.adobe.aam.shredder.ec2.log.StartupResultPersist.Result;
import com.adobe.aam.shredder.ec2.notifier.Notifier;
import com.adobe.aam.shredder.ec2.service.startup.StartupRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.IOException;

public class StartupService {

    private static final Logger LOG = LoggerFactory.getLogger(StartupService.class);
    private final StartupRunner startupRunner;
    private final ShredderLogUploader shredderLogUploader;
    private final Notifier notifier;
    private final StartupResultPersist startupResultPersist;

    @Inject
    public StartupService(StartupRunner startupRunner,
                          ShredderLogUploader shredderLogUploader,
                          Notifier notifier, StartupResultPersist startupResultPersist) {
        this.startupRunner = startupRunner;
        this.shredderLogUploader = shredderLogUploader;
        this.notifier = notifier;
        this.startupResultPersist = startupResultPersist;
    }

    public boolean getStartupResult() throws IOException {
        Result previousStartup = startupResultPersist.getPreviousStartupResult();
        switch (previousStartup) {
            case STARTUP_NOT_RUN:
                LOG.info("Running startup scripts.");
                boolean startupSuccessful = runStartup();
                startupResultPersist.persist(startupSuccessful);
                return startupSuccessful;
            case SUCCESSFUL:
                LOG.info("Startup scripts already ran successfully in the past. Skipping.");
                return true;
            default:
                LOG.info("Startup scripts failed in the past. Skipping.");
                return false;
        }
    }

    private boolean runStartup() {
        boolean startupSuccessful = startupRunner.getStartupResult();
        LOG.info("Startup successful: {}", startupSuccessful);
        shredderLogUploader.uploadStartupLogs(startupSuccessful);
        notifier.notifyMonitoringServiceAboutStartup(startupSuccessful);
        notifier.notifyAutoScaleGroup(startupSuccessful);

        return startupSuccessful;
    }
}
