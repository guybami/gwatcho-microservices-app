package com.gwatcho.orderservice.e2e;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gwatcho.orderservice.entity.Order;
import com.gwatcho.orderservice.entity.OrderStatus;
import com.gwatcho.orderservice.repository.OrderRepository;
import java.time.Duration;
import java.util.Collections;
import java.util.Properties;
import java.util.UUID;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class OrderPaymentDeliveryEndToEndTest {
    private static final String KAFKA_BOOTSTRAP_SERVERS = "localhost:9092";

    private static final String PRODUCT_SERVICE_URL = "http://localhost:8081/product-service";

    private static final String DELIVERY_SERVICE_URL = "http://localhost:8084/delivery-service";

    private static final String PAYMENT_COMPLETED_TOPIC = "payment.completed";

    private static final String DELIVERY_COMPLETED_TOPIC = "delivery.completed";

    private static final int TIMEOUT_SECONDS = 30;

    @LocalServerPort private int port;

    @Autowired private TestRestTemplate restTemplate;

    @Autowired private ObjectMapper objectMapper;

    @Autowired private OrderRepository orderRepository;

    @Test
    void shouldProcessOrderPaymentAndDeliveryEndToEnd() throws Exception {
        // =====================================================
        // 1. Verify ProductService
        // =====================================================

        Long productId = 1L;

        ResponseEntity<String> productResponse = restTemplate.getRestTemplate().exchange(
                PRODUCT_SERVICE_URL + "/products/{id}", HttpMethod.GET, null, String.class, productId);

        assertThat(productResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        assertThat(productResponse.getBody()).isNotNull();

        JsonNode product = objectMapper.readTree(productResponse.getBody());

        assertThat(product.get("id").asLong()).isEqualTo(productId);

        System.out.println("Product verified: productId=" + productId);

        // =====================================================
        // 2. Checkout / create order
        // =====================================================

        String requestBody = """
        {
            "customerId" : 200, "currency" : "EUR",
                "street" : "E2E Test Street 100",
                "postalCode" : "74172",
                "city" : "Neckarsulm",
                "country" : "DE",
                "paymentMethod"
          : "CARD",
                "items" : [{"productId" : 1, "quantity" : 2}]
        }
        """;

        HttpHeaders headers = new HttpHeaders();

        headers.set("Content-Type", "application/json");

        HttpEntity<String> request = new HttpEntity<>(requestBody, headers);

        ResponseEntity<String> checkoutResponse = restTemplate.exchange(
                "http://localhost:" + port + "/order-service/orders/checkout", HttpMethod.POST, request, String.class);

        System.out.println("Checkout status: " + checkoutResponse.getStatusCode());

        System.out.println("Checkout response: " + checkoutResponse.getBody());

        assertThat(checkoutResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        assertThat(checkoutResponse.getBody()).isNotNull();

        JsonNode orderJson = objectMapper.readTree(checkoutResponse.getBody());

        Long orderId = orderJson.get("id").asLong();

        assertThat(orderId).isPositive();

        System.out.println("Order created: orderId=" + orderId);

        // =====================================================
        // 3. Wait for payment.completed
        // =====================================================

        PaymentCompletedEvent paymentEvent = waitForPaymentCompleted(orderId);

        assertThat(paymentEvent).isNotNull();

        assertThat(paymentEvent.orderId()).isEqualTo(orderId);

        assertThat(paymentEvent.paymentId()).isPositive();

        System.out.println("Payment completed: paymentId=" + paymentEvent.paymentId() + ", orderId=" + orderId);

        // =====================================================
        // 4. Wait for DeliveryService to create delivery
        //
        // We verify this through delivery.completed later.
        // The delivery status is then driven through the
        // DeliveryService REST endpoint.
        // =====================================================

        Long deliveryId = waitForDelivery(orderId);

        assertThat(deliveryId).isNotNull();

        System.out.println("Delivery created: deliveryId=" + deliveryId + ", orderId=" + orderId);

        // =====================================================
        // 5. CREATED -> PREPARING
        // =====================================================

        updateDeliveryStatus(deliveryId, "PREPARING");

        // =====================================================
        // 6. PREPARING -> SHIPPED
        // =====================================================

        updateDeliveryStatus(deliveryId, "SHIPPED");

        // =====================================================
        // 7. SHIPPED -> DELIVERED
        // =====================================================

        updateDeliveryStatus(deliveryId, "DELIVERED");

        // =====================================================
        // 8. Wait for delivery.completed
        // =====================================================

        DeliveryCompletedEvent deliveryEvent = waitForDeliveryCompleted(orderId);

        assertThat(deliveryEvent).isNotNull();

        assertThat(deliveryEvent.orderId()).isEqualTo(orderId);

        assertThat(deliveryEvent.deliveryId()).isEqualTo(deliveryId);

        System.out.println("Delivery completed: deliveryId=" + deliveryId + ", orderId=" + orderId);

        // =====================================================
        // 9. Wait for OrderService to process
        //    delivery.completed
        // =====================================================

        Order completedOrder = waitForOrderStatus(orderId, OrderStatus.COMPLETED);

        // =====================================================
        // 10. FINAL ASSERTIONS
        // =====================================================

        assertThat(completedOrder).isNotNull();

        assertThat(completedOrder.getId()).isEqualTo(orderId);

        assertThat(completedOrder.getStatus()).isEqualTo(OrderStatus.COMPLETED);

        System.out.println(
                """
                
                ==========================================
                E2E TEST SUCCESS
                ==========================================
                orderId     = %s
                paymentId   = %s
                deliveryId  = %s
                orderStatus = %s
                ==========================================
                """.formatted(
                        orderId,
                        paymentEvent.paymentId(),
                        deliveryId,
                        completedOrder.getStatus()
                )
        );
    }

    // =========================================================
    // PRODUCT / DELIVERY
    // =========================================================

    private Long waitForDelivery(Long orderId) throws InterruptedException, JsonProcessingException {
        long timeout = System.currentTimeMillis() + TIMEOUT_SECONDS * 1_000L;

        while (System.currentTimeMillis() < timeout) {
            /*
             * Replace this section with the actual delivery
             * GET endpoint if DeliveryController exposes one.
             *
             * The preferred implementation is:
             *
             * GET /delivery-service/deliveries/order/{orderId}
             */

            ResponseEntity<String> response = restTemplate.getForEntity("http://localhost:8084/"
                            + "delivery-service/"
                            + "deliveries/order/{orderId}",
                    String.class, orderId);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                JsonNode json = objectMapper.readTree(response.getBody());

                if (json.has("id")) {
                    return json.get("id").asLong();
                }
            }

            Thread.sleep(500);
        }

        throw new AssertionError("Timed out waiting for delivery "
                + "for orderId=" + orderId);
    }

    // =========================================================
    // DELIVERY STATUS
    // =========================================================

    private void updateDeliveryStatus(Long deliveryId, String status) {
        String body = """
        {
            "status" : "%s"
        }
        """.formatted(status);

        HttpHeaders headers = new HttpHeaders();

        headers.set("Content-Type", "application/json");

        HttpEntity<String> request = new HttpEntity<>(body, headers);

        ResponseEntity<String> response = restTemplate.exchange("http://localhost:8084/"
                        + "delivery-service/"
                        + "deliveries/{deliveryId}/status",
                HttpMethod.PUT, request, String.class, deliveryId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        System.out.println("Delivery status updated: deliveryId=" + deliveryId + ", status=" + status);
    }

    // =========================================================
    // PAYMENT COMPLETED
    // =========================================================

    private PaymentCompletedEvent waitForPaymentCompleted(Long orderId) throws Exception {
        try (KafkaConsumer<String, String> consumer = createConsumer("e2e-payment-" + UUID.randomUUID())) {
            consumer.subscribe(Collections.singletonList(PAYMENT_COMPLETED_TOPIC));

            long timeout = System.currentTimeMillis() + TIMEOUT_SECONDS * 1_000L;

            while (System.currentTimeMillis() < timeout) {
                var records = consumer.poll(Duration.ofMillis(500));

                for (ConsumerRecord<String, String> record : records) {
                    JsonNode json = objectMapper.readTree(record.value());

                    if (json.has("orderId") && json.get("orderId").asLong() == orderId) {
                        return new PaymentCompletedEvent(json.get("paymentId").asLong(), json.get("orderId").asLong());
                    }
                }
            }
        }

        throw new AssertionError("Timed out waiting for payment.completed "
                + "for orderId=" + orderId);
    }

    // =========================================================
    // DELIVERY COMPLETED
    // =========================================================

    private DeliveryCompletedEvent waitForDeliveryCompleted(Long orderId) throws Exception {
        try (KafkaConsumer<String, String> consumer = createConsumer("e2e-delivery-" + UUID.randomUUID())) {
            consumer.subscribe(Collections.singletonList(DELIVERY_COMPLETED_TOPIC));

            long timeout = System.currentTimeMillis() + TIMEOUT_SECONDS * 1_000L;

            while (System.currentTimeMillis() < timeout) {
                var records = consumer.poll(Duration.ofMillis(500));

                for (ConsumerRecord<String, String> record : records) {
                    JsonNode json = objectMapper.readTree(record.value());

                    if (json.has("orderId") && json.get("orderId").asLong() == orderId) {
                        return new DeliveryCompletedEvent(json.get("deliveryId").asLong(), json.get("orderId").asLong());
                    }
                }
            }
        }

        throw new AssertionError("Timed out waiting for delivery.completed "
                + "for orderId=" + orderId);
    }

    // =========================================================
    // FINAL ORDER STATUS
    // =========================================================

    private Order waitForOrderStatus(Long orderId, OrderStatus expectedStatus) throws InterruptedException {
        long timeout = System.currentTimeMillis() + TIMEOUT_SECONDS * 1_000L;

        while (System.currentTimeMillis() < timeout) {
            Order order = orderRepository.findById(orderId).orElse(null);

            if (order != null && order.getStatus() == expectedStatus) {
                return order;
            }

            Thread.sleep(500);
        }

        Order finalOrder = orderRepository.findById(orderId).orElse(null);

        assertThat(finalOrder).as("Order must exist after E2E processing").isNotNull();

        assertThat(finalOrder.getStatus()).as("Final order status").isEqualTo(expectedStatus);

        return finalOrder;
    }

    // =========================================================
    // KAFKA CONSUMER
    // =========================================================

    private KafkaConsumer<String, String> createConsumer(String groupId) {
        Properties properties = new Properties();

        properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA_BOOTSTRAP_SERVERS);

        properties.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);

        properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);

        properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);

        properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        properties.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);

        return new KafkaConsumer<>(properties);
    }

    // =========================================================
    // TEST EVENTS
    // =========================================================

    private record PaymentCompletedEvent(Long paymentId, Long orderId) {}

    private record DeliveryCompletedEvent(Long deliveryId, Long orderId) {}
}
