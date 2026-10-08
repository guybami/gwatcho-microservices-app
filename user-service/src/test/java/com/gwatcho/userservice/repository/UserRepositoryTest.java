package com.gwatcho.userservice.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.gwatcho.userservice.entity.User;
import com.gwatcho.userservice.entity.UserStatus;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

@DataJpaTest
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    @Test
    void shouldSaveAndFindUserByEmail() {
        User user = createUser(
                "john@example.com",
                "John",
                "Doe"
        );

        User savedUser = userRepository.save(user);

        Optional<User> result =
                userRepository.findByEmail("john@example.com");

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(savedUser.getId());
        assertThat(result.get().getEmail()).isEqualTo("john@example.com");
        assertThat(result.get().getFirstName()).isEqualTo("John");
        assertThat(result.get().getLastName()).isEqualTo("Doe");
    }

    @Test
    void shouldReturnEmptyWhenUserDoesNotExist() {
        Optional<User> result =
                userRepository.findByEmail("unknown@example.com");

        assertThat(result).isEmpty();
    }

    @Test
    void shouldCheckIfEmailExists() {
        User user = createUser(
                "john@example.com",
                "John",
                "Doe"
        );

        userRepository.save(user);

        assertThat(userRepository.existsByEmail("john@example.com"))
                .isTrue();

        assertThat(userRepository.existsByEmail("unknown@example.com"))
                .isFalse();
    }

    @Test
    void shouldSaveUserWithStatus() {
        User user = createUser(
                "john@example.com",
                "John",
                "Doe"
        );

        user.setStatus(UserStatus.ACTIVE);

        User savedUser = userRepository.save(user);

        assertThat(savedUser.getStatus())
                .isEqualTo(UserStatus.ACTIVE);
    }

    private User createUser(
            String email,
            String firstName,
            String lastName) {

        User user = new User();

        user.setEmail(email);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setStatus(UserStatus.ACTIVE);

        return user;
    }
}
