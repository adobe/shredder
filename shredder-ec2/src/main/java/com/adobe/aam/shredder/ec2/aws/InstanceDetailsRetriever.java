package com.adobe.aam.shredder.ec2.aws;

import com.adobe.aam.shredder.core.aws.servergroup.AutoScaleGroupHelper;
import com.amazonaws.services.autoscaling.model.AutoScalingInstanceDetails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.Optional;

public class InstanceDetailsRetriever {
    private static final Logger LOG = LoggerFactory.getLogger(InstanceDetailsRetriever.class);

    private final AutoScaleGroupHelper asgHelper;
    private final String instanceId;
    private final String region;

    @Inject
    public InstanceDetailsRetriever(AutoScaleGroupHelper asgHelper,
                                    @Named("instanceId") String instanceId,
                                    @Named("region") String region) {
        this.asgHelper = asgHelper;
        this.instanceId = instanceId;
        this.region = region;
    }

    public Optional<InstanceDetails> getInstanceDetails() {
        Optional<AutoScalingInstanceDetails> currentAsg = asgHelper.getCurrentAutoScalingGroup(instanceId);
        if (!currentAsg.isPresent()) {
            LOG.warn("Unable to retrieve the Auto Scale Group for the current instance {}.", instanceId);
            return Optional.empty();
        }

        Optional<String> hookName = asgHelper
                .getLifecycleHookName(currentAsg.get().getAutoScalingGroupName(), "autoscaling:EC2_INSTANCE_LAUNCHING");
        if (!hookName.isPresent()) {
            LOG.warn("No EC2_INSTANCE_LAUNCHING hook found for the current instance {}.", instanceId);
            return Optional.empty();
        }

        return Optional.of(new InstanceDetails(instanceId, hookName.get(), currentAsg.get().getAutoScalingGroupName(), region));
    }
}
