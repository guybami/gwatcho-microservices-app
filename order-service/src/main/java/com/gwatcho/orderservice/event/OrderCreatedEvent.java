package com.gwatcho.orderservice.event;

import com.gwatcho.orderservice.dto.OrderItemResponse;
import com.gwatcho.orderservice.entity.OrderStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record OrderCreatedEvent(

        String eventId,

        String eventType,

        Long orderId,

        Long customerId,

        OrderStatus status,

        BigDecimal totalAmount,

        String currency,

        String paymentMethod,

        String street,

        String postalCode,

        String city,

        String country,

        List<OrderItemResponse> items,

        LocalDateTime createdAt

) {
}