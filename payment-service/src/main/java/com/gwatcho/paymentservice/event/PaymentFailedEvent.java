package com.gwatcho.paymentservice.event;

import java.math.BigDecimal;

public record PaymentFailedEvent(
        Long paymentId,
        Long orderId,
        Long customerId,
        BigDecimal amount,
        String currency,
        String reason
) {
}