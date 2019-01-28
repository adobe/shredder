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

package com.adobe.aam.shredder.ec2.di;

import com.adobe.aam.shredder.ec2.monitoring.CloudWatchSender.CloudWatchFactory;
import com.adobe.aam.shredder.ec2.monitoring.MonitoringService;
import com.adobe.aam.shredder.ec2.monitoring.MonitoringServiceNoop;
import com.amazonaws.services.cloudwatch.AmazonCloudWatch;
import com.amazonaws.services.cloudwatch.AmazonCloudWatchClientBuilder;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.assistedinject.FactoryModuleBuilder;

import javax.inject.Named;

public class ShredderMonitorModule extends AbstractModule {

    @Override
    public void configure() {
        install(new FactoryModuleBuilder().build(CloudWatchFactory.class));
    }

    @Provides
    public MonitoringService monitoringService(@Named("sendCloudWatchMetrics") boolean sendMetrics,
                                               @Named("region") String region,
                                               CloudWatchFactory factory) {
        if (sendMetrics) {
            AmazonCloudWatch cloudWatch = AmazonCloudWatchClientBuilder.standard().withRegion(region).build();
            return factory.create(cloudWatch);
        }
        return new MonitoringServiceNoop();
    }
}
