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

package com.adobe.aam.shredder.core.command;

import com.adobe.aam.shredder.core.HostnameProvider;
import com.adobe.aam.shredder.core.trigger.TriggerMessage;

import javax.inject.Inject;
import javax.inject.Named;

public class MacroReplacer {

    private final String region;
    private final HostnameProvider hostnameProvider;

    @Inject
    public MacroReplacer(@Named("region") String region,
                         HostnameProvider hostnameProvider) {
        this.region = region;
        this.hostnameProvider = hostnameProvider;
    }

    /**
     * Replaces macros with information from local server.
     * Example:
     * - replace REGION_MACRO with the actual EC2 region where this server is running.
     * - replace HOSTNAME_MACRO with this server's hostname
     */
    public String replaceMacros(String command) {
        return new MacroOutputBuilder(command)
                .withHost(hostnameProvider.getHostname())
                .withRegion(region)
                .build();
    }

    /**
     * Replaces *_MACRO with local information
     * Replaces TRIGGER_{FIELD}_MACRO with data from triggerMessage
     */
    public <T extends TriggerMessage> String replaceMacros(String command, T triggerMessage) {
        return new MacroOutputBuilder(command)
                .withTriggerInformation(triggerMessage)
                .withHost(hostnameProvider.getHostname())
                .withRegion(region)
                .build();
    }
}
