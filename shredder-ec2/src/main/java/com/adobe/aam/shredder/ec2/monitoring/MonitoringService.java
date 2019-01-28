package com.adobe.aam.shredder.ec2.monitoring;

import com.adobe.aam.shredder.ec2.aws.InstanceDetails;

public interface MonitoringService {

    void sendMetric(InstanceDetails message, String metricName);
}
