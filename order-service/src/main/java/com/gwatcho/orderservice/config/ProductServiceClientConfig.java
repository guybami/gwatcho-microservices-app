package com.gwatcho.orderservice.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;


public class ProductServiceClientConfig {


    public RestClient productRestClient(
            @Value("${services.product-service.url}")
            String productServiceUrl) {

        return RestClient.builder()
                .baseUrl(productServiceUrl)
                .build();
    }
}