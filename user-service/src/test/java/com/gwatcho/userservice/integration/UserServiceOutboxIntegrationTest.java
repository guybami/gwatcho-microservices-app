package com.gwatcho.userservice.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.gwatcho.userservice.dto.CreateUserRequest;
import com.gwatcho.userservice.dto.UserResponse;
import com.gwatcho.userservice.entity.OutboxEvent;
import com.gwatcho.userservice.entity.OutboxStatus;
import com.gwatcho.userservice.repository.OutboxEventRepository;
import com.gwatcho.userservice.repository.UserRepository;
import com.gwatcho.userservice.service.UserService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class UserServiceOutboxIntegrationTest {
    @Autowired private UserService userService;

    @Autowired private UserRepository userRepository;

    @Autowired private OutboxEventRepository outboxEventRepository;

    @BeforeEach
    void cleanDatabase() {
        outboxEventRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void shouldCreateUserAndOutboxEvent() {
        CreateUserRequest request = new CreateUserRequest("Alice", "Smith", "alice@example.com");

        UserResponse response = userService.createUser(request);

        assertThat(response.id()).isNotNull();

        assertThat(userRepository.existsById(response.id())).isTrue();

        List<OutboxEvent> events = outboxEventRepository.findAll();

        assertThat(events).hasSize(1);

        OutboxEvent event = events.get(0);

        assertThat(event.getAggregateType()).isEqualTo("USER");

        assertThat(event.getAggregateId()).isEqualTo(response.id().toString());

        assertThat(event.getEventType()).isEqualTo("UserCreated");

        assertThat(event.getStatus()).isEqualTo(OutboxStatus.NEW);
    }
}