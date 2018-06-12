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

package com.adobe.aam.shredder.ec2.aws;

import com.adobe.aam.shredder.ec2.trigger.TerminationMessage;
import com.amazonaws.services.autoscaling.AmazonAutoScaling;
import com.amazonaws.services.autoscaling.AmazonAutoScalingClientBuilder;
import com.amazonaws.services.autoscaling.model.CompleteLifecycleActionRequest;
import com.amazonaws.services.autoscaling.model.RecordLifecycleActionHeartbeatRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;

public class LifecycleHandler {

    private static final Logger LOG = LoggerFactory.getLogger(LifecycleHandler.class);
    private final AmazonAutoScaling asg;

    @Inject
    public LifecycleHandler(AmazonAutoScaling asg) {
        this.asg = asg;
    }

    public void sendHeartbeat(TerminationMessage message) {
        LOG.info("Sending heart beat to AWS ASG.");
        asg.recordLifecycleActionHeartbeat(new RecordLifecycleActionHeartbeatRequest()
                .withInstanceId(message.getEc2InstanceId())
                .withLifecycleActionToken(message.getLifecycleActionToken())
                .withAutoScalingGroupName(message.getAutoScalingGroupName())
                .withLifecycleHookName(message.getLifecycleHookName())
        );
    }

    public void completeLifecycle(TerminationMessage message) {
        LOG.info("Sending a COMPLETE lifecycle action.");
        asg.completeLifecycleAction(new CompleteLifecycleActionRequest()
                .withLifecycleActionResult("CONTINUE")
                .withInstanceId(message.getEc2InstanceId())
                .withLifecycleActionToken(message.getLifecycleActionToken())
                .withAutoScalingGroupName(message.getAutoScalingGroupName())
                .withLifecycleHookName(message.getLifecycleHookName())
        );
    }
}
