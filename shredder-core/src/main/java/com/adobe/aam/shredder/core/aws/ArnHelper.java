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
