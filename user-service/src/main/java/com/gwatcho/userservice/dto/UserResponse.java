package com.gwatcho.userservice.dto;

import com.gwatcho.userservice.entity.UserStatus;

import java.time.LocalDateTime;

public record UserResponse(
        Long id,
        String firstName,
        String lastName,
        String email,
        UserStatus status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}