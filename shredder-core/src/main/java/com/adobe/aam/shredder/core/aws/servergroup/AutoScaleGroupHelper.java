package com.adobe.aam.shredder.core.aws.servergroup;

import com.amazonaws.services.autoscaling.model.AutoScalingInstanceDetails;
import com.amazonaws.services.autoscaling.model.CompleteLifecycleActionRequest;
import com.amazonaws.services.autoscaling.model.RecordLifecycleActionHeartbeatRequest;

import java.util.Optional;

public interface AutoScaleGroupHelper {

    int HEARTBEAT_INTERVAL_MS = 2 * 60 * 1000;

    Optional<AutoScalingInstanceDetails> getCurrentAutoScalingGroup(String instanceId);
    Optional<String> getLifecycleHookName(String autoScaleGroupName, String transitionType);

    void recordLifecycleActionHeartbeat(RecordLifecycleActionHeartbeatRequest request);
    void completeLifecycleAction(CompleteLifecycleActionRequest request);
}
