package com.gwatcho.orderservice.config;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springframework.web.client.RestClient;

@Configuration
@Slf4j
public class ProductServiceRestClientConfig {

    @Bean
    public RestClient productServiceRestClient(
            @Value("${services.product-service.url}")
            String productServiceUrl) {

        log.info(
                "Creating ProductService RestClient with base URL: {}",
                productServiceUrl
        );

        return RestClient.builder()
                .baseUrl(productServiceUrl)
                .requestInterceptor((request, body, execution) -> {

                    log.info(
                            "PRODUCT REQUEST: {} {}",
                            request.getMethod(),
                            request.getURI()
                    );

                    var response = execution.execute(
                            request,
                            body
                    );

                    log.info(
                            "PRODUCT RESPONSE: {} {}",
                            response.getStatusCode(),
                            request.getURI()
                    );

                    return response;
                })
                .build();
    }
}