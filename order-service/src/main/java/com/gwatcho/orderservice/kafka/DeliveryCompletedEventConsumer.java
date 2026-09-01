package com.gwatcho.orderservice.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gwatcho.orderservice.event.DeliveryCompletedEvent;
import com.gwatcho.orderservice.service.OrderService;
import java.time.Duration;
import java.util.Collections;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class DeliveryCompletedEventConsumer implements Runnable {
    private static final Logger log = LoggerFactory.getLogger(DeliveryCompletedEventConsumer.class);

    private final KafkaConsumer<String, String> kafkaConsumer;
    private final ObjectMapper objectMapper;
    private final OrderService orderService;
    private final String topic;

    private volatile boolean running = true;

    public DeliveryCompletedEventConsumer(KafkaConsumer<String, String> kafkaConsumer, ObjectMapper objectMapper,
                                          OrderService orderService, @Value("${app.kafka.topics.delivery-completed}") String topic) {
        this.kafkaConsumer = kafkaConsumer;
        this.objectMapper = objectMapper;
        this.orderService = orderService;
        this.topic = topic;
    }

    @Override
    public void run() {
        kafkaConsumer.subscribe(Collections.singletonList(topic));

        log.info("DeliveryCompletedEventConsumer started. topic={}", topic);

        try {
            while (running) {
                var records = kafkaConsumer.poll(Duration.ofMillis(500));

                for (ConsumerRecord<String, String> record : records) {
                    processRecord(record);
                }
            }

        } finally {
            kafkaConsumer.close();

            log.info("DeliveryCompletedEventConsumer stopped");
        }
    }

    void processRecord(ConsumerRecord<String, String> record) {
        try {
            log.info("Received delivery.completed: topic={}, partition={}, offset={}, key={}", record.topic(),
                    record.partition(), record.offset(), record.key());

            DeliveryCompletedEvent event = objectMapper.readValue(record.value(), DeliveryCompletedEvent.class);

            log.info("DeliveryCompletedEvent received: deliveryId={}, orderId={}, customerId={}", event.deliveryId(),
                    event.orderId(), event.customerId());

            orderService.completeOrder(event.orderId());

            kafkaConsumer.commitSync(Collections.singletonMap(new TopicPartition(record.topic(), record.partition()),
                    new org.apache.kafka.clients.consumer.OffsetAndMetadata(record.offset() + 1)));

            log.info("delivery.completed processed successfully: orderId={}, partition={}, offset={}", event.orderId(),
                    record.partition(), record.offset());

        } catch (Exception e) {
            log.error("Failed to process delivery.completed: offset={}", record.offset(), e);
        }
    }

    public void stop() {
        running = false;
    }
}