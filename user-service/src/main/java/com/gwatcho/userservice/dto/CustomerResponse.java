package com.gwatcho.userservice.dto;

import com.gwatcho.userservice.entity.Customer;

import java.time.LocalDateTime;

public record CustomerResponse(

        Long id,

        String keycloakId,

        String firstName,

        String lastName,

        String email,

        String phone,

        String street,

        String houseNumber,

        String postalCode,

        String city,

        String country,

        LocalDateTime createdAt,

        LocalDateTime updatedAt

) {

    public static CustomerResponse from(Customer customer) {

        return new CustomerResponse(
                customer.getId(),
                customer.getKeycloakId(),
                customer.getFirstName(),
                customer.getLastName(),
                customer.getEmail(),
                customer.getPhone(),
                customer.getStreet(),
                customer.getHouseNumber(),
                customer.getPostalCode(),
                customer.getCity(),
                customer.getCountry(),
                customer.getCreatedAt(),
                customer.getUpdatedAt()
        );
    }
}