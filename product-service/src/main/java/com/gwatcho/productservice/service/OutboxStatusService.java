package com.gwatcho.productservice.service;

import com.gwatcho.productservice.entity.OutboxEvent;
import com.gwatcho.productservice.entity.OutboxStatus;
import com.gwatcho.productservice.repository.OutboxEventRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class OutboxStatusService {

    private final OutboxEventRepository outboxEventRepository;


    @Transactional
    public void markPublished(
            Long eventId) {

        OutboxEvent event = outboxEventRepository
                        .findById(eventId)
                        .orElseThrow();

        event.setStatus(OutboxStatus.PUBLISHED
        );

        event.setPublishedAt(
                LocalDateTime.now()
        );
    }


    @Transactional
    public void markFailedAttempt(
            Long eventId,
            String error,
            int maxRetries) {

        OutboxEvent event =
                outboxEventRepository
                        .findById(eventId)
                        .orElseThrow();

        int retryCount =
                event.getRetryCount() + 1;

        event.setRetryCount(retryCount);

        event.setLastError(error);

        if (retryCount >= maxRetries) {

            event.setStatus(
                    OutboxStatus.FAILED
            );
        }
    }
}