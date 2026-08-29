package com.gwatcho.productservice.kafka;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;

import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Collections;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Component
public class ProductStockConsumerRunner {

    private static final String TOPIC =
            "product.stock.requested";

    private final KafkaConsumer<String, String> kafkaConsumer;
    private final ProductStockConsumer productStockConsumer;

    private final ExecutorService executor =
            Executors.newSingleThreadExecutor();

    private volatile boolean running = true;

    public ProductStockConsumerRunner(
            KafkaConsumer<String, String> kafkaConsumer,
            ProductStockConsumer productStockConsumer) {

        this.kafkaConsumer = kafkaConsumer;
        this.productStockConsumer = productStockConsumer;
    }

    @PostConstruct
    public void start() {

        executor.submit(this::consumeMessages);
    }

    private void consumeMessages() {

        try {

            kafkaConsumer.subscribe(
                    Collections.singletonList(TOPIC)
            );

            while (running) {

                ConsumerRecords<String, String> records =
                        kafkaConsumer.poll(
                                Duration.ofMillis(1000)
                        );

                for (ConsumerRecord<String, String> record : records) {

                    try {

                        productStockConsumer.consume(record);

                        kafkaConsumer.commitSync();

                    } catch (Exception e) {

                        System.err.println(
                                "Failed to process Kafka message. "
                                        + "Topic: " + record.topic()
                                        + ", Partition: " + record.partition()
                                        + ", Offset: " + record.offset()
                                        + ", Error: " + e.getMessage()
                        );
                    }
                }
            }

        } catch (Exception e) {

            if (running) {
                System.err.println(
                        "Kafka consumer stopped unexpectedly: "
                                + e.getMessage()
                );
            }

        } finally {

            kafkaConsumer.close();
        }
    }

    @PreDestroy
    public void stop() {

        running = false;

        executor.shutdownNow();
    }
}