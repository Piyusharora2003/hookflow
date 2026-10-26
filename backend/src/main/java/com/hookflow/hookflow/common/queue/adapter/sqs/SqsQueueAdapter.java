package com.hookflow.hookflow.common.queue.adapter.sqs;

import tools.jackson.databind.JavaType;
import tools.jackson.databind.ObjectMapper;
import com.hookflow.hookflow.common.queue.QueueAdapter;
import com.hookflow.hookflow.common.queue.QueueMessage;
import com.hookflow.hookflow.common.queue.QueuePublishException;
import com.hookflow.hookflow.filter.RequestIdFilter;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlRequest;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageResponse;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@ConditionalOnProperty(name = "hookflow.queue.type", havingValue = "sqs", matchIfMissing = true)
public class SqsQueueAdapter implements QueueAdapter {

    private final SqsClient sqsClient;
    private final ObjectMapper objectMapper;
    private final Map<String, String> queueUrlCache = new ConcurrentHashMap<>();

    public SqsQueueAdapter(SqsClient sqsClient, ObjectMapper objectMapper) {
        this.sqsClient = sqsClient;
        this.objectMapper = objectMapper;
    }

    @Override
    public <T> void publish(QueueMessage<T> message, String queueName) {
        try {
            String queueUrl = resolveQueueUrl(queueName);
            String payloadJson = objectMapper.writeValueAsString(message);

            SendMessageRequest request = SendMessageRequest.builder()
                    .queueUrl(queueUrl)
                    .messageBody(payloadJson)
                    .build();

            sqsClient.sendMessage(request);
            log.debug("Successfully published message [messageId={}, queueName={}]",
                    message.getMessageId(), queueName);
        } catch (Exception e) {
            log.error("Failed to publish message to SQS [queueName={}, messageId={}]: {}",
                    queueName, message.getMessageId(), e.getMessage());
            throw new QueuePublishException("Failed to publish message to SQS queue: " + queueName, e);
        }
    }

    @Override
    public <T> List<QueueMessage<T>> poll(String queueName, Class<T> bodyClass, int maxMessages) {
        List<QueueMessage<T>> result = new ArrayList<>();
        try {
            String queueUrl = resolveQueueUrl(queueName);
            int safeMax = Math.clamp(maxMessages, 1, 10);

            ReceiveMessageRequest request = ReceiveMessageRequest.builder()
                    .queueUrl(queueUrl)
                    .maxNumberOfMessages(safeMax)
                    .build();

            ReceiveMessageResponse response = sqsClient.receiveMessage(request);
            JavaType messageType = objectMapper.getTypeFactory()
                    .constructParametricType(QueueMessage.class, bodyClass);

            for (Message sqsMessage : response.messages()) {
                try {
                    QueueMessage<T> queueMessage = objectMapper.readValue(sqsMessage.body(), messageType);
                    queueMessage.setReceiptHandle(sqsMessage.receiptHandle());

                    if (queueMessage.getRequestId() != null) {
                        MDC.put(RequestIdFilter.MDC_REQUEST_ID_KEY, queueMessage.getRequestId());
                    }

                    result.add(queueMessage);
                } catch (Exception e) {
                    log.error("Failed to deserialize SQS message body [sqsMessageId={}]: {}",
                            sqsMessage.messageId(), e.getMessage(), e);
                }
            }
        } catch (Exception e) {
            log.error("Failed to poll messages from SQS queue [queueName={}]: {}",
                    queueName, e.getMessage(), e);
        }
        return result;
    }

    @Override
    public void acknowledge(String queueName, String receiptHandle) {
        try {
            String queueUrl = resolveQueueUrl(queueName);
            DeleteMessageRequest request = DeleteMessageRequest.builder()
                    .queueUrl(queueUrl)
                    .receiptHandle(receiptHandle)
                    .build();

            sqsClient.deleteMessage(request);
            log.debug("Acknowledged message in SQS queue [queueName={}]", queueName);
        } catch (Exception e) {
            log.error("Failed to acknowledge message in SQS [queueName={}, receiptHandle={}]: {}",
                    queueName, receiptHandle, e.getMessage(), e);
        }
    }

    private String resolveQueueUrl(String queueNameOrUrl) {
        if (queueNameOrUrl == null || queueNameOrUrl.isBlank()) {
            throw new IllegalArgumentException("Queue name or URL must not be empty");
        }
        if (queueNameOrUrl.startsWith("http://") || queueNameOrUrl.startsWith("https://")) {
            return queueNameOrUrl;
        }
        return queueUrlCache.computeIfAbsent(queueNameOrUrl, name -> {
            GetQueueUrlRequest request = GetQueueUrlRequest.builder().queueName(name).build();
            return sqsClient.getQueueUrl(request).queueUrl();
        });
    }
}
