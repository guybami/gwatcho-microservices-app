package com.gwatcho.paymentservice.dto;


public record DeliveryAddress(
        String street,
        String postalCode,
        String city,
        String country
) {
}