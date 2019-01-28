package com.adobe.aam.shredder.ec2.monitoring;

import com.adobe.aam.shredder.ec2.aws.InstanceDetails;
import com.amazonaws.AmazonClientException;
import com.amazonaws.services.cloudwatch.AmazonCloudWatch;
import com.amazonaws.services.cloudwatch.model.*;
import com.google.common.collect.Lists;
import com.google.inject.assistedinject.Assisted;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.Collection;
import java.util.Date;

public class CloudWatchSender implements MonitoringService {

    private static final Logger LOG = LoggerFactory.getLogger(CloudWatchSender.class);
    private final AmazonCloudWatch cloudWatch;
    private final String appName;
    private final String cloudWatchNamespace;
    private final String environment;

    @Inject
    public CloudWatchSender(@Assisted AmazonCloudWatch cloudWatch,
                            @Named("appName") String appName,
                            @Named("cloudWatchNamespace") String cloudWatchNamespace,
                            @Named("environment") String environment) {
        this.cloudWatch = cloudWatch;
        this.appName = appName;
        this.cloudWatchNamespace = cloudWatchNamespace;
        this.environment = environment;
    }

    public interface CloudWatchFactory {
        CloudWatchSender create(@Assisted AmazonCloudWatch amazonCloudWatch);
    }

    @Override
    public void sendMetric(InstanceDetails message, String metricName) {
        LOG.info("Sending CloudWatch metric {}", metricName);

        Collection<Dimension> dimensions = getMetricDimensions(message);
        MetricDatum datum = getMetricDatum(dimensions, metricName);
        PutMetricDataRequest request = new PutMetricDataRequest()
                .withNamespace(cloudWatchNamespace)
                .withMetricData(datum);
        try {
            cloudWatch.putMetricData(request);
        } catch (AmazonClientException e) {
            LOG.warn("Unable to send CloudWatch metric for {}, {}", message, e.getMessage());
        }
    }

    private Collection<Dimension> getMetricDimensions(InstanceDetails instanceDetails) {
        Dimension dimension1 = new Dimension()
                .withName("asgName")
                .withValue(instanceDetails.getAutoScalingGroupName());
        Dimension dimension2 = new Dimension()
                .withName("region")
                .withValue(instanceDetails.getRegionName());
        Dimension dimension3 = new Dimension()
                .withName("appName")
                .withValue(appName);
        Dimension dimension4 = new Dimension()
                .withName("environment")
                .withValue(environment);

        return Lists.newArrayList(dimension1, dimension2, dimension3, dimension4);
    }

    private MetricDatum getMetricDatum(Collection<Dimension> dimensions, String metricName) {
        return new MetricDatum()
                .withMetricName(metricName)
                .withUnit(StandardUnit.None)
                .withValue(1.0)
                .withDimensions(dimensions)
                .withTimestamp(new Date());
    }
}
