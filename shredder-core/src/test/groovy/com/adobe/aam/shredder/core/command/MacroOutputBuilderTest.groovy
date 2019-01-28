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

package com.adobe.aam.shredder.core.command

import com.adobe.aam.shredder.core.trigger.TriggerMessage
import spock.lang.Specification

class MacroOutputBuilderTest extends Specification {

    def "test host and region replacing"(command, region, expectedOutput) {
        setup:
        def builder = new MacroOutputBuilder(command)

        when:
        def output = builder
                .withHost("myhost")
                .withRegion(region)
                .build()

        then:
        output == expectedOutput

        where:
        command                        | region      | expectedOutput
        "ls REGION_MACRO"              | "myregion"  | "ls myregion"
        "echo HOSTNAME_MACRO"          | ""          | "echo myhost"
        "ls"                           | "us-east-1" | "ls"
        "BROKEN_MACRO cp REGION_MACRO" | "us-east-1" | "BROKEN_MACRO cp us-east-1"
        ""                             | "us-east-1" | ""
    }

    def "test macro replacement with trigger information"(command, trigger, expectedOutput) {
        setup:
        def builder = new MacroOutputBuilder(command)

        when:
        def output = builder.withTriggerInformation(trigger).build()

        then:
        output == expectedOutput

        where:
        command                                          | trigger                        | expectedOutput
        "ls TRIGGER_HOSTNAME_MACRO"                      | genTrigger("value1", "value2") | "ls value1"
        "ls TRIGGER_HOSTNAME_MACRO TRIGGER_FIELD2_MACRO" | genTrigger("value1", "value2") | "ls value1 value2"
        "ls TRIGGER_INVALIDFIELD_MACRO"                  | genTrigger("value1", "value2") | "ls TRIGGER_INVALIDFIELD_MACRO"
    }

    def "test macro replacement with trigger information and host region"(command, region, trigger, expectedOutput) {
        setup:
        def builder = new MacroOutputBuilder(command)

        when:
        def output = builder
                .withTriggerInformation(trigger)
                .withHost("somehost")
                .withRegion(region)
                .build()

        then:
        output == expectedOutput

        where:
        command                                                 | region      | trigger                        | expectedOutput
        "ls TRIGGER_HOSTNAME_MACRO REGION_MACRO HOSTNAME_MACRO" | "us-east-1" | genTrigger("value1", "value2") | "ls value1 us-east-1 somehost"
        "ls REGION_MACRO TRIGGER_FIELD2_MACRO"                  | "us-east-1" | genTrigger("value1", "value2") | "ls us-east-1 value2"
    }

    private TriggerMessage genTrigger(String field1, String field2) {
        return new TestTrigger(field1, field2)
    }

    class TestTrigger implements TriggerMessage {
        private final String hostname;
        private final String field2;

        public TestTrigger(String hostname, String field2) {

            this.hostname = hostname
            this.field2 = field2
        }

        public String getHostname() {
            return hostname;
        }

        public String getField2() {
            return field2;
        }
    }
}
