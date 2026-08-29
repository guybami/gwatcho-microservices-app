package com.gwatcho.orderservice.e2e;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.gwatcho.orderservice.dto.CheckoutItemRequest;
import com.gwatcho.orderservice.dto.CheckoutRequest;
import com.gwatcho.orderservice.dto.DeliveryAddressRequest;
import com.gwatcho.orderservice.dto.OrderResponse;
import com.gwatcho.orderservice.dto.ProductSnapshot;

import com.gwatcho.orderservice.entity.OrderOutboxEvent;
import com.gwatcho.orderservice.entity.OutboxStatus;

import com.gwatcho.orderservice.outbox.OrderOutboxPublisher;
import com.gwatcho.orderservice.repository.OrderOutboxEventRepository;
import com.gwatcho.orderservice.service.OrderService;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        properties = {
                "spring.kafka.bootstrap-servers=localhost:9092",
                "app.kafka.topics.order-created=order.created",
                "spring.task.scheduling.enabled=false"
        }
)
class OrderKafkaEndToEndTest {

    private static final String ORDER_CREATED_TOPIC =
            "order.created";

    private static final String KAFKA_BOOTSTRAP_SERVERS =
            "localhost:9092";


    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderOutboxPublisher outboxPublisher;

    @Autowired
    private OrderOutboxEventRepository outboxRepository;

    @Autowired
    private ObjectMapper objectMapper;


    // =========================================================
    // CHECKOUT -> OUTBOX -> KAFKA
    // =========================================================

    @Test
    void shouldCheckoutCreateOutboxAndPublishOrderCreatedToKafka()
            throws Exception {

        // =====================================================
        // 1. CHECKOUT REQUEST
        // =====================================================

        CheckoutRequest request =
                createCheckoutRequest();


        // =====================================================
        // 2. PRODUCT SNAPSHOTS
        // =====================================================

        List<ProductSnapshot> products =
                createProducts();


        // =====================================================
        // 3. CREATE ORDER
        // =====================================================

        OrderResponse order =
                orderService.createOrder(
                        request,
                        products
                );


        assertThat(order)
                .isNotNull();

        assertThat(order.id())
                .isNotNull();

        assertThat(order.customerId())
                .isEqualTo(100L);

        assertThat(order.status())
                .isEqualTo(
                        com.gwatcho.orderservice.entity.OrderStatus.CREATED
                );

        assertThat(order.currency())
                .isEqualTo("EUR");

        assertThat(order.totalAmount())
                .isEqualByComparingTo(
                        new BigDecimal("2693.28")
                );

        assertThat(order.items())
                .hasSize(2);


        // =====================================================
        // 4. FIND OUTBOX EVENT FOR THIS ORDER
        // =====================================================

        OrderOutboxEvent outboxEvent =
                findOutboxEventForOrder(
                        order.id()
                );


        assertThat(outboxEvent)
                .isNotNull();

        assertThat(outboxEvent.getEventId())
                .isNotNull();

        assertThat(outboxEvent.getEventId())
                .isNotBlank();

        assertThat(outboxEvent.getAggregateId())
                .isEqualTo(order.id());

        assertThat(outboxEvent.getAggregateType())
                .isEqualTo("Order");

        assertThat(outboxEvent.getEventType())
                .isEqualTo("OrderCreated");

        assertThat(outboxEvent.getTopic())
                .isEqualTo(
                        ORDER_CREATED_TOPIC
                );

        assertThat(outboxEvent.getStatus())
                .isEqualTo(
                        OutboxStatus.PENDING
                );

        assertThat(outboxEvent.getPayload())
                .isNotBlank();


        // =====================================================
        // 5. CREATE KAFKA CONSUMER
        // =====================================================

        try (
                KafkaConsumer<String, String> consumer =
                        createConsumer()
        ) {

            consumer.subscribe(
                    List.of(
                            ORDER_CREATED_TOPIC
                    )
            );


            // =================================================
            // 6. PUBLISH OUTBOX EVENT
            // =================================================

            outboxPublisher.publishPendingEvents();


            // =================================================
            // 7. WAIT FOR KAFKA EVENT
            // =================================================

            ConsumerRecord<String, String> record =
                    waitForOrderCreatedEvent(
                            consumer,
                            order.id()
                    );


            assertThat(record)
                    .isNotNull();


            // =================================================
            // 8. VERIFY KAFKA TOPIC
            // =================================================

            assertThat(record.topic())
                    .isEqualTo(
                            ORDER_CREATED_TOPIC
                    );


            // =================================================
            // 9. VERIFY KAFKA KEY
            // =================================================

            /*
             * Kafka key = eventId
             *
             * This is important because eventId is the
             * unique identity of the event.
             */

            assertThat(record.key())
                    .isEqualTo(
                            outboxEvent.getEventId()
                    );


            // =================================================
            // 10. VERIFY EVENT PAYLOAD
            // =================================================

            JsonNode event =
                    objectMapper.readTree(
                            record.value()
                    );


            assertThat(
                    event.get("eventId").asText()
            )
                    .isEqualTo(
                            outboxEvent.getEventId()
                    );


            assertThat(
                    event.get("eventType").asText()
            )
                    .isEqualTo(
                            "OrderCreated"
                    );


            assertThat(
                    event.get("orderId").asLong()
            )
                    .isEqualTo(
                            order.id()
                    );


            assertThat(
                    event.get("customerId").asLong()
            )
                    .isEqualTo(100L);


            assertThat(
                    event.get("status").asText()
            )
                    .isEqualTo(
                            "CREATED"
                    );


            assertThat(
                    event.get("currency").asText()
            )
                    .isEqualTo(
                            "EUR"
                    );


            assertThat(
                    event.get("totalAmount").decimalValue()
            )
                    .isEqualByComparingTo(
                            new BigDecimal("2693.28")
                    );


            // =================================================
            // 11. VERIFY DELIVERY ADDRESS
            // =================================================

            assertThat(
                    event.get("street").asText()
            )
                    .isEqualTo(
                            "Main Street 10"
                    );


            assertThat(
                    event.get("postalCode").asText()
            )
                    .isEqualTo(
                            "74172"
                    );


            assertThat(
                    event.get("city").asText()
            )
                    .isEqualTo(
                            "Neckarsulm"
                    );


            assertThat(
                    event.get("country").asText()
            )
                    .isEqualTo(
                            "DE"
                    );


            // =================================================
            // 12. VERIFY ITEMS
            // =================================================

            JsonNode items =
                    event.get("items");


            assertThat(items)
                    .isNotNull();

            assertThat(items.size())
                    .isEqualTo(2);


            JsonNode item1 =
                    items.get(0);

            assertThat(
                    item1.get("productId").asLong()
            )
                    .isEqualTo(1L);

            assertThat(
                    item1.get("sku").asText()
            )
                    .isEqualTo(
                            "OUTBOX-001"
                    );

            assertThat(
                    item1.get("productName").asText()
            )
                    .isEqualTo(
                            "Outbox Test Product"
                    );

            assertThat(
                    item1.get("unitPrice").decimalValue()
            )
                    .isEqualByComparingTo(
                            new BigDecimal("99.99")
                    );

            assertThat(
                    item1.get("quantity").asInt()
            )
                    .isEqualTo(2);

            assertThat(
                    item1.get("lineTotal").decimalValue()
            )
                    .isEqualByComparingTo(
                            new BigDecimal("199.98")
                    );


            JsonNode item2 =
                    items.get(1);

            assertThat(
                    item2.get("productId").asLong()
            )
                    .isEqualTo(25L);

            assertThat(
                    item2.get("sku").asText()
            )
                    .isEqualTo(
                            "SKU-023"
                    );

            assertThat(
                    item2.get("productName").asText()
            )
                    .isEqualTo(
                            "Smart Accessories Product 023"
                    );

            assertThat(
                    item2.get("unitPrice").decimalValue()
            )
                    .isEqualByComparingTo(
                            new BigDecimal("831.10")
                    );

            assertThat(
                    item2.get("quantity").asInt()
            )
                    .isEqualTo(3);

            assertThat(
                    item2.get("lineTotal").decimalValue()
            )
                    .isEqualByComparingTo(
                            new BigDecimal("2493.30")
                    );
        }


        // =====================================================
        // 13. VERIFY OUTBOX PUBLISHED
        // =====================================================

        OrderOutboxEvent publishedEvent =
                outboxRepository
                        .findById(
                                outboxEvent.getId()
                        )
                        .orElseThrow();


        assertThat(
                publishedEvent.getStatus()
        )
                .isEqualTo(
                        OutboxStatus.PUBLISHED
                );


        assertThat(
                publishedEvent.getPublishedAt()
        )
                .isNotNull();


        assertThat(
                publishedEvent.getEventId()
        )
                .isEqualTo(
                        outboxEvent.getEventId()
                );
    }


    // =========================================================
    // FIND OUTBOX EVENT FOR ORDER
    // =========================================================

    private OrderOutboxEvent findOutboxEventForOrder(
            Long orderId) {

        return outboxRepository
                .findAll()
                .stream()
                .filter(event ->
                        orderId.equals(
                                event.getAggregateId()
                        )
                )
                .findFirst()
                .orElseThrow(() ->
                        new AssertionError(
                                "No outbox event found for order "
                                        + orderId
                        )
                );
    }


    // =========================================================
    // KAFKA CONSUMER
    // =========================================================

    private KafkaConsumer<String, String>
    createConsumer() {

        Properties properties =
                new Properties();

        properties.put(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,
                KAFKA_BOOTSTRAP_SERVERS
        );

        properties.put(
                ConsumerConfig.GROUP_ID_CONFIG,
                "order-e2e-test-"
                        + UUID.randomUUID()
        );

        properties.put(
                ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
                "org.apache.kafka.common.serialization.StringDeserializer"
        );

        properties.put(
                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
                "org.apache.kafka.common.serialization.StringDeserializer"
        );

        /*
         * Important for a new consumer group.
         */
        properties.put(
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG,
                "earliest"
        );

        properties.put(
                ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG,
                "false"
        );

        return new KafkaConsumer<>(
                properties
        );
    }


    // =========================================================
    // WAIT FOR ORDER CREATED EVENT
    // =========================================================

    private ConsumerRecord<String, String>
    waitForOrderCreatedEvent(
            KafkaConsumer<String, String> consumer,
            Long orderId)
            throws Exception {

        long timeout =
                System.currentTimeMillis()
                        + 15_000;


        while (
                System.currentTimeMillis()
                        < timeout
        ) {

            var records =
                    consumer.poll(
                            Duration.ofMillis(500)
                    );


            for (
                    ConsumerRecord<String, String> record
                    : records
            ) {

                JsonNode json =
                        objectMapper.readTree(
                                record.value()
                        );


                if (
                        json.has("orderId")
                                &&
                                json.get("orderId")
                                        .asLong()
                                        == orderId
                ) {

                    return record;
                }
            }
        }


        throw new AssertionError(
                "order.created event was not received for order "
                        + orderId
        );
    }


    // =========================================================
    // CHECKOUT REQUEST
    // =========================================================

    private CheckoutRequest createCheckoutRequest() {

        return new CheckoutRequest(
                100L,
                "EUR",
                createDeliveryAddressRequest(),
                List.of(
                        new CheckoutItemRequest(
                                1L,
                                2
                        ),
                        new CheckoutItemRequest(
                                25L,
                                3
                        )
                )
        );
    }


    // =========================================================
    // DELIVERY ADDRESS
    // =========================================================

    private DeliveryAddressRequest
    createDeliveryAddressRequest() {

        return new DeliveryAddressRequest(
                "Main Street 10",
                "74172",
                "Neckarsulm",
                "DE"
        );
    }


    // =========================================================
    // PRODUCT SNAPSHOTS
    // =========================================================

    private List<ProductSnapshot>
    createProducts() {

        return List.of(

                new ProductSnapshot(
                        1L,
                        "OUTBOX-001",
                        "Outbox Test Product",
                        new BigDecimal("99.99"),
                        "EUR",
                        10
                ),

                new ProductSnapshot(
                        25L,
                        "SKU-023",
                        "Smart Accessories Product 023",
                        new BigDecimal("831.10"),
                        "EUR",
                        10
                )
        );
    }
}