package com.gwatcho.orderservice.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record OrderItemRequest(

        @NotNull
        @Positive
        Long productId,

        @NotBlank
        @Size(max = 255)
        String productName,

        @NotNull
        @Positive
        Integer quantity,

        @NotNull
        @Positive
        BigDecimal unitPrice
) {
}