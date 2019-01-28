package com.adobe.aam.shredder.core.aws.queue;

public interface QueueCreator {

    void createQueue(String queueName);
    void deleteQueue(String queueName) ;
}
