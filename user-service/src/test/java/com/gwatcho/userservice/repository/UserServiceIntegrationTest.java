package com.gwatcho.userservice.repository;

import com.gwatcho.userservice.entity.User;
import com.gwatcho.userservice.entity.UserStatus;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class UserServiceIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    void shouldSaveAndLoadUser() {

        User user = User.builder()
                .firstName("John")
                .lastName("Doe")
                .email("john@example.com")
                .status(UserStatus.ACTIVE)
                .build();

        User saved =
                userRepository.save(user);

        assertThat(saved.getId())
                .isNotNull();

        User loaded =
                userRepository.findById(saved.getId())
                        .orElseThrow();

        assertThat(loaded.getEmail())
                .isEqualTo("john@example.com");

        assertThat(loaded.getFirstName())
                .isEqualTo("John");
    }
}