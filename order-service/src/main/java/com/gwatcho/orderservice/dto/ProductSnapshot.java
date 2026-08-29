package com.gwatcho.orderservice.dto;

import java.math.BigDecimal;

public record ProductSnapshot(

        Long productId,
        String sku,
        String productName,
        BigDecimal unitPrice,
        String currency,
        Integer stockQuantity
) {
}