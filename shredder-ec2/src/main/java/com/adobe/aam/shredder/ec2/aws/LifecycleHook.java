package com.adobe.aam.shredder.ec2.aws;

public interface LifecycleHook {

    String getEc2InstanceId();

    String getLifecycleHookName();

    String getAutoScalingGroupName();
}
