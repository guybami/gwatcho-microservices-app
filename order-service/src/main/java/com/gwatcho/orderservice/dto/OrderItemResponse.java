package com.gwatcho.orderservice.dto;

import java.math.BigDecimal;

public record OrderItemResponse(

        Long productId,

        String sku,

        String productName,

        BigDecimal unitPrice,

        Integer quantity,

        BigDecimal lineTotal
) {
}