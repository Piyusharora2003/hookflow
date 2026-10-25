package com.hookflow.hookflow.common.queue;

import java.util.List;

public interface QueueAdapter {

    /**
     * Publishes a message to the specified queue.
     *
     * @param message   the queue message envelope containing metadata and body
     * @param queueName the destination queue name or URL
     * @param <T>       the body payload type
     * @throws QueuePublishException if the publish operation fails
     */
    <T> void publish(QueueMessage<T> message, String queueName);

    /**
     * Polls messages from the specified queue, deserializing the body into bodyClass,
     * and sets MDC requestId from the message body during extraction.
     *
     * @param queueName   the source queue name or URL
     * @param bodyClass   the class type of the message body
     * @param maxMessages maximum number of messages to poll
     * @param <T>         the body payload type
     * @return a list of polled QueueMessages
     */
    <T> List<QueueMessage<T>> poll(String queueName, Class<T> bodyClass, int maxMessages);

    /**
     * Acknowledges/deletes a message from the queue after successful processing.
     *
     * @param queueName     the queue name or URL
     * @param receiptHandle the receipt handle or delivery tag of the message
     */
    void acknowledge(String queueName, String receiptHandle);
}
