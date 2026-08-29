package com.gwatcho.productservice.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.gwatcho.productservice.dto.ProductCreatedEvent;
import com.gwatcho.productservice.entity.OutboxEvent;
import com.gwatcho.productservice.entity.OutboxStatus;
import com.gwatcho.productservice.entity.Product;
import com.gwatcho.productservice.repository.OutboxEventRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OutboxService {

    private final OutboxEventRepository outboxEventRepository;

    private final ObjectMapper objectMapper;


    public void createProductCreatedEvent(
            Product product) {

        UUID eventId =
                UUID.randomUUID();

        ProductCreatedEvent event =
                new ProductCreatedEvent(
                        eventId,
                        product.getId(),
                        product.getSku(),
                        product.getName(),
                        product.getPrice(),
                        product.getCurrency(),
                        product.getStockQuantity(),
                        product.getCategory(),
                        LocalDateTime.now()
                );

        try {

            String payload =
                    objectMapper.writeValueAsString(event);

            OutboxEvent outboxEvent =
                    OutboxEvent.builder()
                            .eventId(eventId)
                            .aggregateType("PRODUCT")
                            .aggregateId(
                                    product.getId().toString()
                            )
                            .eventType("product.created")
                            .payload(payload)
                            .status(OutboxStatus.NEW)
                            .build();

            outboxEventRepository.save(
                    outboxEvent
            );

        } catch (JsonProcessingException e) {
            throw new IllegalStateException(
                    "Could not serialize ProductCreatedEvent",
                    e
            );
        }
    }
}