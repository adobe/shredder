package com.adobe.aam.shredder.core.aws.queue;

public class NoopQueueCreator implements QueueCreator{

    @Override
    public void createQueue(String queueName) {

    }

    @Override
    public void deleteQueue(String queueName) {

    }
}
