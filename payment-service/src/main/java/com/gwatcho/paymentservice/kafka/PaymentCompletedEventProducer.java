package com.gwatcho.paymentservice.kafka;

import com.gwatcho.paymentservice.event.PaymentCompletedEvent;
import com.gwatcho.paymentservice.exception.PaymentEventPublishingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class PaymentCompletedEventProducer {
    private static final Logger log = LoggerFactory.getLogger(PaymentCompletedEventProducer.class);

    private final KafkaTemplate<String, PaymentCompletedEvent> kafkaTemplate;
    private final String topic;

    public PaymentCompletedEventProducer(KafkaTemplate<String, PaymentCompletedEvent> kafkaTemplate,
                                         @Value("${app.kafka.topics.payment-completed}") String topic) {
        this.kafkaTemplate = kafkaTemplate;
        this.topic = topic;
    }

    public void publish(PaymentCompletedEvent event) {
        if (event == null) {
            throw new IllegalArgumentException("PaymentCompletedEvent must not be null");
        }

        String key = String.valueOf(event.orderId());

        log.info("Publishing payment.completed event: "
                        + "orderId={}, paymentId={}, topic={}",
                event.orderId(), event.paymentId(), topic);

        try {
            kafkaTemplate.send(topic, key, event).whenComplete((result, exception) -> {
                if (exception != null) {
                    log.error("Failed to publish payment.completed event: "
                                    + "orderId={}, paymentId={}, topic={}",
                            event.orderId(), event.paymentId(), topic, exception);

                    return;
                }

                log.info("payment.completed event published successfully: "
                                + "orderId={}, paymentId={}, topic={}, partition={}, offset={}",
                        event.orderId(), event.paymentId(), topic, result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
            });

        } catch (Exception ex) {
            log.error("Unexpected error while publishing payment.completed event: "
                            + "orderId={}, paymentId={}, topic={}",
                    event.orderId(), event.paymentId(), topic, ex);

            throw new PaymentEventPublishingException(
                    "Could not publish payment.completed event for orderId=" + event.orderId(), ex);
        }
    }
}