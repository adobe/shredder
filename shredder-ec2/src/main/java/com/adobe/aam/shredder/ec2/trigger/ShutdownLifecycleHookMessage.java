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

package com.adobe.aam.shredder.ec2.trigger;

import com.adobe.aam.shredder.core.trigger.TriggerMessage;
import com.adobe.aam.shredder.ec2.aws.LifecycleHook;

import java.util.Optional;

public class ShutdownLifecycleHookMessage implements TriggerMessage, LifecycleHook {

    private final String ec2InstanceId;
    private final String lifecycleHookName;
    private final String autoScaleGroupname;

    private ShutdownLifecycleHookMessage(LifecycleHookMessage message) {
        ec2InstanceId = message.getEc2InstanceId();
        lifecycleHookName = message.getLifecycleHookName();
        autoScaleGroupname = message.getAutoScalingGroupName();
    }

    /**
     * Converts generic LifecycleHookMessage to a ShutdownLifecycleHookMessage, if possible.
     */
    public static Optional<ShutdownLifecycleHookMessage> of(LifecycleHookMessage message) {
        return message.getType() == LifecycleHookMessage.Type.TERMINATING
                ? Optional.of(new ShutdownLifecycleHookMessage(message))
                : Optional.empty();
    }

    @Override
    public String getEc2InstanceId() {
        return ec2InstanceId;
    }

    @Override
    public String getLifecycleHookName() {
        return lifecycleHookName;
    }

    @Override
    public String getAutoScalingGroupName() {
        return autoScaleGroupname;
    }
}
