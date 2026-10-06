package com.gwatcho.paymentservice.kafka;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.gwatcho.paymentservice.dto.DeliveryAddress;
import com.gwatcho.paymentservice.event.PaymentCompletedEvent;
import java.math.BigDecimal;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;

class PaymentCompletedEventProducerTest {
    private KafkaTemplate<String, PaymentCompletedEvent> kafkaTemplate;
    private PaymentCompletedEventProducer producer;
    private static final String TOPIC = "payment.completed";

    @BeforeEach
    void setUp() {
        kafkaTemplate = mock(KafkaTemplate.class);
        producer = new PaymentCompletedEventProducer(kafkaTemplate, TOPIC);
    }

    // =========================================================
    // SUCCESS
    // =========================================================

    @Test
    void shouldPublishPaymentCompletedEvent() {
        PaymentCompletedEvent event = createPaymentCompletedEvent();

        CompletableFuture future = CompletableFuture.completedFuture(null);

        when(kafkaTemplate.send(eq(TOPIC), eq("100"), eq(event))).thenReturn(future);

        producer.publish(event);

        verify(kafkaTemplate).send(TOPIC, "100", event);
    }

    // =========================================================
    // KAFKA FAILURE
    // =========================================================
    @Test
    void shouldHandleKafkaPublishingFailure() {
        PaymentCompletedEvent event = createPaymentCompletedEvent();

        CompletableFuture future = new CompletableFuture<>();

        RuntimeException kafkaException = new RuntimeException("Kafka unavailable");

        future.completeExceptionally(kafkaException);

        when(kafkaTemplate.send(eq(TOPIC), eq("100"), eq(event))).thenReturn(future);

        // If your producer uses an asynchronous callback,
        // the exception is handled by the callback.
        producer.publish(event);

        verify(kafkaTemplate).send(TOPIC, "100", event);
    }

    // =========================================================
    // NULL EVENT
    // =========================================================

    @Test
    void shouldRejectNullEvent() {
        assertThatThrownBy(() -> producer.publish(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("PaymentCompletedEvent must not be null");

        verifyNoInteractions(kafkaTemplate);
    }

    // =========================================================
    // KAFKA SEND INVOCATION
    // =========================================================

    @Test
    void shouldUseOrderIdAsKafkaKey() {
        PaymentCompletedEvent event = createPaymentCompletedEvent();

        CompletableFuture future = CompletableFuture.completedFuture(null);

        when(kafkaTemplate.send(anyString(), anyString(), any(PaymentCompletedEvent.class)))
                .thenReturn(future);

        producer.publish(event);

        verify(kafkaTemplate).send(eq(TOPIC), eq("100"), eq(event));
    }

    // =========================================================
    // HELPER
    // =========================================================

    private PaymentCompletedEvent createPaymentCompletedEvent() {
        DeliveryAddress deliveryAddress = new DeliveryAddress(
                "Main Street 10",
                "74172",
                "Neckarsulm",
                "DE"
        );

        return new PaymentCompletedEvent(
                10L,
                100L,
                200L,
                new BigDecimal("2693.28"),
                "EUR",
                "TXN-123456",
                deliveryAddress
        );
    }
}