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
import com.adobe.aam.shredder.core.command.MacroReplacer;
import com.adobe.aam.shredder.core.command.ScriptRunner;
import com.amazonaws.services.autoscaling.AmazonAutoScaling;
import com.amazonaws.services.autoscaling.AmazonAutoScalingClientBuilder;
import com.amazonaws.services.sns.AmazonSNS;
import com.amazonaws.services.sns.AmazonSNSClientBuilder;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import com.amazonaws.util.EC2MetadataUtils;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Splitter;
import com.google.inject.Binder;
import com.google.inject.Module;
import com.google.inject.Provides;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import com.typesafe.config.Config;

import javax.inject.Named;
import javax.inject.Singleton;
import java.util.List;

public class MainModule implements Module {

    @Override
    public void configure(Binder binder) {
        binder.install(new FactoryModuleBuilder().build(ScriptRunner.ScriptRunnerFactory.class));
    }

    @Provides
    @Named("awsEnabled")
    public boolean awsEnabled(Config config) {
        return config.getBoolean("aws.enabled");
    }

    @Provides
    @Named("instanceId")
    public String getInstanceId(Config config) {
        return config.hasPath("AWS_INSTANCE_ID")
                ? config.getString("AWS_INSTANCE_ID")
                : EC2MetadataUtils.getInstanceId();
    }

    @Provides
    @Named("region")
    public String getRegion(Config config) {
        return config.hasPath("AWS_REGION")
                ? config.getString("AWS_REGION")
                : EC2MetadataUtils.getEC2InstanceRegion();
    }

    @Provides
    @Named("accountId")
    public String getAccountId(Config config) {
        return config.hasPath("AWS_ACCOUNT_ID")
                ? config.getString("AWS_ACCOUNT_ID")
                : EC2MetadataUtils.getInstanceInfo().getAccountId();
    }

    @Provides
    @Named("snsTopic")
    public String getSnsTopic(Config config) {
        return ArnHelper.getArnByResourceName(
                config.getString("topic"),
                getRegion(config),
                getAccountId(config),
                "sns");
    }

    @Provides @Singleton
    public QueueCreator queueCreator(@Named("awsEnabled") boolean awsEnabled,
                                     @Named("region") String region) {
        if (awsEnabled) {
            return new AwsQueueCreator(sqs(region));
        }

        return new NoopQueueCreator();
    }

    @Provides @Singleton
    public QueueSuscriber snsSubcriber(@Named("awsEnabled") boolean awsEnabled,
                                       @Named("region") String region) {
        if (awsEnabled) {
            return new AwsQueueSuscriber(sqs(region), sns(region));
        }

        return new NoopQueueSuscriber();
    }

    @Provides
    public TriggerWatcher triggerWatcher(@Named("awsEnabled") boolean awsEnabled,
                                         @Named("region") String region,
                                         TriggerHelper triggerHelper,
                                         ObjectMapper objectMapper) {
        if (awsEnabled) {
            return new SqsTriggerWatcher(region, triggerHelper, objectMapper);
        }
        return new NoopTriggerWatcher();
    }

    @Provides
    public AutoScaleGroupHelper autoScaleGroupHelper(@Named("awsEnabled") boolean awsEnabled,
                                                     @Named("region") String region) {
        if (awsEnabled) {
            return new AwsAutoScaleGroupHelper(asg(region));
        }
        return new NoopAutoScaleGroupHelper();
    }

    @Provides
    @Named("queueName")
    public String queueName(MacroReplacer macroReplacer,
                            Config config,
                            @Named("instanceId") String instanceId) {
        return macroReplacer.replaceMacros(config.getString("sqs_prefix")) + "-" + instanceId;
    }

    @Provides
    @Named("appName")
    public String appName(Config config) {
        return config.getString("app_name");
    }

    @Provides
    @Named("logPaths")
    public List<String> logDir(Config config) {
        return Splitter.on(',').trimResults().omitEmptyStrings().splitToList(config.getString("log_paths"));
    }

    @Provides
    public ObjectMapper mapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true);
        return objectMapper;
    }

    @Provides
    @Named("environment")
    public String getEnvironment(Config config) {
        return config.getString("environment");
    }

    private AmazonSNS sns(@Named("region") String region) {
        return AmazonSNSClientBuilder.standard().withRegion(region).build();
    }

    private AmazonSQS sqs(@Named("region") String region) {
        return AmazonSQSClientBuilder.standard().withRegion(region).build();
    }

    private AmazonAutoScaling asg(@Named("region") String region) {
        return AmazonAutoScalingClientBuilder.standard().withRegion(region).build();
    }
}
