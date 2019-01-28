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

import com.adobe.aam.shredder.core.aws.queue.QueueCreator;
import com.adobe.aam.shredder.core.aws.sns.QueueSuscriber;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

@Singleton
public class TriggerHelper {

    private final QueueCreator queueCreator;
    private final QueueSuscriber snsSubcriber;
    private final String snsTopic;
    private String snsSubscriptionArn;
    private boolean released;

    @Inject
    public TriggerHelper(QueueCreator queueCreator,
                         QueueSuscriber snsSubcriber,
                         @Named("snsTopic") String snsTopic) {
        this.snsTopic = snsTopic;
        this.queueCreator = queueCreator;
        this.snsSubcriber = snsSubcriber;
    }

    public synchronized void createQueueAndSubscribe(String queueName) {
        queueCreator.createQueue(queueName);
        snsSubscriptionArn = snsSubcriber.subscribeSnsToQueue(snsTopic, queueName);
    }

    public synchronized void cleanUp(String queueName) {
        if (released) {
            return;
        }

        released = true;
        unsubscribe();
        queueCreator.deleteQueue(queueName);
    }

    public void unsubscribe() {
        snsSubcriber.unsubscribe(snsSubscriptionArn);
    }
}
