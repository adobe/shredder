package com.adobe.aam.shredder.ec2.aws;

public class InstanceDetails implements LifecycleHook {

    private final String ec2InstanceId;
    private final String lifecycleHookName;
    private final String autoScalingGroupName;
    private final String regionName;

    public InstanceDetails(
            String ec2InstanceId,
            String lifecycleHookName,
            String autoScalingGroupName,
            String regionName
    ) {
        this.ec2InstanceId = ec2InstanceId;
        this.lifecycleHookName = lifecycleHookName;
        this.autoScalingGroupName = autoScalingGroupName;
        this.regionName = regionName;
    }

    public String getEc2InstanceId() {
        return ec2InstanceId;
    }

    public String getLifecycleHookName() {
        return lifecycleHookName;
    }

    public String getAutoScalingGroupName() {
        return autoScalingGroupName;
    }

    public String getRegionName() { return regionName; }

    @Override
    public String toString() {
        return "InstanceDetails{" +
                "ec2InstanceId='" + ec2InstanceId + '\'' +
                ", lifecycleHookName='" + lifecycleHookName + '\'' +
                ", autoScalingGroupName='" + autoScalingGroupName + '\'' +
                ", regionName='" + regionName + '\'' +
                '}';
    }
}
