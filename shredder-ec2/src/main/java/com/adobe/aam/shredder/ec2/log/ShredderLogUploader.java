package com.adobe.aam.shredder.ec2.log;

import com.adobe.aam.shredder.core.log.RemoteLogUploader;

import javax.inject.Inject;

public class ShredderLogUploader {
    private final RemoteLogUploader logsUploader;

    @Inject
    public ShredderLogUploader(RemoteLogUploader logsUploader) {
        this.logsUploader = logsUploader;
    }

    public void uploadStartupLogs(boolean startupSuccessful) {
        logsUploader.upload("startup", startupSuccessful);
    }

    public void uploadShutdownLogs(boolean shutdownSuccessful) {
        logsUploader.upload("shutdown", shutdownSuccessful);
    }
}
