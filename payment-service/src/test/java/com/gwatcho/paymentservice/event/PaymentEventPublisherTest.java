package com.gwatcho.paymentservice.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gwatcho.paymentservice.dto.DeliveryAddress;
import com.gwatcho.paymentservice.entity.Payment;
import com.gwatcho.paymentservice.entity.PaymentStatus;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PaymentEventPublisherTest {

    private KafkaProducer<String, String> producer;

    private PaymentEventPublisher publisher;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {

        producer = mock(KafkaProducer.class);

        objectMapper = new ObjectMapper();

        publisher = new PaymentEventPublisher(
                producer,
                objectMapper
        );
    }

    @Test
    void publishCompleted_sendsCorrectKafkaRecord() {

        Payment payment = new Payment(
                100L,
                10L,
                new BigDecimal("149.99"),
                "EUR",
                "CARD"
        );
        payment.complete("TX-123");
        publisher.publishCompleted(payment, new DeliveryAddress("Main Street 10",
                "74172", "Neckarsulm", "DE"));

        ArgumentCaptor<ProducerRecord<String, String>>
                captor =
                ArgumentCaptor.forClass(
                        ProducerRecord.class
                );

        verify(producer).send(captor.capture());

        ProducerRecord<String, String> record = captor.getValue();

        assertEquals(
                "payment.completed",
                record.topic()
        );

        assertEquals(
                "100",
                record.key()
        );

        assertTrue(
                record.value().contains("\"transactionId\":\"TX-123\"")
        );

        assertTrue(
                record.value().contains("\"orderId\":100")
        );

        assertTrue(
                record.value().contains("\"transactionId\":\"TX-123\"")
        );
    }

    @Test
    void publishFailed_sendsCorrectKafkaRecord() {


        Payment payment = new Payment(
                100L,
                10L,
                new BigDecimal("149.99"),
                "EUR",
                "CARD"
        );

        payment.failed("TX-123");

        publisher.publishFailed(
                payment,
                "Card declined"
        );

        ArgumentCaptor<ProducerRecord<String, String>>
                captor =
                ArgumentCaptor.forClass(
                        ProducerRecord.class
                );

        verify(producer).send(captor.capture());

        ProducerRecord<String, String> record =
                captor.getValue();

        assertEquals(
                "payment.failed",
                record.topic()
        );

        assertEquals(
                "100",
                record.key()
        );

        assertTrue(
                record.value().contains(
                        "\"reason\":\"Card declined\""
                )
        );
    }
}