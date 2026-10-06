package com.gwatcho.userservice.kafka;

import com.gwatcho.userservice.entity.OutboxEvent;
import com.gwatcho.userservice.entity.OutboxStatus;
import com.gwatcho.userservice.repository.OutboxEventRepository;
import java.util.List;
import java.util.concurrent.ExecutionException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class OutboxEventPublisher {
    private static final String USER_CREATED_TOPIC = "user.created";

    private final OutboxEventRepository outboxEventRepository;

    private final KafkaTemplate<String, String> kafkaTemplate;

    @Scheduled(fixedDelay = 1000)
    public void publishEvents() throws ExecutionException, InterruptedException {
        List<OutboxEvent> events = outboxEventRepository.findTop100ByStatusOrderByCreatedAtAsc(OutboxStatus.NEW);

        for (OutboxEvent event : events) {
            try {
                kafkaTemplate.send(USER_CREATED_TOPIC, event.getAggregateId(), event.getPayload()).get();

                log.info("Kafka message sent: topic={}, aggregateId={}, eventId={}", USER_CREATED_TOPIC, event.getAggregateId(),
                        event.getEventId());

                event.markPublished();

                outboxEventRepository.save(event);

            } catch (Exception e) {
                log.error("Could not publish event {}", event.getEventId(), e);
                throw e;
            }
        }
    }
}