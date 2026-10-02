package com.gwatcho.orderservice.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gwatcho.orderservice.event.DeliveryCompletedEvent;
import com.gwatcho.orderservice.service.OrderService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

import java.time.Duration;
import java.util.Collections;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.errors.WakeupException;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DeliveryCompletedEventConsumer {
    private final ObjectMapper objectMapper;
    private final OrderService orderService;

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${app.kafka.consumer.group-id}")
    private String groupId;

    @Value("${app.kafka.topics.delivery-completed}")
    private String deliveryCompletedTopic;

    private KafkaConsumer<String, String> kafkaConsumer;

    private ExecutorService executorService;

    private volatile boolean running = false;

    // =========================================================
    // START
    // =========================================================

    @PostConstruct
    public void start() {
        Properties properties = new Properties();

        properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);

        properties.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);

        properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());

        properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());

        properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        properties.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");

        kafkaConsumer = new KafkaConsumer<>(properties);

        kafkaConsumer.subscribe(Collections.singletonList(deliveryCompletedTopic));

        running = true;

        executorService = Executors.newSingleThreadExecutor();

        executorService.submit(this::pollLoop);

        log.info("DeliveryCompletedEventConsumer started: "
                        + "bootstrapServers={}, topic={}, groupId={}",
                bootstrapServers, deliveryCompletedTopic, groupId);
    }

    // =========================================================
    // POLL LOOP
    // =========================================================

    private void pollLoop() {
        try {
            while (running) {
                var records = kafkaConsumer.poll(Duration.ofSeconds(1));

                for (ConsumerRecord<String, String> record : records) {
                    processRecord(record);
                }
            }

        } catch (WakeupException ex) {
            if (running) {
                log.error("Kafka consumer interrupted unexpectedly", ex);
            }

        } catch (Exception ex) {
            log.error("Fatal error in DeliveryCompletedEventConsumer", ex);

        } finally {
            closeConsumer();
        }
    }

    // =========================================================
    // PROCESS RECORD
    // =========================================================

    void processRecord(ConsumerRecord<String, String> record) {
        log.info("Received delivery.completed: "
                        + "topic={}, partition={}, offset={}, key={}",
                record.topic(), record.partition(), record.offset(), record.key());

        try {
            DeliveryCompletedEvent event = objectMapper.readValue(record.value(), DeliveryCompletedEvent.class);

            log.info("DeliveryCompletedEvent received: "
                            + "deliveryId={}, orderId={}, customerId={}",
                    event.deliveryId(), event.orderId(), event.customerId());

            // -------------------------------------------------
            // Complete the order
            // -------------------------------------------------

            orderService.completeOrder(event.orderId());

            // -------------------------------------------------
            // Commit only after successful processing
            // -------------------------------------------------

            kafkaConsumer.commitSync(Collections.singletonMap(new TopicPartition(record.topic(), record.partition()),
                    new org.apache.kafka.clients.consumer.OffsetAndMetadata(record.offset() + 1)));

            log.info("delivery.completed processed successfully: "
                            + "orderId={}, partition={}, offset={}",
                    event.orderId(), record.partition(), record.offset());

        } catch (Exception ex) {
            log.error("Failed to process delivery.completed: "
                            + "topic={}, partition={}, offset={}, payload={}",
                    record.topic(), record.partition(), record.offset(), record.value(), ex);

            // No offset commit on failure.
        }
    }

    // =========================================================
    // STOP
    // =========================================================

    @PreDestroy
    public void stop() {
        running = false;

        if (kafkaConsumer != null) {
            kafkaConsumer.wakeup();
        }

        if (executorService != null) {
            executorService.shutdown();
        }

        log.info("DeliveryCompletedEventConsumer stopped");
    }

    // =========================================================
    // CLOSE
    // =========================================================
    private void closeConsumer() {
        if (kafkaConsumer != null) {
            kafkaConsumer.close();

            log.info("DeliveryCompletedEventConsumer Kafka consumer closed");
        }
    }
}