package com.gwatcho.paymentservice.kafka;

import com.gwatcho.paymentservice.event.OrderCreatedEvent;
import com.gwatcho.paymentservice.service.PaymentService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;



@Component
public class OrderCreatedEventConsumer {
    private static final Logger log = LoggerFactory.getLogger(OrderCreatedEventConsumer.class);

    private final PaymentService paymentService;

    public OrderCreatedEventConsumer(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @KafkaListener(topics = "${app.kafka.topics.order-created}", groupId = "${app.kafka.consumer.group-id}")
    public void consumeOrderCreated(ConsumerRecord<String, OrderCreatedEvent> record) {
        log.info("Received order.created:key={}, topic={}, partition={}, offset={}", record.key(),
                record.topic(),
                record.partition(), record.offset());
        OrderCreatedEvent event = record.value();
        log.info("Processing order.created: orderId={}, customerId={}, amount={}, currency={}, paymentMethod={}",
                event.orderId(), event.customerId(), event.totalAmount(), event.currency(), event.paymentMethod());

        paymentService.processOrderCreated(event);
    }
}
