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

package com.adobe.aam.shredder.core.aws.trigger;

import com.adobe.aam.shredder.core.aws.TriggerHelper;
import com.adobe.aam.shredder.core.trigger.TriggerMessage;
import com.amazonaws.AmazonClientException;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import com.amazonaws.services.sqs.model.QueueDoesNotExistException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.davidmoten.rx2.aws.Sqs;
import com.github.davidmoten.rx2.aws.SqsMessage;
import io.reactivex.Flowable;
import io.reactivex.Maybe;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class SqsTriggerWatcher implements TriggerWatcher {

    private static final Logger LOG = LoggerFactory.getLogger(SqsTriggerWatcher.class);
    private static final int MAX_SQS_DELAY_SECONDS = 30;

    private final String awsRegion;
    private final TriggerHelper triggerHelper;
    private final ObjectMapper objectMapper;

    public SqsTriggerWatcher(String awsRegion, TriggerHelper triggerHelper, ObjectMapper objectMapper) {
        this.awsRegion = awsRegion;
        this.triggerHelper = triggerHelper;
        this.objectMapper = objectMapper;
    }

    @Override
    public <T extends TriggerMessage> Flowable<T> requestTriggers(String queueName, Class<T> triggerType) {
        triggerHelper.createQueueAndSubscribe(queueName);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> triggerHelper.cleanUp(queueName)));

        LOG.info("Watching for triggers...");
        return Sqs.queueName(queueName)
                .sqsFactory(() -> sqs(awsRegion))
                .messages()
                .doOnError(t -> LOG.error("Received error while reading triggers from SQS. {}", t.getMessage()))
                .doOnError(t -> recreateQueueIfNecessary(t, queueName))
                .retryWhen(errors -> {
                    AtomicInteger counter = new AtomicInteger();
                    return errors.flatMap(e -> {
                        int attempt = counter.incrementAndGet();
                        int delaySeconds = Math.min(attempt, MAX_SQS_DELAY_SECONDS);
                        LOG.info("Failed at attempt {}. Will retry to read triggers in {} second(s).", attempt, delaySeconds);
                        return Flowable.timer(delaySeconds, TimeUnit.SECONDS);
                    });
                })
                .doOnNext(sqsMessage -> LOG.debug("Received message: {}", sqsMessage.message()))
                .doOnNext(SqsMessage::deleteMessage)
                .doFinally(() -> triggerHelper.cleanUp(queueName))
                .flatMapMaybe(message -> parseMessage(message, triggerType));
    }

    private AmazonSQS sqs(String awsRegion) {
        return AmazonSQSClientBuilder.standard().withRegion(awsRegion).build();
    }

    private void recreateQueueIfNecessary(Throwable throwable, String queueName) {
        // Recreate, in case the queue gets deleted by other means (eg. by mistake from the AWS Console)
        if (throwable instanceof QueueDoesNotExistException) {
            try {
                LOG.info("Recreating SQS queue {}", queueName);
                triggerHelper.unsubscribe();
                triggerHelper.createQueueAndSubscribe(queueName);
            } catch (AmazonClientException e) {
                LOG.error("Unable to recreate SQS queue", e);
            }
        }
    }

    private <T extends TriggerMessage> Maybe<T> parseMessage(SqsMessage message, Class<T> triggerType) {
        try {
            JSONObject json = new JSONObject(message.message());
            return Maybe.just(objectMapper
                    .readerFor(triggerType)
                    .readValue(json.getString("Message")));
        } catch (JSONException | IOException e) {
            LOG.error("Unexpected error while parsing JSON message {} as {}. {}", message.message(), triggerType, e);
            return Maybe.empty();
        }
    }
}
