package com.gwatcho.userservice.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.gwatcho.userservice.dto.*;
import com.gwatcho.userservice.entity.*;
import com.gwatcho.userservice.exception.*;

import com.gwatcho.userservice.repository.OutboxEventRepository;
import com.gwatcho.userservice.repository.UserRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    private final OutboxEventRepository outboxEventRepository;

    private final ObjectMapper objectMapper;

    @Transactional
    public UserResponse createUser(
            CreateUserRequest request) {

        if (userRepository.existsByEmail(request.email())) {
            throw new DuplicateUserException(
                    request.email()
            );
        }

        if (!request.email().contains("@")) {
            throw new BadEmailException(
                    request.email()
            );
        }

        User user = User.builder()
                .firstName(request.firstName())
                .lastName(request.lastName())
                .email(request.email())
                .status(UserStatus.ACTIVE)
                .build();

        User savedUser = userRepository.save(user);

        createOutboxEvent(savedUser);

        return toResponse(savedUser);
    }

    @Transactional(readOnly = true)
    public UserResponse getUser(Long id) {

        User user =
                userRepository.findById(id)
                        .orElseThrow(
                                () -> new UserNotFoundException(id)
                        );

        return toResponse(user);
    }

    @Transactional(readOnly = true)
    public List<UserResponse> getUsers() {

        return userRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public UserResponse updateUser(
            Long id,
            UpdateUserRequest request) {

        User user =
                userRepository.findById(id)
                        .orElseThrow(
                                () -> new UserNotFoundException(id)
                        );

        if (!user.getEmail().equals(request.email())
                && userRepository.existsByEmail(request.email())) {

            throw new DuplicateUserException(
                    request.email()
            );
        }

        user.setFirstName(request.firstName());
        user.setLastName(request.lastName());
        user.setEmail(request.email());

        User updatedUser =
                userRepository.save(user);

        return toResponse(updatedUser);
    }

    @Transactional
    public void deleteUser(Long id) {

        User user =
                userRepository.findById(id)
                        .orElseThrow(
                                () -> new UserNotFoundException(id)
                        );

        user.setStatus(UserStatus.INACTIVE);

        userRepository.save(user);
    }

    private void createOutboxEvent(User user) {

        UUID eventId =
                UUID.randomUUID();
        String status = user.getStatus() != null ? user.getStatus().name() : "ACTIVE";
        UserCreatedEvent event =
                new UserCreatedEvent(
                        eventId,
                        user.getId(),
                        user.getFirstName(),
                        user.getLastName(),
                        user.getEmail(),
                        status,
                        LocalDateTime.now()
                );

        try {

            String payload =
                    objectMapper.writeValueAsString(event);

            OutboxEvent outboxEvent =
                    OutboxEvent.builder()
                            .eventId(eventId)
                            .aggregateType("USER")
                            .aggregateId(
                                    user.getId().toString()
                            )
                            .eventType("UserCreated")
                            .payload(payload)
                            .status(OutboxStatus.NEW)
                            .build();

            outboxEventRepository.save(
                    outboxEvent
            );

        } catch (JsonProcessingException exception) {

            throw new IllegalStateException(
                    "Could not serialize UserCreatedEvent",
                    exception
            );
        }
    }

    private UserResponse toResponse(User user) {

        return new UserResponse(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                user.getStatus(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }
}