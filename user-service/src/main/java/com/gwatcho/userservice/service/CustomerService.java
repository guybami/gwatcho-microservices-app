package com.gwatcho.userservice.service;

import com.gwatcho.userservice.dto.CustomerResponse;
import com.gwatcho.userservice.dto.CustomerUpdateRequest;
import com.gwatcho.userservice.entity.Customer;
import com.gwatcho.userservice.repository.CustomerRepository;

import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class CustomerService {

    private final CustomerRepository customerRepository;

    public CustomerService(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    /**
     * Returns the customer associated with the authenticated
     * Keycloak user.
     *
     * If the customer does not yet exist, it is created from
     * the Keycloak JWT claims.
     */
    public CustomerResponse getOrCreateCustomer(Jwt jwt) {

        String keycloakId = jwt.getSubject();

        return customerRepository
                .findByKeycloakId(keycloakId)
                .map(CustomerResponse::from)
                .orElseGet(() -> {

                    Customer customer = createFromKeycloak(jwt);

                    Customer saved = customerRepository.save(customer);

                    return CustomerResponse.from(saved);
                });
    }

    /**
     * Get customer by Keycloak ID.
     */
    @Transactional(readOnly = true)
    public CustomerResponse getCustomerByKeycloakId(String keycloakId) {

        Customer customer = customerRepository
                .findByKeycloakId(keycloakId)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Customer not found for Keycloak ID: " + keycloakId
                        )
                );

        return CustomerResponse.from(customer);
    }

    /**
     * Update the currently authenticated customer.
     */
    public CustomerResponse updateCustomer(
            Jwt jwt,
            CustomerUpdateRequest request
    ) {

        String keycloakId = jwt.getSubject();

        Customer customer = customerRepository
                .findByKeycloakId(keycloakId)
                .orElseGet(() -> createFromKeycloak(jwt));

        if (request.firstName() != null) {
            customer.setFirstName(request.firstName());
        }

        if (request.lastName() != null) {
            customer.setLastName(request.lastName());
        }

        if (request.phone() != null) {
            customer.setPhone(request.phone());
        }

        if (request.street() != null) {
            customer.setStreet(request.street());
        }

        if (request.houseNumber() != null) {
            customer.setHouseNumber(request.houseNumber());
        }

        if (request.postalCode() != null) {
            customer.setPostalCode(request.postalCode());
        }

        if (request.city() != null) {
            customer.setCity(request.city());
        }

        if (request.country() != null) {
            customer.setCountry(request.country());
        }

        Customer saved = customerRepository.save(customer);

        return CustomerResponse.from(saved);
    }

    /**
     * Create a Customer from Keycloak JWT.
     */
    private Customer createFromKeycloak(Jwt jwt) {

        Customer customer = new Customer();

        customer.setKeycloakId(jwt.getSubject());

        customer.setEmail(
                getClaim(jwt, "email", "")
        );

        customer.setFirstName(
                getClaim(jwt, "given_name", "")
        );

        customer.setLastName(
                getClaim(jwt, "family_name", "")
        );

        return customer;
    }

    /**
     * Safely read a String claim.
     */
    private String getClaim(
            Jwt jwt,
            String claim,
            String defaultValue
    ) {

        String value = jwt.getClaimAsString(claim);

        return value != null ? value : defaultValue;
    }
}