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

package com.adobe.aam.shredder.ec2.di;

import com.adobe.aam.shredder.ec2.aws.InstanceDetailsRetriever;
import com.adobe.aam.shredder.ec2.aws.LifecycleHandler;
import com.adobe.aam.shredder.ec2.log.ShredderLogUploader;
import com.adobe.aam.shredder.ec2.log.StartupResultPersist;
import com.adobe.aam.shredder.ec2.notifier.Notifier;
import com.adobe.aam.shredder.ec2.runner.StartupCommandsRunner;
import com.adobe.aam.shredder.ec2.service.StartupService;
import com.adobe.aam.shredder.ec2.service.startup.NoStartupRunner;
import com.adobe.aam.shredder.ec2.service.startup.StartupExternalSignalReceiver;
import com.adobe.aam.shredder.ec2.service.startup.StartupRunner;
import com.adobe.aam.shredder.ec2.service.startup.StartupScriptRunner;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.typesafe.config.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Named;
import java.io.IOException;
import java.time.Duration;

public class StartupModule extends AbstractModule {

    private static final Logger LOG = LoggerFactory.getLogger(StartupModule.class);

    @Override
    public void configure() {
    }

    @Provides
    public StartupService monitoringService(Config config,
                                            @Named("heartbeatLambda") Runnable heartbeatLambda,
                                            StartupCommandsRunner startupCommandsRunner,
                                            ShredderLogUploader shredderLogUploader,
                                            Notifier notifier,
                                            StartupResultPersist startupResultPersist) throws IOException {


        StartupRunner runner = getStartupRunner(config, heartbeatLambda, startupCommandsRunner);
        return new StartupService(runner, shredderLogUploader, notifier, startupResultPersist);
    }

    private StartupRunner getStartupRunner(Config config,
                                           Runnable heartbeatLambda,
                                           StartupCommandsRunner startupCommandsRunner) throws IOException {
        String startupMode = config.getString("startup.mode");
        switch (startupMode) {
            case "run-startup-scripts":
                return new StartupScriptRunner(startupCommandsRunner);
            case "wait-external-http-signal":
                int serverPort = config.getInt("startup.external_signal.http.port");
                Duration timeoutMs = config.getDuration("startup.external_signal.timeout");
                return new StartupExternalSignalReceiver(serverPort, timeoutMs, heartbeatLambda);
            default:
                return new NoStartupRunner();
        }
    }

    @Provides
    @Named("heartbeatLambda")
    public Runnable getHeartbeatLambda(InstanceDetailsRetriever instanceDetailsRetriever,
                                       LifecycleHandler lifecycleHandler) {
        return instanceDetailsRetriever.getInstanceDetails()
                .<Runnable>map(trigger -> () -> lifecycleHandler.sendHeartbeat(trigger))
                .orElse(this::noHeartbeat);
    }

    private void noHeartbeat() {
        LOG.info("Could not connect to the startup lifecycle hook. No heartbeat being sent.");
    }
}
