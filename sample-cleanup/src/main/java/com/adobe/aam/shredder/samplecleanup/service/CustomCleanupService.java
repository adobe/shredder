/*
 *  Copyright 2018 Adobe Systems Incorporated. All rights reserved.
 *  This file is licensed to you under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License. You may obtain a copy
 *  of the License at http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under
 *  the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR REPRESENTATIONS
 *  OF ANY KIND, either express or implied. See the License for the specific language
 *  governing permissions and limitations under the License.
 */

package com.adobe.aam.shredder.samplecleanup.service;

import com.adobe.aam.shredder.core.aws.trigger.TriggerWatcher;
import com.adobe.aam.shredder.core.command.PlaybookRunner;
import com.adobe.aam.shredder.samplecleanup.trigger.ShutdownCompleteMessage;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class CustomCleanupService {
    private static final Logger LOG = LoggerFactory.getLogger(CustomCleanupService.class);

    private final TriggerWatcher triggerWatcher;
    private final PlaybookRunner playbookRunner;
    private final Runnable heartbeat = () -> LOG.info("Heartbeat: Still running command.");

    @Inject
    CustomCleanupService(TriggerWatcher triggerWatcher,
                         PlaybookRunner playbookRunner) {
        this.triggerWatcher = triggerWatcher;
        this.playbookRunner = playbookRunner;
    }

    public void listenForTriggers(String queueName) {
        triggerWatcher
                .requestTriggers(queueName, ShutdownCompleteMessage.class)
                .doOnNext(trigger -> LOG.info("Received SHUTDOWN complete signal: {}", trigger))
                .doOnNext(trigger -> playbookRunner.runCommands(trigger, heartbeat))
                .doOnNext(trigger -> LOG.info("Finished successfully: {}", trigger))
                .doOnError(e -> LOG.warn("Error while trying to process trigger.", e))
                .subscribe();

        LOG.info("Finished successfully. Exiting now.");
    }
}
