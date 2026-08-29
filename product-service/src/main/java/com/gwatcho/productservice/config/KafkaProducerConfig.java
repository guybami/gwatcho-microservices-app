package com.gwatcho.productservice.config;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Properties;

@Configuration
public class KafkaProducerConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;


    @Bean
    public KafkaProducer<String, String> kafkaProducer() {

        Properties properties =
                new Properties();

        properties.put(
                ProducerConfig.BOOTSTRAP_SERVERS_CONFIG,
                bootstrapServers
        );

        properties.put(
                ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,
                StringSerializer.class
        );

        properties.put(
                ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,
                StringSerializer.class
        );

        /*
         * Reliability
         */
        properties.put(
                ProducerConfig.ACKS_CONFIG,
                "all"
        );

        /*
         * Avoid duplicates caused by producer retries.
         */
        properties.put(
                ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG,
                "true"
        );

        properties.put(
                ProducerConfig.RETRIES_CONFIG,
                Integer.MAX_VALUE
        );

        return new KafkaProducer<>(properties);
    }
}