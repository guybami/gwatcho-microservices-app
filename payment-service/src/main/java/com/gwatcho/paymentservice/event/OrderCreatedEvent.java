package com.gwatcho.paymentservice.event;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record OrderCreatedEvent(
        String eventId,
        String eventType,
        Long orderId,
        Long customerId,
        String status,
        BigDecimal totalAmount,
        String currency,
        String paymentMethod,
        String street,
        String postalCode,
        String city,
        String country,
        List<OrderItemEvent> items,
        LocalDateTime createdAt
) {
}
