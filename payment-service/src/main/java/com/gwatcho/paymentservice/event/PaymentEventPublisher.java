package com.gwatcho.paymentservice.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gwatcho.paymentservice.dto.DeliveryAddress;
import com.gwatcho.paymentservice.entity.Payment;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class PaymentEventPublisher {
    public static final String PAYMENT_COMPLETED = "payment.completed";

    public static final String PAYMENT_FAILED = "payment.failed";

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public PaymentEventPublisher(KafkaTemplate<String, String> kafkaTemplate, ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    public void publishCompleted(Payment payment, DeliveryAddress deliveryAddress) {
        PaymentCompletedEvent event =
                new PaymentCompletedEvent(payment.getId(), payment.getOrderId(), payment.getCustomerId(), payment.getAmount(),
                        payment.getCurrency(), payment.getTransactionId(), deliveryAddress);

        send(PAYMENT_COMPLETED, payment.getOrderId().toString(), event);
    }

    public void publishFailed(Payment payment, String reason) {
        PaymentFailedEvent event = new PaymentFailedEvent(payment.getId(), payment.getOrderId(), payment.getCustomerId(),
                payment.getAmount(), payment.getCurrency(), reason);

        send(PAYMENT_FAILED, payment.getOrderId().toString(), event);
    }

    private void send(String topic, String key, Object event) {
        try {
            String payload = objectMapper.writeValueAsString(event);

            kafkaTemplate.send(topic, key, payload).get();

        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Could not serialize payment event", e);

        } catch (Exception e) {
            throw new IllegalStateException("Could not publish payment event to Kafka", e);
        }
    }
}