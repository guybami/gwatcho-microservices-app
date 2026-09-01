package com.gwatcho.paymentservice.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gwatcho.paymentservice.event.PaymentCompletedEvent;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class PaymentCompletedEventProducer {

    private static final Logger log =
            LoggerFactory.getLogger(
                    PaymentCompletedEventProducer.class
            );

    private final KafkaProducer<String, String>
            kafkaProducer;

    private final ObjectMapper objectMapper;

    private final String topic;


    public PaymentCompletedEventProducer(
            KafkaProducer<String, String> kafkaProducer,
            ObjectMapper objectMapper,
            @Value("${app.kafka.topics.payment-completed}")
            String topic
    ) {
        this.kafkaProducer =
                kafkaProducer;

        this.objectMapper =
                objectMapper;

        this.topic =
                topic;
    }


    public void publish(
            PaymentCompletedEvent event
    ) {

        try {

            String payload =
                    objectMapper.writeValueAsString(
                            event
                    );


            ProducerRecord<String, String> record =
                    new ProducerRecord<>(
                            topic,
                            String.valueOf(
                                    event.orderId()
                            ),
                            payload
                    );


            log.info(
                    "Publishing payment.completed: orderId={}, paymentId={}, topic={}",
                    event.orderId(),
                    event.paymentId(),
                    topic
            );


            kafkaProducer
                    .send(record)
                    .get();


            log.info(
                    "payment.completed published successfully: orderId={}",
                    event.orderId()
            );

        } catch (Exception ex) {

            log.error(
                    "Failed to publish payment.completed: orderId={}",
                    event.orderId(),
                    ex
            );

            throw new IllegalStateException(
                    "Could not publish payment.completed",
                    ex
            );
        }
    }
}