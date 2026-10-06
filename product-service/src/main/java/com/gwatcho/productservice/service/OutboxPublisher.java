package com.gwatcho.productservice.service;

import com.gwatcho.productservice.entity.OutboxEvent;
import com.gwatcho.productservice.entity.OutboxStatus;
import com.gwatcho.productservice.repository.OutboxEventRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OutboxPublisher {
    private final OutboxEventRepository outboxEventRepository;
    private final OutboxStatusService outboxStatusService;
    private final KafkaTemplate<String, String> kafkaTemplate;

    @Value("${outbox.publisher.batch-size:100}") private int batchSize;
    @Value("${outbox.publisher.max-retries:5}") private int maxRetries;

    @Scheduled(fixedDelayString = "${outbox.publisher.fixed-delay:5000}")
    public void publishEvents() {
        List<OutboxEvent> events =
                outboxEventRepository.findByStatusOrderByCreatedAtAsc(OutboxStatus.NEW, PageRequest.of(0, batchSize));
        for (OutboxEvent event : events) {
            publishEvent(event);
        }
    }

    protected void publishEvent(OutboxEvent event) {
        try {
            kafkaTemplate.send(event.getEventType(), event.getAggregateId(), event.getPayload()).get();
            outboxStatusService.markPublished(event.getId());
        } catch (Exception exception) {
            String message = exception.getMessage();
            outboxStatusService.markFailedAttempt(event.getId(), message, maxRetries);
        }
    }
}