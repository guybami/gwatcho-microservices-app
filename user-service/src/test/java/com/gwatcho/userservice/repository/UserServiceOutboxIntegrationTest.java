package com.gwatcho.userservice.repository;


import com.gwatcho.userservice.entity.OutboxEvent;
import com.gwatcho.userservice.service.UserService;
import com.gwatcho.userservice.entity.OutboxStatus;
import com.gwatcho.userservice.dto.CreateUserRequest;
import com.gwatcho.userservice.dto.UserResponse;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;



@SpringBootTest
@ActiveProfiles("test")
class UserServiceOutboxIntegrationTest {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @Test
    void shouldCreateUserAndOutboxEvent() {

        CreateUserRequest request =
                new CreateUserRequest(
                        "Alice",
                        "Smith",
                        "alice@example.com"
                );

        UserResponse response =
                userService.createUser(request);

        assertThat(response.id())
                .isNotNull();

        assertThat(
                userRepository.existsById(
                        response.id()
                )
        ).isTrue();

        List<OutboxEvent> events =  outboxEventRepository.findAll();

        assertThat(events).hasSize(1);

        OutboxEvent event =  events.get(0);

        assertThat(event.getAggregateType())
                .isEqualTo("USER");

        assertThat(event.getAggregateId())
                .isEqualTo(
                        response.id().toString()
                );

        assertThat(event.getEventType())
                .isEqualTo("UserCreated");

        assertThat(event.getStatus())
                .isEqualTo(OutboxStatus.NEW);
    }
}
