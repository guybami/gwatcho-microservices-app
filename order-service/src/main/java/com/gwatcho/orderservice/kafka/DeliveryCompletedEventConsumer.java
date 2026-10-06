package com.gwatcho.orderservice.kafka;

import com.gwatcho.orderservice.event.DeliveryCompletedEvent;
import com.gwatcho.orderservice.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DeliveryCompletedEventConsumer {
    private final OrderService orderService;

    @KafkaListener(topics = "${app.kafka.topics.delivery-completed}", groupId = "${app.kafka.consumer.group-id}")
    public void consumeDeliveryCompleted(ConsumerRecord<String, DeliveryCompletedEvent> record) {
        DeliveryCompletedEvent event = record.value();

        log.info("Received delivery.completed: topic={}, partition={}, offset={}, key={}", record.topic(),
                record.partition(), record.offset(), record.key());

        log.info("DeliveryCompletedEvent received: deliveryId={}, orderId={}, customerId={}", event.deliveryId(),
                event.orderId(), event.customerId());

        try {
            // Complete the order
            orderService.completeOrder(event.orderId());

            log.info("delivery.completed processed successfully: orderId={}, partition={}, offset={}", event.orderId(),
                    record.partition(), record.offset());

        } catch (Exception ex) {
            log.error("Failed to process delivery.completed: topic={}, partition={}, offset={}", record.topic(),
                    record.partition(), record.offset(), ex);

            // Exception is rethrown so Spring Kafka can handle
            // the failed message according to the configured
            // acknowledgment/error-handling strategy.
            throw ex;
        }
    }
}