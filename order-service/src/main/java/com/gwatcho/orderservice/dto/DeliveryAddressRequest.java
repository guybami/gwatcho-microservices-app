package com.gwatcho.orderservice.dto;

import jakarta.validation.constraints.NotBlank;

public record DeliveryAddressRequest(

        @NotBlank(message = "Street is required")
        String street,

        @NotBlank(message = "Postal code is required")
        String postalCode,

        @NotBlank(message = "City is required")
        String city,

        @NotBlank(message = "Country is required")
        String country
) {
}