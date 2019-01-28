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

package com.adobe.aam.shredder.ec2.trigger;

import com.adobe.aam.shredder.core.aws.trigger.TriggerWatcher;
import com.google.inject.Inject;
import io.reactivex.schedulers.Schedulers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Named;
import java.util.Optional;

public class ShutdownTriggerListener {
    private static final Logger LOG = LoggerFactory.getLogger(ShutdownTriggerListener.class);

    private final TriggerWatcher triggerWatcher;
    private final String instanceId;
    private String queueName;

    @Inject
    ShutdownTriggerListener(TriggerWatcher triggerWatcher,
                            @Named("instanceId") String instanceId,
                            @Named("queueName") String queueName) {
        this.triggerWatcher = triggerWatcher;
        this.instanceId = instanceId;
        this.queueName = queueName;
    }

    public ShutdownLifecycleHookMessage listenForShutdownTrigger() {
         return triggerWatcher
                .requestTriggers(queueName, LifecycleHookMessage.class)
                .filter(trigger -> instanceId.equals(trigger.getEc2InstanceId()))
                .map(ShutdownLifecycleHookMessage::of)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .doOnNext(trigger -> LOG.info("Received shutdown trigger: {}", trigger))
                .subscribeOn(Schedulers.io())
                .blockingFirst();
    }
}
