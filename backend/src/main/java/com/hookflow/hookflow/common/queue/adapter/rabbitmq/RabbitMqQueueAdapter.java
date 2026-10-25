package com.hookflow.hookflow.common.queue.adapter.rabbitmq;

import com.hookflow.hookflow.common.queue.QueueAdapter;
import com.hookflow.hookflow.common.queue.QueueMessage;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@ConditionalOnProperty(name = "hookflow.queue.type", havingValue = "rabbitmq")
public class RabbitMqQueueAdapter implements QueueAdapter {

    @Override
    public <T> void publish(QueueMessage<T> message, String queueName) {
        throw new UnsupportedOperationException("RabbitMQ queue adapter is not yet implemented");
    }

    @Override
    public <T> List<QueueMessage<T>> poll(String queueName, Class<T> bodyClass, int maxMessages) {
        throw new UnsupportedOperationException("RabbitMQ queue adapter is not yet implemented");
    }

    @Override
    public void acknowledge(String queueName, String receiptHandle) {
        throw new UnsupportedOperationException("RabbitMQ queue adapter is not yet implemented");
    }
}
