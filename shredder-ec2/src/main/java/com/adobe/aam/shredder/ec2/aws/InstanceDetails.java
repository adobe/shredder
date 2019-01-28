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

package com.adobe.aam.shredder.ec2.aws;

public class InstanceDetails implements LifecycleHook {

    private final String ec2InstanceId;
    private final String lifecycleHookName;
    private final String autoScalingGroupName;
    private final String regionName;

    public InstanceDetails(
            String ec2InstanceId,
            String lifecycleHookName,
            String autoScalingGroupName,
            String regionName
    ) {
        this.ec2InstanceId = ec2InstanceId;
        this.lifecycleHookName = lifecycleHookName;
        this.autoScalingGroupName = autoScalingGroupName;
        this.regionName = regionName;
    }

    public String getEc2InstanceId() {
        return ec2InstanceId;
    }

    public String getLifecycleHookName() {
        return lifecycleHookName;
    }

    public String getAutoScalingGroupName() {
        return autoScalingGroupName;
    }

    public String getRegionName() { return regionName; }

    @Override
    public String toString() {
        return "InstanceDetails{" +
                "ec2InstanceId='" + ec2InstanceId + '\'' +
                ", lifecycleHookName='" + lifecycleHookName + '\'' +
                ", autoScalingGroupName='" + autoScalingGroupName + '\'' +
                ", regionName='" + regionName + '\'' +
                '}';
    }
}
