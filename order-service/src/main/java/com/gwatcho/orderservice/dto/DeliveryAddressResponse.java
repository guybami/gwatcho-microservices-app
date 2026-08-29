package com.gwatcho.orderservice.dto;

public record DeliveryAddressResponse(

        String street,

        String houseNumber,

        String postalCode,

        String city,

        String country
) {
}