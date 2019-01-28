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

package com.adobe.aam.shredder.samplecleanup;

import com.adobe.aam.shredder.core.aws.TriggerHelper;
import com.adobe.aam.shredder.samplecleanup.di.InjectorBuilder;
import com.adobe.aam.shredder.samplecleanup.service.CustomCleanupService;
import com.google.inject.Injector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {
    private static final Logger LOG = LoggerFactory.getLogger(Main.class);

    public static void main(String argv[]) {

        Injector injector = InjectorBuilder.build();

        LOG.info("Starting Cleanup daemon.");

        TriggerHelper triggerHelper = injector.getInstance(TriggerHelper.class);

        String queueName = "TBD";
        triggerHelper.createQueueAndSubscribe(queueName);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> deleteResources(triggerHelper, queueName)));

        CustomCleanupService service = injector.getInstance(CustomCleanupService.class);
        service.listenForTriggers(queueName);

        deleteResources(triggerHelper, queueName);
    }

    public static void deleteResources(TriggerHelper triggerHelper, String queueName) {
        triggerHelper.cleanUp(queueName);
        LOG.info("Finished. Exiting now.");
    }
}
