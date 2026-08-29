package com.gwatcho.paymentservice.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record PaymentRequest(

        @NotNull(message = "Order ID is required")
        Long orderId,

        @NotNull(message = "Customer ID is required")
        Long customerId,

        @NotNull(message = "Amount is required")
        @DecimalMin(
                value = "0.01",
                message = "Amount must be greater than zero"
        )
        BigDecimal amount,

        @NotBlank(message = "Currency is required")
        String currency,

        @NotBlank(message = "Payment method is required")
        String paymentMethod
) {
}