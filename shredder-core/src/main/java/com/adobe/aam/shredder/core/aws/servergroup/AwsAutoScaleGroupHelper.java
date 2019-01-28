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

package com.adobe.aam.shredder.core.aws.servergroup;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonWebServiceRequest;
import com.amazonaws.services.autoscaling.AmazonAutoScaling;
import com.amazonaws.services.autoscaling.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;

public class AwsAutoScaleGroupHelper implements AutoScaleGroupHelper {

    private static final Logger LOG = LoggerFactory.getLogger(AwsAutoScaleGroupHelper.class);

    private AmazonAutoScaling asg;

    public AwsAutoScaleGroupHelper(AmazonAutoScaling asg) {

        this.asg = asg;
    }

    @Override
    public Optional<AutoScalingInstanceDetails> getCurrentAutoScalingGroup(String instanceId) {
        try {
            DescribeAutoScalingInstancesRequest request = new DescribeAutoScalingInstancesRequest().withInstanceIds(instanceId);
            DescribeAutoScalingInstancesResult describeResult = asg.describeAutoScalingInstances(request);

            List<AutoScalingInstanceDetails> asgs = describeResult.getAutoScalingInstances();
            if (asgs.isEmpty()) {
                return Optional.empty();
            }

            return Optional.ofNullable(asgs.iterator().next());
        } catch (AmazonClientException e) {
            LOG.error("Unable to fetch current AutoScaleGroup for instance: {} {}", instanceId, e);
            return Optional.empty();
        }
    }

    @Override
    public Optional<String> getLifecycleHookName(String autoScaleGroupName, String transitionType) {
        try {
            DescribeLifecycleHooksRequest request = new DescribeLifecycleHooksRequest()
                    .withAutoScalingGroupName(autoScaleGroupName);
            DescribeLifecycleHooksResult hooks = asg.describeLifecycleHooks(request);
            for (LifecycleHook hook : hooks.getLifecycleHooks()) {
                if (transitionType.equals(hook.getLifecycleTransition())) {
                    return Optional.ofNullable(hook.getLifecycleHookName());
                }
            }

            return Optional.empty();
        }  catch (AmazonClientException e) {
            LOG.error("Unable to fetch current Lifecycle Hook {}", transitionType, e);
            return Optional.empty();
        }
    }

    @Override
    public void recordLifecycleActionHeartbeat(RecordLifecycleActionHeartbeatRequest request) {
        asg.recordLifecycleActionHeartbeat(request);
    }

    @Override
    public void completeLifecycleAction(CompleteLifecycleActionRequest request) {
        asg.completeLifecycleAction(request);
    }
}
