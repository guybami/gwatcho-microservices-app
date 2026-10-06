package com.gwatcho.orderservice.integration;

import com.gwatcho.orderservice.dto.CheckoutItemRequest;
import com.gwatcho.orderservice.dto.CheckoutRequest;
import com.gwatcho.orderservice.dto.DeliveryAddressRequest;
import com.gwatcho.orderservice.dto.OrderResponse;
import com.gwatcho.orderservice.dto.ProductSnapshot;
import com.gwatcho.orderservice.entity.Order;
import com.gwatcho.orderservice.entity.OrderStatus;
import com.gwatcho.orderservice.repository.OrderRepository;

import com.gwatcho.orderservice.service.OrderService;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class OrderCheckoutIntegrationTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderRepository orderRepository;

    private List<ProductSnapshot> createProducts() {

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

    // =========================================================
    // CHECKOUT
    // =========================================================
    @Test
    @Transactional
    void shouldCreateOrderFromCheckout() {

        // -----------------------------------------------------
        // Checkout request
        // -----------------------------------------------------

        CheckoutRequest request =
                new CheckoutRequest(
                        100L,
                        "EUR",
                        "CARD",
                        new DeliveryAddressRequest(
                                "Main Street 10",
                                "74172",
                                "Neckarsulm",
                                "DE"
                        ),
                        List.of(
                                new CheckoutItemRequest(1L, 2),
                                new CheckoutItemRequest(2L, 1)
                        )
                );

        // -----------------------------------------------------
        // Product snapshots
        //
        // Normally these would come from the checkout/orchestration
        // layer after obtaining product information.
        // -----------------------------------------------------

        ProductSnapshot laptop =
                new ProductSnapshot(
                        1L,
                        "LAPTOP-001",
                        "Laptop",
                        new BigDecimal("1200.00"),
                        "EUR",
                        20
                );

        ProductSnapshot mouse =
                new ProductSnapshot(
                        2L,
                        "MOUSE-001",
                        "Mouse",
                        new BigDecimal("25.00"),
                        "EUR",
                        50
                );

        List<ProductSnapshot> products =
                List.of(
                        laptop,
                        mouse
                );

        // -----------------------------------------------------
        // Execute checkout
        // -----------------------------------------------------

        OrderResponse response =
                orderService.createOrder(
                        request,
                        products
                );

        // -----------------------------------------------------
        // Verify response
        // -----------------------------------------------------

        assertThat(response)
                .isNotNull();

        assertThat(response.id())
                .isNotNull();

        assertThat(response.customerId())
                .isEqualTo(100L);

        assertThat(response.status())
                .isEqualTo(OrderStatus.CREATED);

        assertThat(response.currency())
                .isEqualTo("EUR");


        /*
         * 2 × 1200 + 1 × 25
         *
         * = 2425 EUR
         */
        assertThat(response.totalAmount())
                .isEqualByComparingTo("2425.00");

        // -----------------------------------------------------
        // Verify delivery address
        // -----------------------------------------------------

        assertThat(response.street())
                .isEqualTo("Main Street 10");

        assertThat(response.postalCode())
                .isEqualTo("74172");

        assertThat(response.city())
                .isEqualTo("Neckarsulm");

        assertThat(response.country())
                .isEqualTo("DE");

        // -----------------------------------------------------
        // Verify order items
        // -----------------------------------------------------

        assertThat(response.items())
                .hasSize(2);

        // Laptop

        var laptopItem =
                response.items().get(0);

        assertThat(laptopItem.productId())
                .isEqualTo(1L);

        assertThat(laptopItem.sku())
                .isEqualTo("LAPTOP-001");

        assertThat(laptopItem.productName())
                .isEqualTo("Laptop");

        assertThat(laptopItem.unitPrice())
                .isEqualByComparingTo("1200.00");

        assertThat(laptopItem.quantity())
                .isEqualTo(2);

        assertThat(laptopItem.lineTotal())
                .isEqualByComparingTo("2400.00");

        // Mouse

        var mouseItem =
                response.items().get(1);

        assertThat(mouseItem.productId())
                .isEqualTo(2L);

        assertThat(mouseItem.sku())
                .isEqualTo("MOUSE-001");

        assertThat(mouseItem.productName())
                .isEqualTo("Mouse");

        assertThat(mouseItem.unitPrice())
                .isEqualByComparingTo("25.00");

        assertThat(mouseItem.quantity())
                .isEqualTo(1);

        assertThat(mouseItem.lineTotal())
                .isEqualByComparingTo("25.00");

        // -----------------------------------------------------
        // Verify persistence
        // -----------------------------------------------------

        Order persisted =
                orderRepository
                        .findById(response.id())
                        .orElseThrow();

        assertThat(persisted.getId())
                .isEqualTo(response.id());

        assertThat(persisted.getCustomerId())
                .isEqualTo(100L);

        assertThat(persisted.getStatus())
                .isEqualTo(OrderStatus.CREATED);

        assertThat(persisted.getCurrency())
                .isEqualTo("EUR");

        assertThat(persisted.getTotalAmount())
                .isEqualByComparingTo("2425.00");

        // -----------------------------------------------------
        // Verify persisted items
        // -----------------------------------------------------

        assertThat(persisted.getItems()).hasSize(2);

        Order persistedOrder =
                persisted;

        var persistedLaptop =
                persistedOrder.getItems()
                        .stream()
                        .filter(item ->
                                item.getProductId()
                                        .equals(1L)
                        )
                        .findFirst()
                        .orElseThrow();

        assertThat(persistedLaptop.getSku())
                .isEqualTo("LAPTOP-001");

        assertThat(persistedLaptop.getProductName())
                .isEqualTo("Laptop");

        assertThat(persistedLaptop.getUnitPrice())
                .isEqualByComparingTo("1200.00");

        assertThat(persistedLaptop.getQuantity())
                .isEqualTo(2);

        assertThat(persistedLaptop.getLineTotal())
                .isEqualByComparingTo("2400.00");

        var persistedMouse =
                persistedOrder.getItems()
                        .stream()
                        .filter(item ->
                                item.getProductId()
                                        .equals(2L)
                        )
                        .findFirst()
                        .orElseThrow();

        assertThat(persistedMouse.getSku())
                .isEqualTo("MOUSE-001");

        assertThat(persistedMouse.getProductName())
                .isEqualTo("Mouse");

        assertThat(persistedMouse.getUnitPrice())
                .isEqualByComparingTo("25.00");

        assertThat(persistedMouse.getQuantity())
                .isEqualTo(1);

        assertThat(persistedMouse.getLineTotal())
                .isEqualByComparingTo("25.00");
    }

    // =========================================================
    // PRODUCT CURRENCY VALIDATION
    // =========================================================

    @Test
    void shouldRejectProductsWithDifferentCurrencies() {

        CheckoutRequest request =
                new CheckoutRequest(
                        100L,
                        "EUR",
                        "CARD",
                        new DeliveryAddressRequest(
                                "Main Street 10",
                                "74172",
                                "Neckarsulm",
                                "DE"
                        ),
                        List.of(
                                new CheckoutItemRequest(
                                        1L,
                                        1
                                ),
                                new CheckoutItemRequest(
                                        2L,
                                        1
                                )
                        )
                );

        ProductSnapshot eurProduct =
                new ProductSnapshot(
                        1L,
                        "EUR-001",
                        "EUR Product",
                        new BigDecimal("100.00"),
                        "EUR",
                        10
                );

        ProductSnapshot chfProduct =
                new ProductSnapshot(
                        2L,
                        "CHF-001",
                        "CHF Product",
                        new BigDecimal("100.00"),
                        "CHF",
                        10
                );

        assertThat(
                org.assertj.core.api.Assertions
                        .catchThrowable(() ->
                                orderService.createOrder(
                                        request,
                                        List.of(
                                                eurProduct,
                                                chfProduct
                                        )
                                )
                        )
        )
                .isInstanceOf(
                        IllegalArgumentException.class
                )
                .hasMessageContaining(
                        "same currency"
                );
    }

    // =========================================================
    // MISSING CURRENCY
    // =========================================================

    @Test
    void shouldRejectMissingProductCurrency() {

        CheckoutRequest request =
                new CheckoutRequest(
                        100L,
                        "EUR",
                        "CARD",
                        new DeliveryAddressRequest(
                                "Main Street 10",
                                "74172",
                                "Neckarsulm",
                                "DE"
                        ),
                        List.of(
                                new CheckoutItemRequest(
                                        1L,
                                        1
                                )
                        )

                );

        ProductSnapshot product =
                new ProductSnapshot(
                        1L,
                        "TEST-001",
                        "Test Product",
                        new BigDecimal("100.00"),
                        null,
                        10
                );

        assertThat(
                org.assertj.core.api.Assertions
                        .catchThrowable(() ->
                                orderService.createOrder(
                                        request,
                                        List.of(product)
                                )
                        )
        )
                .isInstanceOf(
                        IllegalStateException.class
                )
                .hasMessageContaining(
                        "Product currency is missing"
                );
    }

    // =========================================================
    // PRODUCT MISMATCH
    // =========================================================

    @Test
    void shouldRejectProductSnapshotMismatch() {

        CheckoutRequest request =
                new CheckoutRequest(
                        100L,
                        "EUR",
                        "CARD",
                        new DeliveryAddressRequest(
                                "Main Street 10",
                                "74172",
                                "Neckarsulm",
                                "DE"
                        ),
                        List.of(
                                new CheckoutItemRequest(
                                        1L,
                                        1
                                )
                        )
                );

        ProductSnapshot wrongProduct =
                new ProductSnapshot(
                        999L,
                        "WRONG-001",
                        "Wrong Product",
                        new BigDecimal("100.00"),
                        "EUR",
                        10
                );

        assertThat(
                org.assertj.core.api.Assertions
                        .catchThrowable(() ->
                                orderService.createOrder(
                                        request,
                                        List.of(wrongProduct)
                                )
                        )
        )
                .isInstanceOf(
                        IllegalArgumentException.class
                )
                .hasMessageContaining(
                        "Product mismatch"
                );
    }

    // =========================================================
    // INSUFFICIENT STOCK
    // =========================================================

    @Test
    void shouldRejectInsufficientStock() {

        CheckoutRequest request =
                new CheckoutRequest(
                        100L,
                        "EUR",
                        "CARD",
                        new DeliveryAddressRequest(
                                "Main Street 10",
                                "74172",
                                "Neckarsulm",
                                "DE"
                        ),
                        List.of(
                                new CheckoutItemRequest(
                                        1L,
                                        10
                                )
                        )

                );

        ProductSnapshot product =
                new ProductSnapshot(
                        1L,
                        "TEST-001",
                        "Test Product",
                        new BigDecimal("100.00"),
                        "EUR",
                        5
                );

        assertThat(
                org.assertj.core.api.Assertions
                        .catchThrowable(() ->
                                orderService.createOrder(
                                        request,
                                        List.of(product)
                                )
                        )
        )
                .isInstanceOf(
                        IllegalStateException.class
                )
                .hasMessageContaining(
                        "Insufficient stock"
                );
    }
}