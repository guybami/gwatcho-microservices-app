package com.gwatcho.productservice.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record ProductCreatedEvent(

        UUID eventId,

        Long productId,

        String sku,

        String name,

        BigDecimal price,

        String currency,

        Integer stockQuantity,

        String category,

        LocalDateTime occurredAt
) {
}