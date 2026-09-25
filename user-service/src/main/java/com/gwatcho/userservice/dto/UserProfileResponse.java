package com.gwatcho.userservice.dto;

public record UserProfileResponse(
        Long id,
        String firstName,
        String lastName,
        String email,
        String phone,
        AddressResponse address
) {
}