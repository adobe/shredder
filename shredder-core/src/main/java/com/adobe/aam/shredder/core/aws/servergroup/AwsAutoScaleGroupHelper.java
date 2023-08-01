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
import com.amazonaws.services.autoscaling.AmazonAutoScaling;
import com.amazonaws.services.autoscaling.model.*;
import com.github.rholder.retry.Attempt;
import com.github.rholder.retry.RetryException;
import com.github.rholder.retry.RetryListener;
import com.github.rholder.retry.Retryer;
import com.github.rholder.retry.RetryerBuilder;
import com.github.rholder.retry.StopStrategies;
import com.github.rholder.retry.WaitStrategies;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class AwsAutoScaleGroupHelper implements AutoScaleGroupHelper {

    private static final Logger LOG = LoggerFactory.getLogger(AwsAutoScaleGroupHelper.class);

    private final Retryer<DescribeAutoScalingInstancesResult> describeAutoScalingInstancesRetryer = RetryerBuilder.<DescribeAutoScalingInstancesResult>newBuilder()
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

    private final Retryer<DescribeLifecycleHooksResult> describeLifecycleHooksRetryer = RetryerBuilder.<DescribeLifecycleHooksResult>newBuilder()
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

    private AmazonAutoScaling asg;

    public AwsAutoScaleGroupHelper(AmazonAutoScaling asg) {

        this.asg = asg;
    }

    @Override
    public Optional<AutoScalingInstanceDetails> getCurrentAutoScalingGroup(String instanceId) {
        try {
            DescribeAutoScalingInstancesRequest request = new DescribeAutoScalingInstancesRequest().withInstanceIds(instanceId);
            DescribeAutoScalingInstancesResult describeResult = describeAutoScalingInstancesRetryer.call(getCurrentAutoScalingGroupCommand(request));

            List<AutoScalingInstanceDetails> asgs = describeResult.getAutoScalingInstances();
            if (asgs.isEmpty()) {
                return Optional.empty();
            }

            return Optional.ofNullable(asgs.iterator().next());
        } catch (AmazonClientException | ExecutionException | RetryException e) {
            LOG.error("Unable to fetch current AutoScaleGroup for instance: {} {}", instanceId, e);
            return Optional.empty();
        }
    }

    private Callable<DescribeAutoScalingInstancesResult> getCurrentAutoScalingGroupCommand(DescribeAutoScalingInstancesRequest request) {
        return () -> asg.describeAutoScalingInstances(request);
    }

    @Override
    public Optional<String> getLifecycleHookName(String autoScaleGroupName, String transitionType) {
        try {
            DescribeLifecycleHooksRequest request = new DescribeLifecycleHooksRequest()
                    .withAutoScalingGroupName(autoScaleGroupName);
            DescribeLifecycleHooksResult hooks = describeLifecycleHooksRetryer.call(getLifecycleHookNameCommand(request));
            for (LifecycleHook hook : hooks.getLifecycleHooks()) {
                if (transitionType.equals(hook.getLifecycleTransition())) {
                    return Optional.ofNullable(hook.getLifecycleHookName());
                }
            }

            return Optional.empty();
        }  catch (AmazonClientException | ExecutionException | RetryException e) {
            LOG.error("Unable to fetch current Lifecycle Hook {}", transitionType, e);
            return Optional.empty();
        }
    }

    private Callable<DescribeLifecycleHooksResult> getLifecycleHookNameCommand(DescribeLifecycleHooksRequest request) {
        return () -> asg.describeLifecycleHooks(request);
    }

    @Override
    public void recordLifecycleActionHeartbeat(RecordLifecycleActionHeartbeatRequest request) {
        asg.recordLifecycleActionHeartbeat(request);
    }

    @Override
    public void completeLifecycleAction(CompleteLifecycleActionRequest request) {
        asg.completeLifecycleAction(request);
    }

    private boolean shouldRetryOnException(Throwable throwable) {
        return throwable != null
                && throwable.getMessage() != null
                && throwable.getMessage().contains("Rate exceeded");
    }
}
