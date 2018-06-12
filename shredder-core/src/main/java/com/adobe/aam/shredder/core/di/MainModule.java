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

package com.adobe.aam.shredder.core.di;

import com.adobe.aam.shredder.core.aws.TriggerHelper;
import com.adobe.aam.shredder.core.aws.TriggerWatcher;
import com.adobe.aam.shredder.core.command.MacroReplacer;
import com.amazonaws.services.autoscaling.AmazonAutoScaling;
import com.amazonaws.services.autoscaling.AmazonAutoScalingClientBuilder;
import com.amazonaws.services.sns.AmazonSNS;
import com.amazonaws.services.sns.AmazonSNSClientBuilder;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import com.amazonaws.util.EC2MetadataUtils;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.typesafe.config.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Named;

public class MainModule extends AbstractModule {

    private static final Logger LOG = LoggerFactory.getLogger(MainModule.class);

    @Override
    protected void configure() {

    }

    @Provides @Named("instanceId")
    public String getInstanceId() {
        return EC2MetadataUtils.getInstanceId();
    }

    @Provides @Named("region")
    public String getRegion() {
        return EC2MetadataUtils.getEC2InstanceRegion();
    }

    @Provides @Named("accountId")
    public String getAccountId() {
        return EC2MetadataUtils.getInstanceInfo().getAccountId();
    }

    @Provides @Named("snsTopic")
    public String getSnsTopic() {
        return "arn:aws:sns:" + getRegion() + ":" + getAccountId() + ":spinnaker-shutdowns-pending";
    }

    @Provides
    public AmazonSQS sqs(@Named("region") String region) {
        return AmazonSQSClientBuilder.standard().withRegion(region).build();
    }

    @Provides
    public AmazonSNS sns(@Named("region") String region) {
        return AmazonSNSClientBuilder.standard().withRegion(region).build();
    }

    @Provides
    public AmazonAutoScaling asg(@Named("region") String region) {
        return AmazonAutoScalingClientBuilder.standard().withRegion(region).build();
    }

    @Provides @Singleton
    public TriggerHelper helper(Config config,
                                @Named("instanceId") String instanceId,
                                @Named("snsTopic") String snsTopic,
                                AmazonSQS sqs, MacroReplacer macroReplacer,
                                AmazonSNS sns) {
        String queueName = macroReplacer.replaceMacros(config.getString("sqs_prefix")) + "-" + instanceId;
        LOG.info("SnsTopic: {}", snsTopic);
        TriggerHelper helper = new TriggerHelper(queueName, snsTopic, sqs, sns);
        helper.createQueue();
        helper.subscribeQueue();
        return helper;
    }

    @Provides @Singleton
    public TriggerWatcher watcher(TriggerHelper triggerHelper, AmazonSQS sqs, ObjectMapper objectMapper) {
        return new TriggerWatcher(triggerHelper.getQueueName(), sqs, objectMapper);
    }

    @Provides
    public ObjectMapper mapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true);
        return objectMapper;
    }

}
