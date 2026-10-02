package com.gwatcho.orderservice.client;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@RequiredArgsConstructor
public class ProductServiceClient {

    private final RestClient productRestClient;

    public ProductResponse getProduct(Long productId) {

        return productRestClient
                .get()
                .uri("/{id}", productId)
                .retrieve()
                .body(ProductResponse.class);
    }
}