package com.gwatcho.orderservice.entity;

public enum OutboxStatus {

    NEW,
    PUBLISHED,
    FAILED,
    PENDING
}