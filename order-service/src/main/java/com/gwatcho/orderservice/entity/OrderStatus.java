package com.gwatcho.orderservice.entity;

public enum OrderStatus {

    CREATED,
    PAYMENT_PENDING,
    PAID,
    PAYMENT_FAILED,
    STOCK_PENDING,
    STOCK_RESERVED,
    STOCK_FAILED,
    SHIPPED,
    DELIVERED,
    COMPLETED,
    CANCELLED
}