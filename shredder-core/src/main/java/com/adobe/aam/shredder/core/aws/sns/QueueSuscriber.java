package com.adobe.aam.shredder.core.aws.sns;

public interface QueueSuscriber {

    /**
     * @return the SNS subscription ARN
     */
    String subscribeSnsToQueue(String snsTopic, String queueName);

    void unsubscribe(String snsSubscriptionArn);
}
