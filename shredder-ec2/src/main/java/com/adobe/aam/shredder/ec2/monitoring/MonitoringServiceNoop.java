package com.adobe.aam.shredder.ec2.monitoring;

import com.adobe.aam.shredder.ec2.aws.InstanceDetails;

public class MonitoringServiceNoop implements MonitoringService {

    @Override
    public void sendMetric(InstanceDetails message, String metricName) {

    }
}
