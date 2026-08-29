package com.gwatcho.orderservice.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.util.List;

public record CreateOrderRequest(

        @NotNull
        @Positive
        Long userId,

        @NotEmpty
        @Size(min = 1, max = 100)
        List<@Valid OrderItemRequest> items,

        @NotNull
        @Valid
        DeliveryAddressRequest deliveryAddress,

        @NotBlank
        @Size(min = 3, max = 3)
        String currency
) {
}