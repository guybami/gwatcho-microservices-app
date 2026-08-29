package com.gwatcho.productservice.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gwatcho.productservice.event.ProductStockRequestedEvent;
import com.gwatcho.productservice.service.ProductService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.stereotype.Component;

@Component
public class ProductStockConsumer {

    private final ProductService productService;
    private final ObjectMapper objectMapper;

    public ProductStockConsumer(
            ProductService productService,
            ObjectMapper objectMapper) {

        this.productService = productService;
        this.objectMapper = objectMapper;
    }

    public void consume(
            ConsumerRecord<String, String> record) {

        try {

            ProductStockRequestedEvent event =
                    objectMapper.readValue(
                            record.value(),
                            ProductStockRequestedEvent.class
                    );

            productService.removeStock(
                    event.productId(),
                    event.quantity()
            );

        } catch (Exception e) {

            throw new IllegalStateException(
                    "Failed to process product stock request",
                    e
            );
        }
    }
}