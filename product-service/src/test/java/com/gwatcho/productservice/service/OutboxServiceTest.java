package com.gwatcho.productservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import com.gwatcho.productservice.entity.Product;
import com.gwatcho.productservice.entity.OutboxEvent;
import com.gwatcho.productservice.entity.OutboxStatus;
import com.gwatcho.productservice.repository.OutboxEventRepository;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class OutboxServiceTest {

    @Test
    void shouldCreateProductCreatedEvent() {

        OutboxEventRepository repository =
                mock(OutboxEventRepository.class);

        ObjectMapper objectMapper =
                new ObjectMapper()
                        .registerModule(
                                new JavaTimeModule()
                        );

        OutboxService outboxService =
                new OutboxService(
                        repository,
                        objectMapper
                );

        Product product =
                Product.builder()
                        .id(1L)
                        .sku("LAPTOP-001")
                        .name("Laptop")
                        .price(
                                new BigDecimal("1200.00")
                        )
                        .currency("EUR")
                        .stockQuantity(10)
                        .category("COMPUTERS")
                        .build();

        outboxService.createProductCreatedEvent(
                product
        );

        ArgumentCaptor<OutboxEvent> captor =
                ArgumentCaptor.forClass(
                        OutboxEvent.class
                );

        verify(repository)
                .save(captor.capture());

        OutboxEvent event =
                captor.getValue();

        assertThat(event.getEventId())
                .isNotNull();

        assertThat(event.getAggregateType())
                .isEqualTo("PRODUCT");

        assertThat(event.getAggregateId())
                .isEqualTo("1");

        assertThat(event.getEventType())
                .isEqualTo("product.created");

        assertThat(event.getStatus())
                .isEqualTo(OutboxStatus.NEW);

        assertThat(event.getPayload())
                .contains("LAPTOP-001");
    }
}