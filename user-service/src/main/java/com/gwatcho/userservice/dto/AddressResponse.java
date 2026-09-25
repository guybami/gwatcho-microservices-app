package com.gwatcho.userservice.dto;

public record AddressResponse(
        String street,
        String postalCode,
        String city,
        String country
) {
}
