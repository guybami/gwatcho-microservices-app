package com.gwatcho.userservice.controller;

import com.gwatcho.userservice.dto.CustomerResponse;
import com.gwatcho.userservice.dto.CustomerUpdateRequest;
import com.gwatcho.userservice.service.CustomerService;

import org.springframework.http.ResponseEntity;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
public class CustomerController {

    private final CustomerService customerService;

    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    /**
     * GET /api/users/me
     *
     * Returns the currently authenticated customer.
     *
     * If the customer does not exist in MySQL yet,
     * it will automatically be created from Keycloak.
     */
    @GetMapping("/me")
    public ResponseEntity<CustomerResponse> getCurrentCustomer(
            @AuthenticationPrincipal Jwt jwt
    ) {

        CustomerResponse customer =
                customerService.getOrCreateCustomer(jwt);

        return ResponseEntity.ok(customer);
    }

    /**
     * PUT /api/users/me
     *
     * Updates the profile/address of the current customer.
     */
    @PutMapping("/me")
    public ResponseEntity<CustomerResponse> updateCurrentCustomer(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody CustomerUpdateRequest request
    ) {

        CustomerResponse customer =
                customerService.updateCustomer(jwt, request);

        return ResponseEntity.ok(customer);
    }
}