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
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import static com.adobe.aam.shredder.ec2.trigger.LifecycleHookMessage.Type.*;

@JsonIgnoreProperties(ignoreUnknown = true)
public class LifecycleHookMessage implements TriggerMessage, LifecycleHook {

    public enum Type {
        INITIALIZING,
        TERMINATING,
        UNKNOWN
    }
    private String lifecycleHookName;
    private String autoScalingGroupName;
    private String lifecycleActionToken;
    private String lifecycleTransition;
    private String ec2InstanceId;

    public String getLifecycleHookName() {
        return lifecycleHookName;
    }

    public String getAutoScalingGroupName() {
        return autoScalingGroupName;
    }

    public String getLifecycleActionToken() {
        return lifecycleActionToken;
    }

    public String getLifecycleTransition() {
        return lifecycleTransition;
    }

    public String getEc2InstanceId() {
        return ec2InstanceId;
    }
    
    @JsonIgnore
    public Type getType() {
        switch (getLifecycleTransition()) {
            case "autoscaling:EC2_INSTANCE_TERMINATING":
                return TERMINATING;
            case "autoscaling:EC2_INSTANCE_LAUNCHING":
                return INITIALIZING;
            default:
                return UNKNOWN;
        }
    }

    @Override
    public String toString() {
        return "LifecycleHookMessage{" +
                "lifecycleHookName='" + getLifecycleHookName() + '\'' +
                ", autoScalingGroupName='" + getAutoScalingGroupName() + '\'' +
                ", lifecycleActionToken='" + getLifecycleActionToken() + '\'' +
                ", lifecycleTransition='" + getLifecycleTransition() + '\'' +
                ", ec2InstanceId='" + getEc2InstanceId() + '\'' +
                ", type='" + getType() + '\'' +
                '}';
    }
}
