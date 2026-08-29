package com.gwatcho.orderservice.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record CheckoutRequest(

        @NotNull(message = "Customer ID is required")
        Long customerId,

        @NotNull(message = "Currency is required")
        String currency,

        @NotNull(message = "Delivery address is required")
        @Valid
        DeliveryAddressRequest deliveryAddress,

        @NotEmpty(message = "Checkout must contain at least one item")
        @Valid
        List<CheckoutItemRequest> items

) {
}