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

package com.adobe.aam.shredder.core.di;

import com.adobe.aam.shredder.core.aws.TriggerHelper;
import com.adobe.aam.shredder.core.aws.queue.AwsQueueCreator;
import com.adobe.aam.shredder.core.aws.queue.NoopQueueCreator;
import com.adobe.aam.shredder.core.aws.queue.QueueCreator;
import com.adobe.aam.shredder.core.aws.servergroup.AutoScaleGroupHelper;
import com.adobe.aam.shredder.core.aws.servergroup.AwsAutoScaleGroupHelper;
import com.adobe.aam.shredder.core.aws.servergroup.NoopAutoScaleGroupHelper;
import com.adobe.aam.shredder.core.aws.sns.AwsQueueSuscriber;
import com.adobe.aam.shredder.core.aws.sns.NoopQueueSuscriber;
import com.adobe.aam.shredder.core.aws.sns.QueueSuscriber;
import com.adobe.aam.shredder.core.aws.trigger.NoopTriggerWatcher;
import com.adobe.aam.shredder.core.aws.trigger.SqsTriggerWatcher;
import com.adobe.aam.shredder.core.aws.trigger.TriggerWatcher;
import com.adobe.aam.shredder.core.aws.ArnHelper;
import com.amazonaws.services.autoscaling.AmazonAutoScaling;
import com.amazonaws.services.autoscaling.AmazonAutoScalingClientBuilder;
import com.amazonaws.services.sns.AmazonSNS;
import com.amazonaws.services.sns.AmazonSNSClientBuilder;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import com.amazonaws.util.EC2MetadataUtils;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class ShredderModule {

    @Bean(name = "awsEnabled")
    public boolean awsEnabled(ShredderConfig config) {
        return config.isAwsEnabled();
    }

    @Bean(name = "instanceId")
    public String getInstanceId(ShredderConfig config) {
        String instanceId = config.getInstanceId();
        return Optional.ofNullable(instanceId).orElseGet(EC2MetadataUtils::getInstanceId);
    }

    @Bean(name = "region")
    public String getRegion(ShredderConfig config) {
        String region = config.getRegion();
        return Optional.ofNullable(region).orElseGet(EC2MetadataUtils::getEC2InstanceRegion);
    }

    @Bean(name = "accountId")
    public String getAccountId(ShredderConfig config) {
        String accountId = config.getAccountId();
        return Optional.ofNullable(accountId).orElseGet(() -> EC2MetadataUtils.getInstanceInfo().getAccountId());
    }

    @Bean(name = "snsTopic")
    public String getSnsTopic(ShredderConfig config) {
        return ArnHelper.getArnByResourceName(
                config.getSnsTopic(),
                getRegion(config),
                getAccountId(config),
                "sns");
    }

    @Bean
    public QueueCreator queueCreator(@Qualifier("awsEnabled") boolean awsEnabled,
                                     @Qualifier("region") String region) {
        if (awsEnabled) {
            return new AwsQueueCreator(sqs(region));
        }

        return new NoopQueueCreator();
    }

    @Bean
    public QueueSuscriber snsSubcriber(@Qualifier("awsEnabled") boolean awsEnabled,
                                       @Qualifier("region") String region) {
        if (awsEnabled) {
            return new AwsQueueSuscriber(sqs(region), sns(region));
        }

        return new NoopQueueSuscriber();
    }

    @Bean
    public TriggerWatcher triggerWatcher(@Qualifier("awsEnabled") boolean awsEnabled,
                                         @Qualifier("region") String region,
                                         TriggerHelper triggerHelper,
                                         ObjectMapper objectMapper) {
        if (awsEnabled) {
            return new SqsTriggerWatcher(region, triggerHelper, objectMapper);
        }
        return new NoopTriggerWatcher();
    }

    @Bean
    public TriggerHelper triggerHelper(QueueCreator queueCreator,
                                       QueueSuscriber snsSubcriber,
                                       @Qualifier("snsTopic") String snsTopic) {
        return new TriggerHelper(queueCreator, snsSubcriber, snsTopic);
    }

    @Bean
    public AutoScaleGroupHelper autoScaleGroupHelper(@Qualifier("awsEnabled") boolean awsEnabled,
                                                     @Qualifier("region") String region) {
        if (awsEnabled) {
            return new AwsAutoScaleGroupHelper(asg(region));
        }
        return new NoopAutoScaleGroupHelper();
    }

    @Bean(name = "queueName")
    public String queueName(ShredderConfig config,
                            @Qualifier("instanceId") String instanceId) {
        return config.getSqsPrefix() + "-" + instanceId;
    }

    @Bean(name = "appName")
    public String appName(ShredderConfig config) {
        return config.getAppName();
    }

    @Bean
    public ObjectMapper mapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true);
        return objectMapper;
    }

    private AmazonSNS sns(@Qualifier("region") String region) {
        return AmazonSNSClientBuilder.standard().withRegion(region).build();
    }

    private AmazonSQS sqs(@Qualifier("region") String region) {
        return AmazonSQSClientBuilder.standard().withRegion(region).build();
    }

    private AmazonAutoScaling asg(@Qualifier("region") String region) {
        return AmazonAutoScalingClientBuilder.standard().withRegion(region).build();
    }
}
