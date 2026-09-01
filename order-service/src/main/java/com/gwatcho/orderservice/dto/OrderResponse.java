package com.gwatcho.orderservice.dto;

import com.gwatcho.orderservice.entity.OrderStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record OrderResponse(

        Long id,

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

        Long version,

        LocalDateTime createdAt,

        LocalDateTime updatedAt
) {
}