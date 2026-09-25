package com.gwatcho.userservice.dto;

public record CustomerUpdateRequest(

        String firstName,

        String lastName,

        String phone,

        String street,

        String houseNumber,

        String postalCode,

        String city,

        String country

) {
}