package com.gwatcho.userservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gwatcho.userservice.dto.CreateUserRequest;
import com.gwatcho.userservice.dto.UserResponse;
import com.gwatcho.userservice.entity.User;
import com.gwatcho.userservice.exception.DuplicateUserException;
import com.gwatcho.userservice.repository.OutboxEventRepository;
import com.gwatcho.userservice.repository.UserRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class UserServiceTest {

    private UserRepository userRepository;

    private OutboxEventRepository outboxEventRepository;

    private ObjectMapper objectMapper;

    private UserService userService;

    @BeforeEach
    void setUp() {

        userRepository = mock(UserRepository.class);
        outboxEventRepository = mock(OutboxEventRepository.class);
        objectMapper = mock(ObjectMapper.class);

        userService = new UserService(
                userRepository,
                outboxEventRepository,
                objectMapper
        );
    }

    @Test
    void shouldCreateUser() throws Exception {

        // Given
        CreateUserRequest request =
                new CreateUserRequest(
                        "John",
                        "Doe",
                        "john@example.com"
                );

        when(userRepository.existsByEmail(request.email()))
                .thenReturn(false);

        User savedUser = User.builder()
                .id(1L)
                .firstName("John")
                .lastName("Doe")
                .email("john@example.com")
                .build();

        when(userRepository.save(any(User.class)))
                .thenReturn(savedUser);

        when(objectMapper.writeValueAsString(any()))
                .thenReturn("{\"userId\":1}");

        // When
        UserResponse result = userService.createUser(request);

        // Then
        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.firstName()).isEqualTo("John");
        assertThat(result.lastName()).isEqualTo("Doe");
        assertThat(result.email())
                .isEqualTo("john@example.com");

        verify(userRepository)
                .save(any(User.class));

        verify(outboxEventRepository)
                .save(any());
    }

    @Test
    void shouldRejectDuplicateEmail() {

        // Given
        CreateUserRequest request =
                new CreateUserRequest(
                        "John",
                        "Doe",
                        "john@example.com"
                );

        when(userRepository.existsByEmail(request.email()))
                .thenReturn(true);

        // When
        DuplicateUserException exception =
                assertThrows(
                        DuplicateUserException.class,
                        () -> userService.createUser(request)
                );

        // Then
        assertThat(exception.getMessage())
                .contains("already exists");

        verify(userRepository, never())
                .save(any());

        verify(outboxEventRepository, never())
                .save(any());
    }

    @Test
    void shouldGetUser() {

        // Given
        User user = User.builder()
                .id(1L)
                .firstName("John")
                .lastName("Doe")
                .email("john@example.com")
                .build();

        when(userRepository.findById(1L))
                .thenReturn(Optional.of(user));

        // When
        UserResponse result =
                userService.getUser(1L);

        // Then
        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.email())
                .isEqualTo("john@example.com");
    }

    @Test
    void shouldThrowExceptionWhenUserDoesNotExist() {

        when(userRepository.findById(999L))
                .thenReturn(Optional.empty());

        assertThrows(
                RuntimeException.class,
                () -> userService.getUser(999L)
        );
    }

    @Test
    void shouldGetAllUsers() {

        User user1 = User.builder()
                .id(1L)
                .firstName("John")
                .lastName("Doe")
                .email("john@example.com")
                .build();

        User user2 = User.builder()
                .id(2L)
                .firstName("Alice")
                .lastName("Smith")
                .email("alice@example.com")
                .build();

        when(userRepository.findAll())
                .thenReturn(List.of(user1, user2));

        List<UserResponse> result =
                userService.getUsers();

        assertThat(result)
                .hasSize(2);

        assertThat(result.get(0).email())
                .isEqualTo("john@example.com");

        assertThat(result.get(1).email())
                .isEqualTo("alice@example.com");
    }
}