package com.gwatcho.productservice.kafka;

import com.gwatcho.productservice.event.ProductStockRequestedEvent;
import com.gwatcho.productservice.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ProductStockConsumer {
    private final ProductService productService;

    @KafkaListener(topics = "${app.kafka.topics.product-stock-requested}", groupId = "${app.kafka.consumer.group-id}")
    public void consume(ConsumerRecord<String, ProductStockRequestedEvent> record) {
        ProductStockRequestedEvent event = record.value();

        log.info("Received product.stock.requested: topic={}, partition={}, offset={}, key={}", record.topic(),
                record.partition(), record.offset(), record.key());

        log.info("Product stock request: productId={}, quantity={}", event.productId(), event.quantity());

        try {
            productService.removeStock(event.productId(), event.quantity());
            log.info("Product stock updated successfully: productId={}, quantity={}", event.productId(), event.quantity());
        } catch (Exception e) {
            log.error("Failed to process product.stock.requested: "
                            + "productId={}, quantity={}, partition={}, offset={}",
                    event.productId(), event.quantity(), record.partition(), record.offset(), e);

            throw e;
        }
    }
}