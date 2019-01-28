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

package com.adobe.aam.shredder.core.aws.sns;

import com.amazonaws.AmazonClientException;
import com.amazonaws.auth.policy.Policy;
import com.amazonaws.auth.policy.Principal;
import com.amazonaws.auth.policy.Resource;
import com.amazonaws.auth.policy.Statement;
import com.amazonaws.auth.policy.actions.SQSActions;
import com.amazonaws.auth.policy.conditions.ConditionFactory;
import com.amazonaws.services.sns.AmazonSNS;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.model.QueueAttributeName;
import com.amazonaws.services.sqs.model.SetQueueAttributesRequest;
import com.google.common.collect.ImmutableList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AwsQueueSuscriber implements QueueSuscriber {

    private static final Logger LOG = LoggerFactory.getLogger(AwsQueueSuscriber.class);

    private final AmazonSQS sqs;
    private final AmazonSNS sns;

    public AwsQueueSuscriber(AmazonSQS sqs, AmazonSNS sns) {
        this.sqs = sqs;
        this.sns = sns;
    }

    @Override
    public String subscribeSnsToQueue(String snsTopic, String queueName) {
        try {
            String queueUrl = sqs.getQueueUrl(queueName).getQueueUrl();
            LOG.info("Subscribing SNS topic {} to the queue: {}", snsTopic, queueUrl);

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

            String snsSubscriptionArn = sns.subscribe(snsTopic, "sqs", queueArn).getSubscriptionArn();
            LOG.info("Subscribed SNS to dedicated SQS queue. subscriptionArn: {}", snsSubscriptionArn);

            return snsSubscriptionArn;
        } catch (AmazonClientException e) {
            LOG.info("Failed to subscribe to subscribe SNS to SQS queue", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public void unsubscribe(String snsSubscriptionArn) {

        try {
            LOG.info("Cleaning up SNS subscription {}.", snsSubscriptionArn);
            sns.unsubscribe(snsSubscriptionArn);
        } catch (AmazonClientException e) {
            LOG.info("Failed to unsubscribe SNS from SQS queue", e);
            throw new RuntimeException(e);
        }
    }
}
