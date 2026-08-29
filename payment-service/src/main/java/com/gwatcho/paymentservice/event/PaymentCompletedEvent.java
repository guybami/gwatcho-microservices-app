package com.gwatcho.paymentservice.event;

import java.math.BigDecimal;

public record PaymentCompletedEvent(
        Long paymentId,
        Long orderId,
        Long customerId,
        BigDecimal amount,
        String currency,
        String transactionId
) {
}