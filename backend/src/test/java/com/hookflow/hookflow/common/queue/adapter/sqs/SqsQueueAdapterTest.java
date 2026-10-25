package com.hookflow.hookflow.common.queue.adapter.sqs;

import tools.jackson.databind.ObjectMapper;
import com.hookflow.hookflow.common.queue.QueueMessage;
import com.hookflow.hookflow.common.queue.QueuePublishException;
import com.hookflow.hookflow.filter.RequestIdFilter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.DeleteMessageResponse;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageResponse;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;
import software.amazon.awssdk.services.sqs.model.SqsException;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SqsQueueAdapterTest {

    @Mock
    private SqsClient sqsClient;

    private ObjectMapper objectMapper;
    private SqsQueueAdapter sqsQueueAdapter;

    private final String queueUrl = "https://sqs.us-east-1.amazonaws.com/123456789012/test-queue";

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        sqsQueueAdapter = new SqsQueueAdapter(sqsClient, objectMapper);
    }

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    @Test
    void testPublishSuccess() {
        when(sqsClient.sendMessage(any(SendMessageRequest.class)))
                .thenReturn(SendMessageResponse.builder().messageId("msg-123").build());

        QueueMessage<Map<String, String>> message = QueueMessage.<Map<String, String>>builder()
                .requestId("req_test123")
                .messageId("msg_uuid_1")
                .eventType("order.created")
                .timestamp(Instant.now())
                .body(Map.of("orderId", "ord_999"))
                .build();

        sqsQueueAdapter.publish(message, queueUrl);

        ArgumentCaptor<SendMessageRequest> captor = ArgumentCaptor.forClass(SendMessageRequest.class);
        verify(sqsClient).sendMessage(captor.capture());

        SendMessageRequest captured = captor.getValue();
        assertEquals(queueUrl, captured.queueUrl());
        assertNotNull(captured.messageBody());
    }

    @Test
    void testPublishThrowsQueuePublishExceptionOnAwsError() {
        when(sqsClient.sendMessage(any(SendMessageRequest.class)))
                .thenThrow(SqsException.builder().message("AWS Service Down").build());

        QueueMessage<String> message = QueueMessage.<String>builder()
                .requestId("req_fail")
                .messageId("msg_fail")
                .body("data")
                .build();

        assertThrows(QueuePublishException.class, () -> sqsQueueAdapter.publish(message, queueUrl));
    }

    @Test
    void testPollSuccessAndPopulatesMdc() {
        String jsonBody = "{\"requestId\":\"req_poll_999\",\"messageId\":\"msg_1\",\"eventType\":\"user.signup\",\"body\":{\"name\":\"John\"}}";
        Message sqsMessage = Message.builder()
                .messageId("sqs-msg-1")
                .receiptHandle("handle-xyz")
                .body(jsonBody)
                .build();

        when(sqsClient.receiveMessage(any(ReceiveMessageRequest.class)))
                .thenReturn(ReceiveMessageResponse.builder().messages(List.of(sqsMessage)).build());

        @SuppressWarnings("rawtypes")
        List<QueueMessage<Map>> messages = sqsQueueAdapter.poll(queueUrl, Map.class, 5);

        assertFalse(messages.isEmpty());
        QueueMessage<Map> polled = messages.getFirst();
        assertEquals("req_poll_999", polled.getRequestId());
        assertEquals("handle-xyz", polled.getReceiptHandle());
        assertEquals("user.signup", polled.getEventType());
        assertEquals("req_poll_999", MDC.get(RequestIdFilter.MDC_REQUEST_ID_KEY));
    }

    @Test
    void testAcknowledge() {
        when(sqsClient.deleteMessage(any(DeleteMessageRequest.class)))
                .thenReturn(DeleteMessageResponse.builder().build());

        sqsQueueAdapter.acknowledge(queueUrl, "receipt-handle-123");

        ArgumentCaptor<DeleteMessageRequest> captor = ArgumentCaptor.forClass(DeleteMessageRequest.class);
        verify(sqsClient).deleteMessage(captor.capture());
        assertEquals(queueUrl, captor.getValue().queueUrl());
        assertEquals("receipt-handle-123", captor.getValue().receiptHandle());
    }
}
