package com.gwatcho.productservice.service;

import com.gwatcho.productservice.dto.CreateProductRequest;
import com.gwatcho.productservice.entity.OutboxEvent;
import com.gwatcho.productservice.entity.OutboxStatus;
import com.gwatcho.productservice.repository.OutboxEventRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import java.math.BigDecimal;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class ProductOutboxIntegrationTest {

    @Autowired
    private ProductService productService;

    @Autowired
    private OutboxEventRepository outboxEventRepository;


    @Test
    void shouldCreateProductAndOutboxEvent() {

        CreateProductRequest request =
                new CreateProductRequest(
                        "OUTBOX-001",
                        "Outbox Test Product",
                        "Testing transactional outbox",
                        new BigDecimal("99.99"),
                        "EUR",
                        10,
                        "TEST"
                );

        var response = productService.createProduct(request);

        assertThat(response.id()).isNotNull();

        var events =     outboxEventRepository
                        .findByStatusOrderByCreatedAtAsc(
                                OutboxStatus.NEW,
                                PageRequest.of(
                                        0,
                                        100
                                )
                        );

        assertThat(events)
                .anyMatch(event ->
                        event.getAggregateId()
                                .equals(response.id().toString())
                                &&
                                event.getEventType()
                                        .equals("product.created")
                );
    }
}