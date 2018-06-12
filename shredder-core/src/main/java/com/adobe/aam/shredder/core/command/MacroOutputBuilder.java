
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

package com.adobe.aam.shredder.core.command;

import com.adobe.aam.shredder.core.trigger.TriggerMessage;
import com.amazonaws.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Stream;

class MacroOutputBuilder {

    private static final Logger LOG = LoggerFactory.getLogger(MacroOutputBuilder.class);
    private String output;

    MacroOutputBuilder(String output) {
        this.output = output;
    }

    /**
     * eg. Replaces TRIGGER_HOSTNAME_MACRO with triggerMessage.getHostname()
     */
    <T extends TriggerMessage> MacroOutputBuilder withTriggerInformation(T triggerMessage) {

        getPropertyDescriptors(triggerMessage)
                .filter(pd -> Objects.nonNull(pd.getReadMethod()))
                .forEach(pd -> {
                    String fieldName = pd.getName();
                    try {
                        Object value = pd.getReadMethod().invoke(triggerMessage);
                        withField(fieldName, value);
                    } catch (Exception e) {
                        LOG.error("Unable to get value for field {}. {}", fieldName, e);
                    }
                });

        return this;
    }

    private <T extends TriggerMessage> Stream<PropertyDescriptor> getPropertyDescriptors(T triggerMessage) {
        try {
            return Arrays.stream(
                    Introspector.getBeanInfo(triggerMessage.getClass(), Object.class)
                            .getPropertyDescriptors()
            );
        } catch (IntrospectionException e) {
            LOG.error("Unable to fetch getters for trigger {} {}.", triggerMessage, e);
            return Stream.empty();
        }
    }

    private void withField(String fieldName, Object value) {
        if (!StringUtils.isNullOrEmpty(fieldName)) {
            output = output.replace("TRIGGER_" + fieldName.toUpperCase() + "_MACRO", value.toString());
        }
    }

    MacroOutputBuilder withHost() {
        System.setProperty("java.net.preferIPv4Stack", "true");
        try {
            output = output.replace("HOSTNAME_MACRO", InetAddress.getLocalHost().getHostName());
        } catch (UnknownHostException e) {
            LOG.error("Unable to get hostname", e);
        }

        return this;
    }

    MacroOutputBuilder withRegion(String region) {
        output = output.replace("REGION_MACRO", region);

        return this;
    }

    String build() {
        return output;
    }
}
