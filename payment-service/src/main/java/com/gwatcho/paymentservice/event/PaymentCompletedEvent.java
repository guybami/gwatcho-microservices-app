package com.gwatcho.paymentservice.event;

import com.gwatcho.paymentservice.dto.DeliveryAddress;

import java.math.BigDecimal;

public record PaymentCompletedEvent(
        Long paymentId,
        Long orderId,
        Long customerId,
        BigDecimal amount,
        String currency,
        String transactionId,
        DeliveryAddress deliveryAddress
) {
}