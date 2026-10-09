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
import lombok.extern.slf4j.Slf4j;
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
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;


/**
 * End-to-end test for:
 * <p>
 * ProductService
 * ↓
 * OrderService
 * ↓
 * Kafka: order.created
 * ↓
 * ┌───────────────┐
 * │               │
 * ▼               ▼
 * PaymentService  DeliveryService
 * │               │
 * ▼               ▼
 * payment.completed delivery.completed
 * │
 * ▼
 * OrderService
 * │
 * ▼
 * COMPLETED
 * <p>
 * <p>
 * Authentication:
 * <p>
 * E2E Test
 * ↓
 * Keycloak
 * ↓
 * JWT
 * ↓
 * OrderService / DeliveryService
 */
@Slf4j
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class OrderPaymentDeliveryEndToEndTest {

    // =========================================================
    // INFRASTRUCTURE
    // =========================================================

    private static final String KAFKA_BOOTSTRAP_SERVERS = "localhost:9092";

    private static final String KEYCLOAK_TOKEN_URL = "http://localhost:8080/realms/gwatcho-shop"
            + "/protocol/openid-connect/token";

    private static final String PRODUCT_SERVICE_URL = "http://localhost:8083/product-service";

    private static final String DELIVERY_SERVICE_URL = "http://localhost:8084/delivery-service";

    // =========================================================
    // KAFKA TOPICS
    // =========================================================

    private static final String PAYMENT_COMPLETED_TOPIC = "payment.completed";

    private static final String DELIVERY_COMPLETED_TOPIC = "delivery.completed";

    // =========================================================
    // TEST CONFIGURATION
    // =========================================================

    private static final int TIMEOUT_SECONDS = 30;

    private static final String TEST_CLIENT_ID = "e2e-test-client";

    private static final String TEST_USERNAME = "e2e-test-user";

    // =========================================================
    // SPRING
    // =========================================================
    @LocalServerPort private int port;

    @Autowired private TestRestTemplate restTemplate;

    @Autowired private ObjectMapper objectMapper;

    @Autowired private OrderRepository orderRepository;

    // =========================================================
    // MAIN E2E TEST
    // =========================================================

    @Test
    void shouldProcessOrderPaymentAndDeliveryEndToEnd() throws Exception {
        // =====================================================
        // 0. AUTHENTICATION
        // =====================================================

        String accessToken = getAccessToken();

        assertThat(accessToken).as("Keycloak access token").isNotBlank();

        log.info("E2E authentication successful");

        // =====================================================
        // 1. CREATE E2E PRODUCT
        // =====================================================

        String sku = "E2E-" + UUID.randomUUID();
        String productRequestBody = """
        {
            "sku" : "%s",
                "name" : "E2E Test Product",
                "description" : "Product created for E2E testing",
                "price" : 99.99,
                "currency" : "EUR",
                "stockQuantity" : 100,
                "category" : "E2E"
        }
        """
                .formatted(sku);

        HttpEntity<String> productRequest = new HttpEntity<>(productRequestBody, jsonHeaders(accessToken));

        ResponseEntity<String> productCreateResponse =
                restTemplate.exchange(PRODUCT_SERVICE_URL + "/api/products", HttpMethod.POST, productRequest, String.class);

        log.info("Product creation status: {}", productCreateResponse.getStatusCode());

        log.debug("Product creation response: {}", productCreateResponse.getBody());

        assertThat(productCreateResponse.getStatusCode()).as("Product creation").isEqualTo(HttpStatus.CREATED);

        assertThat(productCreateResponse.getBody()).isNotNull();

        JsonNode createdProduct = objectMapper.readTree(productCreateResponse.getBody());

        Long productId = createdProduct.get("id").asLong();

        assertThat(productId).as("Created product ID").isPositive();

        assertThat(createdProduct.get("sku").asText()).isEqualTo(sku);

        log.info("E2E product created: productId={}, sku={}", productId, sku);

        // =====================================================
        // 2. VERIFY PRODUCT SERVICE
        // =====================================================

        ResponseEntity<String> productResponse = restTemplate.exchange(PRODUCT_SERVICE_URL + "/api/products/{id}",
                HttpMethod.GET, authenticatedRequest(accessToken), String.class, productId);

        log.info("Product verification status: {}", productResponse.getStatusCode());

        assertThat(productResponse.getStatusCode()).as("Product verification").isEqualTo(HttpStatus.OK);

        assertThat(productResponse.getBody()).isNotNull();

        JsonNode product = objectMapper.readTree(productResponse.getBody());

        assertThat(product.get("id").asLong()).isEqualTo(productId);

        assertThat(product.get("sku").asText()).isEqualTo(sku);

        assertThat(product.get("stockQuantity").asInt()).isEqualTo(100);

        log.info("Product verified: productId={}, sku={}, stockQuantity={}", productId, sku,
                product.get("stockQuantity").asInt());

        // =====================================================
        // 3. CHECKOUT / CREATE ORDER
        // =====================================================

        String requestBody = """
        {
            "customerId" : 200, "currency" : "EUR",
                "paymentMethod" : "CARD",
                "deliveryAddress"
          : {"street" : "E2E Test Street 100", "postalCode" : "74172", "city" : "Neckarsulm", "country" : "DE"},
            "items" : [{"productId" : % d, "quantity" : 2}]
        }
        """
                .formatted(productId);

        HttpHeaders headers = jsonHeaders(accessToken);

        HttpEntity<String> request = new HttpEntity<>(requestBody, headers);

        ResponseEntity<String> checkoutResponse = restTemplate.exchange(
                "http://localhost:" + port + "/order-service/orders/checkout", HttpMethod.POST, request, String.class);

        log.info("Checkout status: {}", checkoutResponse.getStatusCode());

        log.debug("Checkout response: {}", checkoutResponse.getBody());

        assertThat(checkoutResponse.getStatusCode()).as("Checkout").isEqualTo(HttpStatus.CREATED);

        assertThat(checkoutResponse.getBody()).isNotNull();

        JsonNode orderJson = objectMapper.readTree(checkoutResponse.getBody());

        Long orderId = orderJson.get("id").asLong();

        assertThat(orderId).as("Created order ID").isPositive();

        log.info("Order created: orderId={}, productId={}", orderId, productId);

        // =====================================================
        // 4. WAIT FOR PAYMENT.COMPLETED
        // =====================================================

        PaymentCompletedEvent paymentEvent = waitForPaymentCompleted(orderId);

        assertThat(paymentEvent).isNotNull();

        assertThat(paymentEvent.orderId()).isEqualTo(orderId);

        assertThat(paymentEvent.paymentId()).isPositive();

        log.info("Payment completed: paymentId={}, orderId={}", paymentEvent.paymentId(), orderId);

        // =====================================================
        // 5. WAIT FOR DELIVERY CREATION
        // =====================================================

        Long deliveryId = waitForDelivery(orderId, accessToken);

        assertThat(deliveryId).isNotNull();

        assertThat(deliveryId).isPositive();

        log.info("Delivery created: deliveryId={}, orderId={}", deliveryId, orderId);

        // =====================================================
        // 6. CREATED -> PREPARING
        // =====================================================

        updateDeliveryStatus(deliveryId, "PREPARING", accessToken);

        // =====================================================
        // 7. PREPARING -> SHIPPED
        // =====================================================

        updateDeliveryStatus(deliveryId, "SHIPPED", accessToken);

        // =====================================================
        // 8. SHIPPED -> DELIVERED
        // =====================================================

        updateDeliveryStatus(deliveryId, "DELIVERED", accessToken);

        // =====================================================
        // 9. WAIT FOR DELIVERY.COMPLETED
        // =====================================================

        DeliveryCompletedEvent deliveryEvent = waitForDeliveryCompleted(orderId);

        assertThat(deliveryEvent).isNotNull();

        assertThat(deliveryEvent.orderId()).isEqualTo(orderId);

        assertThat(deliveryEvent.deliveryId()).isEqualTo(deliveryId);

        log.info("Delivery completed: deliveryId={}, orderId={}", deliveryId, orderId);

        // =====================================================
        // 10. WAIT FOR ORDER TO BECOME COMPLETED
        // =====================================================

        Order completedOrder = waitForOrderStatus(orderId, OrderStatus.COMPLETED);

        // =====================================================
        // 11. FINAL ASSERTIONS
        // =====================================================

        assertThat(completedOrder).isNotNull();

        assertThat(completedOrder.getId()).isEqualTo(orderId);

        assertThat(completedOrder.getStatus()).isEqualTo(OrderStatus.COMPLETED);

        // =====================================================
        // SUCCESS
        // =====================================================

        log.info("""
            == == == == == == == == == == == == == == == == == == == == == E2E TEST SUCCESS == == == == == == == == ==
            == == == == == == == == == == ==
            == productId = {} orderId = {} paymentId = {} deliveryId = {} orderStatus = {} == == == == == == == == == ==
            == == == == == == == == == ==
            == """,
        productId,
                orderId, paymentEvent.paymentId(), deliveryId, completedOrder.getStatus());
    }

    // =========================================================
    // KEYCLOAK AUTHENTICATION
    // =========================================================
    private String getAccessToken() {
        String clientSecret = getRequiredEnvironmentVariable("E2E_KEYCLOAK_CLIENT_SECRET");

        String password = getRequiredEnvironmentVariable("E2E_KEYCLOAK_PASSWORD");

        HttpHeaders headers = new HttpHeaders();

        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();

        form.add("grant_type", "password");
        form.add("client_id", TEST_CLIENT_ID);
        form.add("client_secret", clientSecret);
        form.add("username", TEST_USERNAME);
        form.add("password", password);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(form, headers);

        ResponseEntity<String> response =
                restTemplate.getRestTemplate().exchange(KEYCLOAK_TOKEN_URL, HttpMethod.POST, request, String.class);

        assertThat(response.getStatusCode()).as("Keycloak token endpoint").isEqualTo(HttpStatus.OK);

        assertThat(response.getBody()).isNotNull();

        try {
            JsonNode json = objectMapper.readTree(response.getBody());

            JsonNode accessToken = json.get("access_token");

            assertThat(accessToken).as("Keycloak access_token").isNotNull();

            assertThat(accessToken.asText()).isNotBlank();

            return accessToken.asText();

        } catch (JsonProcessingException e) {
            throw new AssertionError("Unable to parse Keycloak token response", e);
        }
    }

    // =========================================================
    // HTTP HEADERS
    // =========================================================

    private HttpEntity<Void> authenticatedRequest(String accessToken) {
        HttpHeaders headers = new HttpHeaders();

        headers.setBearerAuth(accessToken);

        return new HttpEntity<>(headers);
    }

    private HttpHeaders jsonHeaders(String accessToken) {
        HttpHeaders headers = new HttpHeaders();

        headers.setContentType(MediaType.APPLICATION_JSON);

        headers.setBearerAuth(accessToken);

        return headers;
    }

    // =========================================================
    // DELIVERY
    // =========================================================

    private Long waitForDelivery(Long orderId, String accessToken) throws InterruptedException, JsonProcessingException {
        long timeout = System.currentTimeMillis() + TIMEOUT_SECONDS * 1_000L;

        while (System.currentTimeMillis() < timeout) {
            ResponseEntity<String> response = restTemplate.exchange(DELIVERY_SERVICE_URL + "/deliveries/order/{orderId}",
                    HttpMethod.GET, authenticatedRequest(accessToken), String.class, orderId);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                JsonNode json = objectMapper.readTree(response.getBody());

                if (json.has("id")) {
                    return json.get("id").asLong();
                }

            } else {
                log.debug("Delivery not available yet: orderId={}, status={}", orderId, response.getStatusCode());
            }

            Thread.sleep(500);
        }

        throw new AssertionError("Timed out waiting for delivery "
                + "for orderId=" + orderId);
    }

    // =========================================================
    // DELIVERY STATUS
    // =========================================================

    private void updateDeliveryStatus(Long deliveryId, String status, String accessToken) {
        String body = """
        {
            "status" : "%s"
        }
        """
                .formatted(status);

        HttpHeaders headers = jsonHeaders(accessToken);

        HttpEntity<String> request = new HttpEntity<>(body, headers);

        ResponseEntity<String> response = restTemplate.exchange(
                DELIVERY_SERVICE_URL + "/deliveries/{deliveryId}/status", HttpMethod.PUT, request, String.class, deliveryId);

        assertThat(response.getStatusCode()).as("Delivery status update: " + status).isEqualTo(HttpStatus.OK);

        log.info("Delivery status updated: deliveryId={}, status={}", deliveryId, status);
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
                        log.debug("Received payment.completed event: orderId={}", orderId);

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
                        log.debug("Received delivery.completed event: orderId={}", orderId);

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

        OrderStatus lastStatus = null;

        while (System.currentTimeMillis() < timeout) {
            Order order = orderRepository.findById(orderId).orElse(null);

            if (order != null) {
                if (lastStatus != order.getStatus()) {
                    log.info("E2E order status changed: "
                                    + "orderId={}, status={}",
                            orderId, order.getStatus());

                    lastStatus = order.getStatus();
                }

                if (order.getStatus() == expectedStatus) {
                    log.info("E2E order reached expected status: "
                                    + "orderId={}, status={}",
                            orderId, expectedStatus);

                    return order;
                }
            }

            Thread.sleep(500);
        }

        Order finalOrder = orderRepository.findById(orderId).orElse(null);

        assertThat(finalOrder).as("Order must exist after E2E processing").isNotNull();

        log.error("E2E order did not reach expected status: "
                        + "orderId={}, expectedStatus={}, actualStatus={}",
                orderId, expectedStatus, finalOrder.getStatus());

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
    // ENVIRONMENT VARIABLES
    // =========================================================

    private String getRequiredEnvironmentVariable(String name) {
        String value = System.getenv(name);

        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Required environment variable is missing: " + name);
        }

        return value;
    }

    // =========================================================
    // TEST EVENTS
    // =========================================================

    private record PaymentCompletedEvent(Long paymentId, Long orderId) {}

    private record DeliveryCompletedEvent(Long deliveryId, Long orderId) {}
}