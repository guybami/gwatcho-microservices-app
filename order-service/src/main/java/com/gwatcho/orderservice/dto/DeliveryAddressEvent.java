package com.gwatcho.orderservice.dto;

public record DeliveryAddressEvent(

        String street,
        String houseNumber,
        String postalCode,
        String city,
        String country
) {
}