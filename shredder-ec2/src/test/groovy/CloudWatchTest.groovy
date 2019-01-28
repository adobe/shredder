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

import com.adobe.aam.shredder.ec2.monitoring.CloudWatchSender
import com.adobe.aam.shredder.ec2.aws.InstanceDetails
import com.adobe.aam.shredder.ec2.trigger.LifecycleHookMessage
import com.amazonaws.services.cloudwatch.AmazonCloudWatch
import com.amazonaws.services.cloudwatch.AmazonCloudWatchClientBuilder
import com.amazonaws.services.cloudwatch.model.Dimension
import com.amazonaws.services.cloudwatch.model.MetricDatum
import com.amazonaws.services.cloudwatch.model.PutMetricDataRequest
import com.amazonaws.services.cloudwatch.model.PutMetricDataResult
import com.amazonaws.services.cloudwatch.model.StandardUnit
import spock.lang.Ignore
import spock.lang.Specification

class CloudWatchTest extends Specification {

    def "test sending custom metric to CloudWatch"() {
        given: "a CloudWatch object with mocked putMetricData method"
        PutMetricDataRequest metricRequest
        def cloudWatch = Mock(AmazonCloudWatch) {
            putMetricData(_) >> { args ->
                metricRequest = args[0]
                return null
            }
        }
        and: "a cloudWatchSender object"
        def cloudWatchSender = new CloudWatchSender(cloudWatch, "appName", "namespace", "env")
        and: "a mock InstanceDetails with mocked getAutoScalingGroupName and getRegionName methods"
        def instanceDetails = Mock(InstanceDetails) {
            getAutoScalingGroupName() >> "asgName"
            getRegionName() >> "regionName"
        }
        when: "we call sendMetric on the cloudWatchSender object"
        cloudWatchSender.sendMetric(instanceDetails, "metricName")
        then: "we expect a PutMetricDataRequest object which has metricData field populated with given dimensions"
        metricRequest.metricData.dimensions[0] as Set == [dimension("asgName", "asgName"),
                                                       dimension("region", "regionName"),
                                                       dimension("appName", "appName"),
                                                       dimension("environment", "env")] as Set
    }

    Dimension dimension(String name, String value) {
        return new Dimension()
                .withName(name)
                .withValue(value);
    }

    @Ignore
    def "test actual sending custom metric to CloudWatch"() {
        setup:
        def message = Mock(LifecycleHookMessage) {
            getAutoScalingGroupName() >> "dcs-test2"
            getRegionName() >> "us-east1"
        }

        Dimension dimension1 = new Dimension()
                .withName("asgName")
                .withValue(message.getAutoScalingGroupName())
        Dimension dimension2 = new Dimension()
                .withName("region")
                .withValue(message.getRegionName())
        Dimension dimension3 = new Dimension()
                .withName("appName")
                .withValue("aapName");
        Dimension dimension4 = new Dimension()
                .withName("environment")
                .withValue("env");

        MetricDatum datum = new MetricDatum()
                .withMetricName("ec2StartupFailedTest")
                .withUnit(StandardUnit.None)
                .withValue(1.0)
                .withDimensions(dimension1, dimension2, dimension3, dimension4)
                .withTimestamp(new Date())
        PutMetricDataRequest request = new PutMetricDataRequest()
                .withNamespace("Edge/Shredder")
                .withMetricData(datum)

        def cloudWatch = AmazonCloudWatchClientBuilder.standard().withRegion("us-east-1").build()

        when:
        PutMetricDataResult response = cloudWatch.putMetricData(request);

        then:
        println response.toString()
    }
}
