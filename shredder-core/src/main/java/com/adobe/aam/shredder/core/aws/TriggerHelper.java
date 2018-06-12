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

package com.adobe.aam.shredder.core.aws;

import com.amazonaws.auth.policy.Policy;
import com.amazonaws.auth.policy.Principal;
import com.amazonaws.auth.policy.Resource;
import com.amazonaws.auth.policy.Statement;
import com.amazonaws.auth.policy.actions.SQSActions;
import com.amazonaws.auth.policy.conditions.ConditionFactory;
import com.amazonaws.services.sns.AmazonSNS;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.model.*;
import com.github.rholder.retry.*;
import com.google.common.collect.ImmutableList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

@Singleton
public class TriggerHelper {

    private static final Logger LOG = LoggerFactory.getLogger(TriggerHelper.class);

    private final String queueName;
    private final String snsTopic;
    private final AmazonSQS sqs;
    private final AmazonSNS sns;
    private String snsSubscriptionArn;
    private final Callable<Boolean> createQueueTask;

    @Inject
    public TriggerHelper(String queueName, String snsTopic, AmazonSQS sqs, AmazonSNS sns) {
        this.queueName = queueName;
        this.snsTopic = snsTopic;
        this.sqs = sqs;
        this.sns = sns;
        this.createQueueTask = () -> {
            try {
                LOG.info("Creating queue {}.", queueName);
                sqs.createQueue(getCreateQueueRequest(queueName));
            } catch (AmazonSQSException e) {
                if (!e.getErrorCode().equals("QueueAlreadyExists")) {
                    throw e;
                }
            }

            return true;
        };
    }

    private static CreateQueueRequest getCreateQueueRequest(String queueName) {
        return new CreateQueueRequest()
                .withQueueName(queueName)
                .addAttributesEntry(QueueAttributeName.VisibilityTimeout.toString(), "3600")
                .addAttributesEntry(QueueAttributeName.ReceiveMessageWaitTimeSeconds.toString(), "20");
    }

    /**
     * Creates SQS queue. Retries if the queue was recently deleted (eg. on app restart, the queue is cleaned up).
     */
    public synchronized void createQueue() {
        try {
            Retryer<Boolean> retryer = RetryerBuilder.<Boolean>newBuilder()
                    .retryIfException()
                    .withWaitStrategy(WaitStrategies.fixedWait(61, TimeUnit.SECONDS))
                    .withStopStrategy(StopStrategies.neverStop())
                    .withRetryListener(new RetryListener() {
                        @Override
                        public <V> void onRetry(Attempt<V> attempt) {
                            if (attempt.hasException()) {
                                LOG.info("Retrying to create queue in 61 seconds. {}", attempt.getExceptionCause().getMessage());
                            }
                        }
                    })
                    .build();
            retryer.call(createQueueTask);
        } catch (RetryException | ExecutionException e) {
            LOG.error("Received error while trying to create queue", e);
        }
    }

    public void subscribeQueue() {
        String queueUrl = getQueueUrl();
        LOG.info("Subscribe SNS to the queue {}.", queueUrl);

        String queueArn = sqs.getQueueAttributes(queueUrl, ImmutableList.of("QueueArn")).getAttributes().get("QueueArn");

        Policy policy = new Policy().withStatements(
                new Statement(Statement.Effect.Allow)
                        .withActions(SQSActions.SendMessage)
                        .withPrincipals(Principal.All)
                        .withResources(new Resource(queueArn))
                        .withConditions(ConditionFactory.newSourceArnCondition(snsTopic)));

        sqs.setQueueAttributes(new SetQueueAttributesRequest()
                .withQueueUrl(queueUrl)
                .addAttributesEntry(QueueAttributeName.Policy.toString(), policy.toJson())
        );

        snsSubscriptionArn = sns.subscribe(snsTopic, "sqs", queueArn).getSubscriptionArn();
        LOG.info("Subscribed SNS to dedicated SQS queue. subscriptionArn: {}", snsSubscriptionArn);
    }

    public String getQueueUrl() {
        GetQueueUrlResult queueUrlReult = sqs.getQueueUrl(queueName);
        return queueUrlReult.getQueueUrl();
    }

    public String getQueueName() {
        return queueName;
    }

    public synchronized void cleanUp() {
        LOG.info("Cleaning up SNS subscription {}.", snsSubscriptionArn);
        sns.unsubscribe(snsSubscriptionArn);

        String queueUrl = getQueueUrl();
        LOG.info("Cleaning up SQS queue {}.", queueUrl);
        sqs.deleteQueue(queueUrl);
    }
}
