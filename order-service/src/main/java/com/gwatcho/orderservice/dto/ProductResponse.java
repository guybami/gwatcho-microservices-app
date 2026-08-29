package com.gwatcho.orderservice.client.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ProductResponse(

        Long id,
        String sku,
        String name,
        String description,
        BigDecimal price,
        String currency,
        Integer stockQuantity,
        String status,
        String category,
        Long version,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}