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

import com.adobe.aam.shredder.core.aws.servergroup.AutoScaleGroupHelper;
import com.amazonaws.AmazonClientException;
import com.amazonaws.services.autoscaling.model.CompleteLifecycleActionRequest;
import com.amazonaws.services.autoscaling.model.RecordLifecycleActionHeartbeatRequest;
import com.github.rholder.retry.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class LifecycleHandler {

    private static final Logger LOG = LoggerFactory.getLogger(LifecycleHandler.class);
    private final AutoScaleGroupHelper asgHelper;
    private final Retryer<Boolean> retryer = RetryerBuilder.<Boolean>newBuilder()
            .retryIfException(this::shouldRetryOnException)
            .withWaitStrategy(WaitStrategies.randomWait(2, TimeUnit.MINUTES))
            .withStopStrategy(StopStrategies.stopAfterAttempt(7))
            .withRetryListener(new RetryListener() {
                @Override
                public <V> void onRetry(Attempt<V> attempt) {
                    if (attempt.hasException()) {
                        LOG.warn("Failed to send lifecycle event to AWS. {}", attempt.getExceptionCause().getMessage());
                    }
                }
            })
            .build();

    @Inject
    public LifecycleHandler(AutoScaleGroupHelper asgHelper) {
        this.asgHelper = asgHelper;
    }

    public void sendHeartbeat(LifecycleHook message) {
        LOG.info("Sending heart beat to AWS ASG.");

        try {
            asgHelper.recordLifecycleActionHeartbeat(new RecordLifecycleActionHeartbeatRequest()
                    .withInstanceId(message.getEc2InstanceId())
                    .withAutoScalingGroupName(message.getAutoScalingGroupName())
                    .withLifecycleHookName(message.getLifecycleHookName())
            );
        } catch (AmazonClientException e) {
            LOG.warn("Unable to send heartbeat for lifecycle {}, {}", message, e.getMessage());
        }
    }

    public void successfulCompleteLifecycle(LifecycleHook message) {
        completeLifecycle(message, "CONTINUE");
    }

    public void abandonCompleteLifecycle(LifecycleHook message) {
        completeLifecycle(message, "ABANDON");
    }

    private void completeLifecycle(LifecycleHook message, String actionResult) {
        try {
            retryer.call(getCompleteLifecycleCommand(message, actionResult));
        } catch (ExecutionException | RetryException e) {
            LOG.warn("Unable to send COMPLETE lifecycle action for {}, {}", message, e.getMessage());
        }
    }

    private Callable<Boolean> getCompleteLifecycleCommand(LifecycleHook message, String actionResult) {

        return () -> {
            LOG.info("Sending a COMPLETE lifecycle action with actionResult: {}", actionResult);
            asgHelper.completeLifecycleAction(new CompleteLifecycleActionRequest()
                    .withLifecycleActionResult(actionResult)
                    .withInstanceId(message.getEc2InstanceId())
                    .withAutoScalingGroupName(message.getAutoScalingGroupName())
                    .withLifecycleHookName(message.getLifecycleHookName())
            );
            return true;
        };
    }

    /**
     * On some exceptions, a retry would yield no change. If we keep retrying we would lose time with the random wait,
     * so we want to avoid that.
     * For instance, if we can't find an active Lifecycle Action for the current instance id, we won't find any on
     * subsequent retries either.
     */
    private boolean shouldRetryOnException(Throwable throwable) {
        return throwable != null
                && throwable.getMessage() != null
                && !throwable.getMessage().contains("No active Lifecycle Action found");
    }
}
