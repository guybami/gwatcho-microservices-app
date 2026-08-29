package com.gwatcho.userservice.kafka;

import com.gwatcho.userservice.entity.OutboxEvent;
import com.gwatcho.userservice.entity.OutboxStatus;
import com.gwatcho.userservice.repository.OutboxEventRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.Future;

@Component
@RequiredArgsConstructor
@Slf4j
public class OutboxEventPublisher {

    private static final String USER_CREATED_TOPIC = "user.created";

    private final OutboxEventRepository outboxEventRepository;

    private final KafkaProducer<String, String> kafkaProducer;

    @Scheduled(fixedDelay = 1000)
    public void publishEvents() {

        List<OutboxEvent> events =
                outboxEventRepository
                        .findTop100ByStatusOrderByCreatedAtAsc(
                                OutboxStatus.NEW
                        );

        for (OutboxEvent event : events) {

            try {

                ProducerRecord<String, String> record =
                        new ProducerRecord<>(
                                USER_CREATED_TOPIC,
                                event.getAggregateId(),
                                event.getPayload()
                        );

                Future<RecordMetadata> future =
                        kafkaProducer.send(record);

                RecordMetadata metadata =
                        future.get();

                log.info(
                        "Kafka message sent: topic={}, partition={}, offset={}",
                        metadata.topic(),
                        metadata.partition(),
                        metadata.offset()
                );
                event.markPublished();
                outboxEventRepository.save(event);
            } catch (Exception e) {
                log.error(
                        "Could not publish event {}",
                        event.getEventId(),
                        e
                );
            }
        }
    }
}