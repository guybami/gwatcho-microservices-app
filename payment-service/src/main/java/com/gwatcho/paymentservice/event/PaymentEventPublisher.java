package com.gwatcho.paymentservice.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gwatcho.paymentservice.dto.DeliveryAddress;
import com.gwatcho.paymentservice.entity.Payment;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class PaymentEventPublisher {

    public static final String PAYMENT_COMPLETED =
            "payment.completed";

    public static final String PAYMENT_FAILED =
            "payment.failed";

    private final KafkaProducer<String, String> producer;
    private final ObjectMapper objectMapper;

    public PaymentEventPublisher(
            KafkaProducer<String, String> producer,
            ObjectMapper objectMapper) {

        this.producer = producer;
        this.objectMapper = objectMapper;
    }

    public void publishCompleted(Payment payment, DeliveryAddress deliveryAddress) {

        PaymentCompletedEvent event =
                new PaymentCompletedEvent(
                        payment.getId(),
                        payment.getOrderId(),
                        payment.getCustomerId(),
                        payment.getAmount(),
                        payment.getCurrency(),
                        payment.getTransactionId(),
                        deliveryAddress
                );

        send(
                PAYMENT_COMPLETED,
                payment.getOrderId().toString(),
                event
        );
    }

    public void publishFailed(
            Payment payment,
            String reason) {

        PaymentFailedEvent event =
                new PaymentFailedEvent(
                        payment.getId(),
                        payment.getOrderId(),
                        payment.getCustomerId(),
                        payment.getAmount(),
                        payment.getCurrency(),
                        reason
                );

        send(
                PAYMENT_FAILED,
                payment.getOrderId().toString(),
                event
        );
    }

    private void send(
            String topic,
            String key,
            Object event) {

        try {
            String payload =
                    objectMapper.writeValueAsString(event);

            ProducerRecord<String, String> record =
                    new ProducerRecord<>(
                            topic,
                            key,
                            payload
                    );

            producer.send(record);

        } catch (JsonProcessingException e) {
            throw new IllegalStateException(
                    "Could not serialize payment event",
                    e
            );
        }
    }
}