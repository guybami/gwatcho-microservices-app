package com.gwatcho.userservice.controller;

import com.gwatcho.userservice.dto.UserResponse;
import com.gwatcho.userservice.entity.UserStatus;
import com.gwatcho.userservice.service.UserService;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;

import org.springframework.boot.test.mock.mockito.MockBean;

import org.springframework.http.MediaType;

import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @Test
    void shouldCreateUser() throws Exception {

        UserResponse response =
                new UserResponse(
                        1L,
                        "John",
                        "Doe",
                        "john@example.com",
                        UserStatus.ACTIVE,
                        null,
                        null
                );

        when(userService.createUser(any()))
                .thenReturn(response);

        mockMvc.perform(
                        post("/api/users")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                            {
                                "firstName": "John",
                                "lastName": "Doe",
                                "email": "john@example.com"
                            }
                            """)
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.firstName").value("John"))
                .andExpect(jsonPath("$.lastName").value("Doe"))
                .andExpect(jsonPath("$.email").value("john@example.com"));
    }

    @Test
    void shouldRejectInvalidEmail() throws Exception {

        mockMvc.perform(
                        post("/api/users")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                            {
                                "firstName": "John",
                                "lastName": "Doe",
                                "email": "invalid-email"
                            }
                            """)
                )
                .andExpect(status(). isBadRequest());
    }

    @Test
    void shouldGetUser() throws Exception {

        UserResponse response = new UserResponse(
                        1L,
                        "John",
                        "Doe",
                        "john@example.com",
                        UserStatus.ACTIVE,
                        null,
                        null
                );

        when(userService.getUser(1L))
                .thenReturn(response);

        mockMvc.perform(get("/api/users/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.email")
                        .value("john@example.com"));
    }

    @Test
    void shouldGetUsers() throws Exception {

        UserResponse user =
                new UserResponse(
                        1L,
                        "John",
                        "Doe",
                        "john@example.com",
                        UserStatus.ACTIVE,
                        null,
                        null
                );

        when(userService.getUsers())
                .thenReturn(List.of(user));

        mockMvc.perform(
                        get("/api/users")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].email")
                        .value("john@example.com"));
    }
}