package com.gwatcho.orderservice.outbox;

import com.gwatcho.orderservice.entity.OrderOutboxEvent;
import com.gwatcho.orderservice.entity.OutboxStatus;
import com.gwatcho.orderservice.repository.OrderOutboxEventRepository;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderOutboxPublisher {
    private final OrderOutboxEventRepository outboxRepository;

    private final KafkaTemplate<String, String> kafkaTemplate;

    @Scheduled(fixedDelay = 1000)
    public void publishPendingEvents() {
        List<OrderOutboxEvent> events = outboxRepository.findTop100ByStatusOrderByCreatedAtAsc(OutboxStatus.PENDING);

        for (OrderOutboxEvent event : events) {
            publish(event);
        }
    }

    private void publish(OrderOutboxEvent event) {
        try {
            log.info("Publishing outbox event: eventId={}, aggregateId={}, topic={}", event.getEventId(),
                    event.getAggregateId(), event.getTopic());

            kafkaTemplate.send(event.getTopic(), event.getEventId(), event.getPayload()).get();

            markAsPublished(event);

            log.info("Published order outbox event: eventId={}, orderId={}, eventType={}, topic={}", event.getEventId(),
                    event.getAggregateId(), event.getEventType(), event.getTopic());

        } catch (Exception ex) {
            log.error(
                    "Failed to publish outbox event: eventId={}, orderId={}", event.getEventId(), event.getAggregateId(), ex);

            event.setAttempts(event.getAttempts() + 1);

            event.setLastError(ex.getMessage());

            outboxRepository.save(event);
        }
    }

    @Transactional
    protected void markAsPublished(OrderOutboxEvent event) {
        event.setStatus(OutboxStatus.PUBLISHED);

        event.setPublishedAt(LocalDateTime.now());

        outboxRepository.save(event);
    }
}