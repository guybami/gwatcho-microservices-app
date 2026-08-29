package com.gwatcho.paymentservice.dto;

import com.gwatcho.paymentservice.entity.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PaymentResponse(
        Long id,
        Long orderId,
        Long customerId,
        BigDecimal amount,
        String currency,
        PaymentStatus status,
        String paymentMethod,
        String transactionId,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}