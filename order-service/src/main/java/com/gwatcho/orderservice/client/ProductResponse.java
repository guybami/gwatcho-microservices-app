package com.gwatcho.orderservice.client;

import java.math.BigDecimal;

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

        Long version
) {
}