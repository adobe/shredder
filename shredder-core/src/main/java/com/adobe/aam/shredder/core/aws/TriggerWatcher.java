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

package com.adobe.aam.shredder.core.aws;

import com.adobe.aam.shredder.core.trigger.TriggerMessage;
import com.amazonaws.services.sqs.AmazonSQS;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.davidmoten.rx2.aws.Sqs;
import com.github.davidmoten.rx2.aws.SqsMessage;
import io.reactivex.Flowable;
import io.reactivex.Maybe;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.IOException;

@Singleton
public class TriggerWatcher {

    private static final Logger LOG = LoggerFactory.getLogger(TriggerWatcher.class);

    private String queueName;
    private AmazonSQS sqs;
    private final ObjectMapper objectMapper;

    @Inject
    public TriggerWatcher(String queueName, AmazonSQS sqs, ObjectMapper objectMapper) {
        this.queueName = queueName;
        this.sqs = sqs;
        this.objectMapper = objectMapper;
    }

    public <T extends TriggerMessage> Flowable<T> requestTriggers(Class<T> triggerType) {
        return Sqs.queueName(queueName)
                .sqsFactory(() -> sqs)
                .messages()
                .doOnError(t -> LOG.error("Received error while reading triggers from SQS", t))
                .doOnNext(message -> LOG.debug("Received message: {}", message))
                .doOnNext(SqsMessage::deleteMessage)
                .flatMapMaybe(message -> parseMessage(message, triggerType));
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
