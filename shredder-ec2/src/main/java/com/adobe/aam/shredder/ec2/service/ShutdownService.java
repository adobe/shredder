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

package com.adobe.aam.shredder.ec2.service;

import com.adobe.aam.shredder.core.aws.TriggerHelper;
import com.adobe.aam.shredder.core.aws.TriggerWatcher;
import com.adobe.aam.shredder.core.command.ScriptRunner;
import com.adobe.aam.shredder.ec2.aws.LifecycleHandler;
import com.adobe.aam.shredder.ec2.trigger.TerminationMessage;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Named;

@Singleton
public class ShutdownService {
    private static final Logger LOG = LoggerFactory.getLogger(ShutdownService.class);

    private TriggerHelper triggerHelper;
    private TriggerWatcher triggerWatcher;
    private LifecycleHandler lifecycleHandler;
    private ScriptRunner scriptRunner;
    private String instanceId;

    @Inject
    ShutdownService(TriggerHelper triggerHelper,
                    TriggerWatcher triggerWatcher,
                    LifecycleHandler lifecycleHandler,
                    ScriptRunner scriptRunner,
                    @Named("instanceId") String instanceId) {
        this.triggerHelper = triggerHelper;
        this.triggerWatcher = triggerWatcher;
        this.lifecycleHandler = lifecycleHandler;
        this.scriptRunner = scriptRunner;
        this.instanceId = instanceId;
    }

    public void run() {
        triggerWatcher
                .requestTriggers(TerminationMessage.class)
                .filter(this::shouldTerminate)
                .subscribe(new TerminationSubscriber());

        LOG.info("Finished successfully. Exiting now.");
    }

    private boolean shouldTerminate(TerminationMessage trigger) {
        return "autoscaling:EC2_INSTANCE_TERMINATING".equals(trigger.getLifecycleTransition())
                && instanceId.equals(trigger.getEc2InstanceId());
    }

    private class TerminationSubscriber implements Subscriber<TerminationMessage> {

        private Subscription subscription;

        @Override
        public void onSubscribe(Subscription s) {
            this.subscription = s;
            s.request(Long.MAX_VALUE);
        }

        @Override
        public void onNext(TerminationMessage trigger) {
            LOG.info("Received TERMINATION signal.");
            subscription.cancel();
            scriptRunner.runScripts(() -> lifecycleHandler.sendHeartbeat(trigger));
            triggerHelper.cleanUp();
            lifecycleHandler.completeLifecycle(trigger);
        }

        @Override
        public void onError(Throwable t) {
            LOG.error("Received error when processing SQS messages.", t);
        }

        @Override
        public void onComplete() {
            LOG.error("Unexpected #onComplete call.");
        }
    }
}
