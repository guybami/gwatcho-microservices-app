package com.gwatcho.productservice.event;

public record ProductStockRequestedEvent(
        Long orderId,
        Long productId,
        int quantity
) {
}