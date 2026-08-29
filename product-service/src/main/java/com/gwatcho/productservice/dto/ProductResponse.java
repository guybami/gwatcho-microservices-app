package com.gwatcho.productservice.dto;

import com.gwatcho.productservice.entity.ProductStatus;

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
        ProductStatus status,
        String category,
        Long version,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}