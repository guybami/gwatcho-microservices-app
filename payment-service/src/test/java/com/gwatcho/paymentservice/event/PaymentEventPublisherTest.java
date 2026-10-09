package com.gwatcho.paymentservice.event;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gwatcho.paymentservice.dto.DeliveryAddress;
import com.gwatcho.paymentservice.entity.Payment;
import java.math.BigDecimal;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

class PaymentEventPublisherTest {
    private KafkaTemplate<String, String> kafkaTemplate;
    private PaymentEventPublisher publisher;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        kafkaTemplate = mock(KafkaTemplate.class);

        objectMapper = new ObjectMapper();

        // PaymentEventPublisher calls kafkaTemplate.send(...).get(),
        // therefore the mock must return a completed future.
        CompletableFuture<SendResult<String, String>> future = CompletableFuture.completedFuture(null);

        when(kafkaTemplate.send(anyString(), anyString(), anyString())).thenReturn(future);

        publisher = new PaymentEventPublisher(kafkaTemplate, objectMapper);
    }

    @Test
    void publishCompleted_sendsCorrectKafkaRecord() {
        // Arrange
        Payment payment = new Payment(100L, 10L, new BigDecimal("149.99"), "EUR", "CARD");

        payment.complete("TX-123");

        DeliveryAddress deliveryAddress = new DeliveryAddress("Main Street 10", "74172", "Neckarsulm", "DE");

        // Act
        publisher.publishCompleted(payment, deliveryAddress);

        // Assert
        ArgumentCaptor<String> topicCaptor = ArgumentCaptor.forClass(String.class);

        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);

        ArgumentCaptor<String> valueCaptor = ArgumentCaptor.forClass(String.class);

        verify(kafkaTemplate, times(1)).send(topicCaptor.capture(), keyCaptor.capture(), valueCaptor.capture());

        assertEquals("payment.completed", topicCaptor.getValue());
        assertEquals("100", keyCaptor.getValue());

        String value = valueCaptor.getValue();

        assertNotNull(value);

        assertTrue(value.contains("\"transactionId\":\"TX-123\""), "Kafka event should contain transactionId");

        assertTrue(value.contains("\"orderId\":100"), "Kafka event should contain orderId");

        verifyNoMoreInteractions(kafkaTemplate);
    }

    @Test
    void publishFailed_sendsCorrectKafkaRecord() {
        // Arrange
        Payment payment = new Payment(100L, 10L, new BigDecimal("149.99"), "EUR", "CARD");

        payment.failed("TX-123");

        // Act
        publisher.publishFailed(payment, "Card declined");

        // Assert
        ArgumentCaptor<String> topicCaptor = ArgumentCaptor.forClass(String.class);

        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);

        ArgumentCaptor<String> valueCaptor = ArgumentCaptor.forClass(String.class);

        verify(kafkaTemplate, times(1)).send(topicCaptor.capture(), keyCaptor.capture(), valueCaptor.capture());

        assertEquals("payment.failed", topicCaptor.getValue());
        assertEquals("100", keyCaptor.getValue());

        String value = valueCaptor.getValue();

        assertNotNull(value);

        assertTrue(value.contains("\"reason\":\"Card declined\""), "Kafka event should contain failure reason");

        verifyNoMoreInteractions(kafkaTemplate);
    }
}
