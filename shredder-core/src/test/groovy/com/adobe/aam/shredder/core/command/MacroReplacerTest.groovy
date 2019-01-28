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

package com.adobe.aam.shredder.core.command

import com.adobe.aam.shredder.core.HostnameProvider
import spock.lang.Specification;

class MacroReplacerTest extends Specification {

    def "test macro replacement"(region, hostname, input, expectedOutput) {
        setup:
        def hostnameProvider = Mock(HostnameProvider) {
            getHostname() >> hostname
        }

        def macroReplacer = new MacroReplacer(region, hostnameProvider)

        when:
        def output = macroReplacer.replaceMacros(input)

        then:
        output == expectedOutput

        where:
        region      | hostname | input                             | expectedOutput
        "us-east-1" | "myhost" | "cat REGION_MACRO HOSTNAME_MACRO" | "cat us-east-1 myhost"
        "us-east-1" | null     | "cat REGION_MACRO HOSTNAME_MACRO" | "cat us-east-1 no-hostname"
        null        | null     | "cat REGION_MACRO HOSTNAME_MACRO" | "cat no-region no-hostname"
        ""          | ""       | "cat REGION_MACRO HOSTNAME_MACRO" | "cat  "
        "us-east-1" | "myhost" | "cat HOSTNAME_MACRO"              | "cat myhost"
    }

    def "test macro replacement if hostname changes"() {
        setup:
        def attempt = 0
        def hostnameProvider = Mock(HostnameProvider) {
            getHostname() >> {
                if (attempt++ == 0) {
                    return "first-hostname"
                }
                return "second-hostname"
            }
        }

        def input = "cat HOSTNAME_MACRO"
        def macroReplacer = new MacroReplacer("region", hostnameProvider)

        when: "we replace the macros for the FIRST time"
        def output = macroReplacer.replaceMacros(input)

        then: "the hostname is replaced with the provided value"
        output == "cat first-hostname"

        when: "we replace the macros for the SECOND time"
        output = macroReplacer.replaceMacros(input)

        then: "the hostname is not cached"
        output == "cat second-hostname"
    }
}
