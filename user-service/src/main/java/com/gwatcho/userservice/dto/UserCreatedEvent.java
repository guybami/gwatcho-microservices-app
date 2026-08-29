package com.gwatcho.userservice.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record UserCreatedEvent(

        UUID eventId,

        Long userId,

        String firstName,

        String lastName,

        String email,

        String status,

        LocalDateTime occurredAt
) {
}