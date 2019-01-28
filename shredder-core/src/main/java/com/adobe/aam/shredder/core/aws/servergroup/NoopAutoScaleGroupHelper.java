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
