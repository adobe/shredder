package com.adobe.aam.shredder.ec2.service;

import com.adobe.aam.shredder.ec2.aws.LifecycleHandler;
import com.adobe.aam.shredder.ec2.aws.LifecycleHook;
import com.adobe.aam.shredder.ec2.log.ShredderLogUploader;
import com.adobe.aam.shredder.ec2.notifier.Notifier;
import com.adobe.aam.shredder.ec2.runner.BlockOnShutdownFailure;
import com.adobe.aam.shredder.ec2.runner.ShutdownCommandsRunner;
import com.adobe.aam.shredder.ec2.trigger.ShutdownLifecycleHookMessage;
import com.adobe.aam.shredder.ec2.trigger.ShutdownTriggerListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;

public class ShutdownService {

    private static final Logger LOG = LoggerFactory.getLogger(ShutdownService.class);

    private final ShutdownTriggerListener shutdownTriggerListener;
    private final Notifier notifier;
    private final ShutdownCommandsRunner shutdownCommandsRunner;
    private final ShredderLogUploader shredderLogUploader;
    private final BlockOnShutdownFailure block;
    private final boolean shutdownOnStartupFail;
    private final LifecycleHandler lifecycleHandler;

    @Inject
    public ShutdownService(ShutdownTriggerListener shutdownTriggerListener,
                           Notifier notifier,
                           ShutdownCommandsRunner shutdownCommandsRunner,
                           ShredderLogUploader shredderLogUploader,
                           BlockOnShutdownFailure block,
                           @Named("shutdownOnStartupFail") boolean shutdownOnStartupFail,
                           LifecycleHandler lifecycleHandler) {
        this.shutdownTriggerListener = shutdownTriggerListener;
        this.notifier = notifier;
        this.shutdownCommandsRunner = shutdownCommandsRunner;
        this.shredderLogUploader = shredderLogUploader;
        this.block = block;
        this.shutdownOnStartupFail = shutdownOnStartupFail;
        this.lifecycleHandler = lifecycleHandler;
    }

    public boolean getShutdownResult(boolean startupSuccessful) {
        ShutdownLifecycleHookMessage trigger = shutdownTriggerListener.listenForShutdownTrigger();

        boolean shutdownSuccessful = shouldShutdown(startupSuccessful, trigger);
        shredderLogUploader.uploadShutdownLogs(shutdownSuccessful);
        notifier.notifyMonitoringServiceAboutShutdown(shutdownSuccessful);
        if (shutdownSuccessful) {
            terminateInstance(trigger);
        } else {
            // Block till someone fixes the issue.
            blockOnShutdownFailure(trigger);
        }

        return shutdownSuccessful;
    }

    private boolean shouldShutdown(boolean startupSuccessful, ShutdownLifecycleHookMessage trigger) {
        if (startupSuccessful) {
            return executeShutdownScripts(trigger);
        }

        return shutdownOnStartupFail;
    }

    private void blockOnShutdownFailure(ShutdownLifecycleHookMessage trigger) {
        LOG.error("Shutdown failed.");
        block.blockOnShutdownFailure(trigger);
        LOG.error("Shutdown block time expired.");
    }

    private boolean executeShutdownScripts(ShutdownLifecycleHookMessage trigger) {
        return shutdownCommandsRunner.getRunShutdownScriptsResult(trigger);
    }

    public void terminateInstance(LifecycleHook trigger) {
        lifecycleHandler.successfulCompleteLifecycle(trigger);
    }
}
