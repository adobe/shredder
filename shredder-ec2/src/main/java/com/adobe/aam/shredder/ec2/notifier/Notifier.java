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

package com.adobe.aam.shredder.ec2.notifier;

import com.adobe.aam.shredder.ec2.aws.*;
import com.adobe.aam.shredder.ec2.monitoring.MonitoringService;
import com.google.inject.Inject;

public class Notifier {

    private final LifecycleHandler lifecycleHandler;
    private final InstanceDetailsRetriever instanceDetailsRetriever;
    private final MonitoringService monitoringService;

    @Inject
    public Notifier(LifecycleHandler lifecycleHandler,
                    InstanceDetailsRetriever instanceDetailsRetriever,
                    MonitoringService monitoringService) {

        this.lifecycleHandler = lifecycleHandler;
        this.instanceDetailsRetriever = instanceDetailsRetriever;
        this.monitoringService = monitoringService;
    }

    public void notifyAutoScaleGroup(boolean startupSuccessful) {
        instanceDetailsRetriever.getInstanceDetails().ifPresent(details -> complete(details, startupSuccessful));
    }

    public void notifyMonitoringServiceAboutStartup(boolean successful) {
        String metricName = successful ? "ec2StartupSucceeded" : "ec2StartupFailed";
        notifyMonitoringService(metricName);
    }

    public void notifyMonitoringServiceAboutShutdown(boolean successful) {
        String metricName = successful ? "ec2ShutdownSucceeded" : "ec2ShutdownFailed";
        notifyMonitoringService(metricName);
    }

    public void notifyMonitoringService(String metricName) {
        instanceDetailsRetriever.getInstanceDetails().ifPresent(details -> monitoringService.sendMetric(details, metricName));
    }

    private void complete(LifecycleHook message, boolean startupSuccessful) {
        if (startupSuccessful) {
            lifecycleHandler.successfulCompleteLifecycle(message);
        } else {
            lifecycleHandler.abandonCompleteLifecycle(message);
        }
    }
}
