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

package com.adobe.aam.shredder.core.aws;

public class ArnHelper {
    private static String arnMustHavePrefix = "arn:aws:%s:";

    public static String getArnByResourceName(String resourceName, String region, String accountId, String resourceType) {
        if (isArn(resourceName, resourceType))
                return resourceName;
        return String.format(arnMustHavePrefix, resourceType) + region + ":" + accountId + ":" + resourceName;
    }

    public static boolean isArn(String resourceName, String resourceType) {
        String resourceSpecificArn = String.format(arnMustHavePrefix, resourceType);
        if (resourceName.startsWith(resourceSpecificArn))
            return true;
        return false;
    }
}
