package com.gwatcho.paymentservice.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gwatcho.paymentservice.event.OrderCreatedEvent;
import com.gwatcho.paymentservice.service.PaymentService;
import jakarta.annotation.PostConstruct;
import java.time.Duration;
import java.util.Collections;
import java.util.Map;
import java.util.Properties;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class OrderCreatedEventConsumer {
    private static final Logger log = LoggerFactory.getLogger(OrderCreatedEventConsumer.class);

    private final PaymentService paymentService;

    private final ObjectMapper objectMapper;

    private final String bootstrapServers;

    private final String groupId;

    private final String topic;

    public OrderCreatedEventConsumer(PaymentService paymentService, ObjectMapper objectMapper,
                                     @Value("${spring.kafka.bootstrap-servers}") String bootstrapServers,
                                     @Value("${app.kafka.consumer.group-id}") String groupId,
                                     @Value("${app.kafka.topics.order-created}") String topic) {
        this.paymentService = paymentService;
        this.objectMapper = objectMapper;
        this.bootstrapServers = bootstrapServers;
        this.groupId = groupId;
        this.topic = topic;
    }

    @PostConstruct
    public void start() {
        Thread thread = new Thread(this::consume, "order-created-consumer");

        thread.setDaemon(true);

        thread.start();

        log.info("PaymentService order.created consumer started: topic={}, groupId={}", topic, groupId);
    }

    private void consume() {
        Properties properties = new Properties();

        properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);

        properties.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);

        properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);

        properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);

        /*
         * Important for a new consumer group.
         */
        properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        /*
         * We commit manually only after successful
         * payment processing.
         */
        properties.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");

        try (KafkaConsumer<String, String> kafkaConsumer = new KafkaConsumer<>(properties)) {
            kafkaConsumer.subscribe(Collections.singletonList(topic));

            log.info("Subscribed to order.created: topic={}, groupId={}", topic, groupId);

            while (!Thread.currentThread().isInterrupted()) {
                ConsumerRecords<String, String> records = kafkaConsumer.poll(Duration.ofMillis(500));

                log.debug("order.created poll returned {} records", records.count());

                for (ConsumerRecord<String, String> record : records) {
                    processRecord(kafkaConsumer, record);
                }
            }

        } catch (Exception ex) {
            log.error("PaymentService order.created consumer stopped", ex);
        }
    }

    private void processRecord(KafkaConsumer<String, String> kafkaConsumer, ConsumerRecord<String, String> record) {
        try {
            log.info("Received order.created: topic={}, partition={}, offset={}, key={}", record.topic(), record.partition(),
                    record.offset(), record.key());

            OrderCreatedEvent event = objectMapper.readValue(record.value(), OrderCreatedEvent.class);

            log.info("Deserialized order.created: eventId={}, orderId={}, customerId={}, amount={}", event.eventId(),
                    event.orderId(), event.customerId(), event.totalAmount());

            paymentService.processOrderCreated(event);

            /*
             * Commit ONLY after successful processing.
             */
            kafkaConsumer.commitSync(Map.of(new TopicPartition(record.topic(), record.partition()),
                    new org.apache.kafka.clients.consumer.OffsetAndMetadata(record.offset() + 1)));

            log.info("Committed order.created offset: topic={}, partition={}, offset={}", record.topic(), record.partition(),
                    record.offset() + 1);

        } catch (Exception ex) {
            log.error("Failed to process order.created: topic={}, partition={}, offset={}", record.topic(),
                    record.partition(), record.offset(), ex);

            /*
             * Do NOT commit.
             *
             * Kafka will redeliver the record.
             */
        }
    }
}