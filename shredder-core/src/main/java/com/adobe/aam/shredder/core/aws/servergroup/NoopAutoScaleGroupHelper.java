package com.adobe.aam.shredder.core.aws.servergroup;

import com.amazonaws.services.autoscaling.model.AutoScalingInstanceDetails;
import com.amazonaws.services.autoscaling.model.CompleteLifecycleActionRequest;
import com.amazonaws.services.autoscaling.model.RecordLifecycleActionHeartbeatRequest;

import java.util.Optional;

public class NoopAutoScaleGroupHelper implements AutoScaleGroupHelper {

    @Override
    public Optional<AutoScalingInstanceDetails> getCurrentAutoScalingGroup(String instanceId) {
        return Optional.empty();
    }

    @Override
    public Optional<String> getLifecycleHookName(String autoScaleGroupName, String transitionType) {
        return Optional.empty();
    }

    @Override
    public void recordLifecycleActionHeartbeat(RecordLifecycleActionHeartbeatRequest request) {

    }

    @Override
    public void completeLifecycleAction(CompleteLifecycleActionRequest request) {

    }
}
