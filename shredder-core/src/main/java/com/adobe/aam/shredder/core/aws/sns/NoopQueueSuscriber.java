package com.adobe.aam.shredder.core.aws.sns;

public class NoopQueueSuscriber implements QueueSuscriber {

    @Override
    public String subscribeSnsToQueue(String snsTopic, String queueName) {
        return null;
    }

    @Override
    public void unsubscribe(String snsSubscriptionArn) {

    }
}
