package com.gwatcho.orderservice.dto;

import java.math.BigDecimal;

public record OrderItemEvent(

        Long productId,

        String productName,

        Integer quantity,

        BigDecimal unitPrice,

        BigDecimal subtotal
) {
}