package com.gwatcho.orderservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import com.gwatcho.orderservice.dto.*;
import com.gwatcho.orderservice.entity.DeliveryAddress;
import com.gwatcho.orderservice.entity.Order;
import com.gwatcho.orderservice.entity.OrderItem;
import com.gwatcho.orderservice.entity.OrderOutboxEvent;
import com.gwatcho.orderservice.entity.OrderStatus;
import com.gwatcho.orderservice.repository.OrderOutboxEventRepository;
import com.gwatcho.orderservice.repository.OrderRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderOutboxEventRepository outboxEventRepository;

    private ObjectMapper objectMapper;

    private OrderService orderService;

    // =========================================================
    // SETUP
    // =========================================================

    @BeforeEach
    void setUp() {

        objectMapper = new ObjectMapper();

        objectMapper.registerModule(
                new JavaTimeModule()
        );

        orderService =
                new OrderService(
                        orderRepository,
                        outboxEventRepository,
                        objectMapper,
                        "order.created"
                );
    }

    // =========================================================
    // CREATE ORDER
    // =========================================================

    @Test
    void shouldCreateOrderFromCheckout() {

        CheckoutRequest request =
                createCheckoutRequest();

        List<ProductSnapshot> products =
                createProducts();

        when(orderRepository.save(any(Order.class)))
                .thenAnswer(invocation -> {

                    Order order =
                            invocation.getArgument(0);

                    order.setId(1L);

                    return order;
                });

        when(outboxEventRepository.save(
                any(OrderOutboxEvent.class)))
                .thenAnswer(invocation ->
                        invocation.getArgument(0));

        OrderResponse response =
                orderService.createOrder(
                        request,
                        products
                );

        // =====================================================
        // ORDER
        // =====================================================

        assertThat(response).isNotNull();

        assertThat(response.id())
                .isEqualTo(1L);

        assertThat(response.customerId())
                .isEqualTo(100L);

        assertThat(response.status())
                .isEqualTo(OrderStatus.CREATED);

        assertThat(response.currency())
                .isEqualTo("EUR");

        assertThat(response.totalAmount())
                .isEqualByComparingTo(
                        new BigDecimal("2693.28")
                );

        assertThat(response.items())
                .hasSize(2);

        // =====================================================
        // ORDER ITEM 1
        // =====================================================

        assertThat(response.items().get(0).productId())
                .isEqualTo(1L);

        assertThat(response.items().get(0).sku())
                .isEqualTo("OUTBOX-001");

        assertThat(response.items().get(0).productName())
                .isEqualTo("Outbox Test Product");

        assertThat(response.items().get(0).unitPrice())
                .isEqualByComparingTo(
                        new BigDecimal("99.99")
                );

        assertThat(response.items().get(0).quantity())
                .isEqualTo(2);

        assertThat(response.items().get(0).lineTotal())
                .isEqualByComparingTo(
                        new BigDecimal("199.98")
                );

        // =====================================================
        // ORDER ITEM 2
        // =====================================================

        assertThat(response.items().get(1).productId())
                .isEqualTo(25L);

        assertThat(response.items().get(1).sku())
                .isEqualTo("SKU-023");

        assertThat(response.items().get(1).productName())
                .isEqualTo(
                        "Smart Accessories Product 023"
                );

        assertThat(response.items().get(1).unitPrice())
                .isEqualByComparingTo(
                        new BigDecimal("831.10")
                );

        assertThat(response.items().get(1).quantity())
                .isEqualTo(3);

        assertThat(response.items().get(1).lineTotal())
                .isEqualByComparingTo(
                        new BigDecimal("2493.30")
                );

        // =====================================================
        // OUTBOX
        // =====================================================

        verify(orderRepository)
                .save(any(Order.class));

        verify(outboxEventRepository)
                .save(any(OrderOutboxEvent.class));
    }

    // =========================================================
    // PRODUCT SNAPSHOT
    // =========================================================

    @Test
    void shouldStoreProductSnapshotInOrder() {

        CheckoutRequest request =
                createCheckoutRequest();

        List<ProductSnapshot> products =
                createProducts();

        when(orderRepository.save(any(Order.class)))
                .thenAnswer(invocation -> {

                    Order order =
                            invocation.getArgument(0);

                    order.setId(1L);

                    return order;
                });

        when(outboxEventRepository.save(
                any(OrderOutboxEvent.class)))
                .thenAnswer(invocation ->
                        invocation.getArgument(0));

        OrderResponse response =
                orderService.createOrder(
                        request,
                        products
                );

        assertThat(response.items())
                .hasSize(2);

        assertThat(response.items().get(0).productId())
                .isEqualTo(1L);

        assertThat(response.items().get(0).sku())
                .isEqualTo("OUTBOX-001");

        assertThat(response.items().get(0).productName())
                .isEqualTo("Outbox Test Product");

        assertThat(response.items().get(0).unitPrice())
                .isEqualByComparingTo(
                        new BigDecimal("99.99")
                );

        assertThat(response.items().get(1).productId())
                .isEqualTo(25L);

        assertThat(response.items().get(1).sku())
                .isEqualTo("SKU-023");

        assertThat(response.items().get(1).productName())
                .isEqualTo(
                        "Smart Accessories Product 023"
                );

        assertThat(response.items().get(1).unitPrice())
                .isEqualByComparingTo(
                        new BigDecimal("831.10")
                );
    }

    // =========================================================
    // OUTBOX EVENT
    // =========================================================

    @Test
    void shouldCreateOrderCreatedOutboxEvent() {

        CheckoutRequest request =
                createCheckoutRequest();

        List<ProductSnapshot> products =
                createProducts();

        when(orderRepository.save(any(Order.class)))
                .thenAnswer(invocation -> {

                    Order order =
                            invocation.getArgument(0);

                    order.setId(1L);

                    return order;
                });

        ArgumentCaptor<OrderOutboxEvent> captor =
                ArgumentCaptor.forClass(
                        OrderOutboxEvent.class
                );

        when(outboxEventRepository.save(
                any(OrderOutboxEvent.class)))
                .thenAnswer(invocation ->
                        invocation.getArgument(0));

        orderService.createOrder(
                request,
                products
        );

        verify(outboxEventRepository)
                .save(captor.capture());

        OrderOutboxEvent event =
                captor.getValue();

        assertThat(event.getEventId())
                .isNotNull();

        assertThat(event.getEventId())
                .isNotBlank();

        assertThat(event.getAggregateId())
                .isEqualTo(1L);

        assertThat(event.getAggregateType())
                .isEqualTo("Order");

        assertThat(event.getEventType())
                .isEqualTo("OrderCreated");

        assertThat(event.getTopic())
                .isEqualTo("order.created");

        assertThat(event.getStatus())
                .isNotNull();

        assertThat(event.getPayload())
                .isNotNull();

        assertThat(event.getPayload())
                .isNotBlank();
    }

    // =========================================================
    // OUTBOX PAYLOAD
    // =========================================================

    @Test
    void shouldCreateValidOrderCreatedEventPayload() throws Exception {

        CheckoutRequest request =
                createCheckoutRequest();

        List<ProductSnapshot> products =
                createProducts();

        when(orderRepository.save(any(Order.class)))
                .thenAnswer(invocation -> {

                    Order order =
                            invocation.getArgument(0);

                    order.setId(1L);

                    return order;
                });

        ArgumentCaptor<OrderOutboxEvent> captor =
                ArgumentCaptor.forClass(
                        OrderOutboxEvent.class
                );

        when(outboxEventRepository.save(
                any(OrderOutboxEvent.class)))
                .thenAnswer(invocation ->
                        invocation.getArgument(0));

        orderService.createOrder(
                request,
                products
        );

        verify(outboxEventRepository)
                .save(captor.capture());

        OrderOutboxEvent event =
                captor.getValue();

        var json =
                objectMapper.readTree(
                        event.getPayload()
                );

        assertThat(
                json.get("eventId").asText()
        )
                .isEqualTo(
                        event.getEventId()
                );

        assertThat(
                json.get("eventType").asText()
        )
                .isEqualTo("OrderCreated");

        assertThat(
                json.get("orderId").asLong()
        )
                .isEqualTo(1L);

        assertThat(
                json.get("customerId").asLong()
        )
                .isEqualTo(100L);

        assertThat(
                json.get("totalAmount").decimalValue()
        )
                .isEqualByComparingTo(
                        new BigDecimal("2693.28")
                );

        assertThat(
                json.get("currency").asText()
        )
                .isEqualTo("EUR");

        assertThat(
                json.get("items").size()
        )
                .isEqualTo(2);
    }

    // =========================================================
    // VALIDATION
    // =========================================================

    @Test
    void shouldRejectNullCheckoutRequest() {

        assertThatThrownBy(() ->
                orderService.createOrder(
                        null,
                        createProducts()
                )
        )
                .isInstanceOf(
                        IllegalArgumentException.class
                )
                .hasMessage(
                        "Checkout request must not be null"
                );
    }

    @Test
    void shouldRejectEmptyProducts() {

        CheckoutRequest request =
                createCheckoutRequest();

        assertThatThrownBy(() ->
                orderService.createOrder(
                        request,
                        List.of()
                )
        )
                .isInstanceOf(
                        IllegalArgumentException.class
                )
                .hasMessage(
                        "Products must not be empty"
                );
    }

    @Test
    void shouldRejectNullProducts() {

        CheckoutRequest request =
                createCheckoutRequest();

        assertThatThrownBy(() ->
                orderService.createOrder(
                        request,
                        null
                )
        )
                .isInstanceOf(
                        IllegalArgumentException.class
                )
                .hasMessage(
                        "Products must not be empty"
                );
    }

    @Test
    void shouldRejectEmptyCheckoutItems() {

        CheckoutRequest request =
                new CheckoutRequest(
                        100L,
                        "EUR",
                        "CARD",
                        createDeliveryAddressRequest(),
                        List.of()
                );

        assertThatThrownBy(() ->
                orderService.createOrder(
                        request,
                        createProducts()
                )
        )
                .isInstanceOf(
                        IllegalArgumentException.class
                )
                .hasMessage(
                        "Checkout must contain at least one item"
                );
    }

    @Test
    void shouldRejectProductCountMismatch() {

        CheckoutRequest request =
                createCheckoutRequest();

        List<ProductSnapshot> products =
                List.of(
                        createProducts().get(0)
                );

        assertThatThrownBy(() ->
                orderService.createOrder(
                        request,
                        products
                )
        )
                .isInstanceOf(
                        IllegalArgumentException.class
                )
                .hasMessage(
                        "Product information does not match checkout items"
                );
    }

    @Test
    void shouldRejectProductMismatch() {

        CheckoutRequest request =
                createCheckoutRequest();

        ProductSnapshot wrongProduct =
                new ProductSnapshot(
                        999L,
                        "WRONG-SKU",
                        "Wrong Product",
                        new BigDecimal("10.00"),
                        "EUR",
                        10
                );

        assertThatThrownBy(() ->
                orderService.createOrder(
                        request,
                        List.of(
                                wrongProduct,
                                createProducts().get(1)
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

    @Test
    void shouldRejectNullProductSnapshot() {

        CheckoutRequest request =
                createCheckoutRequest();

        List<ProductSnapshot> products =
                Arrays.asList(
                        null,
                        createProducts().get(1)
                );

        assertThatThrownBy(() ->
                orderService.createOrder(
                        request,
                        products
                )
        )
                .isInstanceOf(
                        IllegalArgumentException.class
                )
                .hasMessage(
                        "Product information must not be null"
                );
    }

    @Test
    void shouldRejectMissingProductCurrency() {

        CheckoutRequest request =
                createCheckoutRequest();

        ProductSnapshot product =
                new ProductSnapshot(
                        1L,
                        "TEST",
                        "Test Product",
                        new BigDecimal("10.00"),
                        null,
                        10
                );

        assertThatThrownBy(() ->
                orderService.createOrder(
                        request,
                        List.of(
                                product,
                                createProducts().get(1)
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

    @Test
    void shouldRejectMissingProductPrice() {

        CheckoutRequest request =
                createCheckoutRequest();

        ProductSnapshot product =
                new ProductSnapshot(
                        1L,
                        "TEST",
                        "Test Product",
                        null,
                        "EUR",
                        10
                );

        assertThatThrownBy(() ->
                orderService.createOrder(
                        request,
                        List.of(
                                product,
                                createProducts().get(1)
                        )
                )
        )
                .isInstanceOf(
                        IllegalStateException.class
                )
                .hasMessageContaining(
                        "Product price is missing"
                );
    }

    @Test
    void shouldRejectInvalidQuantity() {

        CheckoutRequest request =
                new CheckoutRequest(
                        100L,
                        "EUR",
                        "CARD",
                        createDeliveryAddressRequest(),
                        List.of(
                                new CheckoutItemRequest(
                                        1L,
                                        0
                                ),
                                new CheckoutItemRequest(
                                        25L,
                                        3
                                )
                        )
                );

        assertThatThrownBy(() ->
                orderService.createOrder(
                        request,
                        createProducts()
                )
        )
                .isInstanceOf(
                        IllegalArgumentException.class
                )
                .hasMessage(
                        "Quantity must be greater than zero"
                );
    }

    @Test
    void shouldRejectInsufficientStock() {

        CheckoutRequest request =
                new CheckoutRequest(
                        100L,
                        "EUR",
                        "CARD",
                        createDeliveryAddressRequest(),
                        List.of(
                                new CheckoutItemRequest(
                                        1L,
                                        20
                                ),
                                new CheckoutItemRequest(
                                        25L,
                                        3
                                )
                        )
                );

        assertThatThrownBy(() ->
                orderService.createOrder(
                        request,
                        createProducts()
                )
        )
                .isInstanceOf(
                        IllegalStateException.class
                )
                .hasMessageContaining(
                        "Insufficient stock"
                );
    }

    @Test
    void shouldRejectDifferentCurrencies() {

        CheckoutRequest request =
                createCheckoutRequest();

        ProductSnapshot euroProduct = createProducts().get(0);

        ProductSnapshot usdProduct =
                new ProductSnapshot(
                        25L,
                        "USD-001",
                        "USD Product",
                        new BigDecimal("100.00"),
                        "USD",
                        10
                );

        assertThatThrownBy(() ->
                orderService.createOrder(
                        request,
                        List.of(
                                euroProduct,
                                usdProduct
                        )
                )
        )
                .isInstanceOf(
                        IllegalArgumentException.class
                )
                .hasMessage(
                        "All products must use the same currency"
                );
    }

    // =========================================================
    // GET ORDER
    // =========================================================

    @Test
    void shouldGetOrder() {

        Order order =
                createOrderEntity();

        when(orderRepository.findById(1L))
                .thenReturn(
                        java.util.Optional.of(order)
                );

        OrderResponse response =
                orderService.getOrder(1L);

        assertThat(response).isNotNull();

        assertThat(response.id())
                .isEqualTo(1L);

        assertThat(response.customerId())
                .isEqualTo(100L);

        assertThat(response.items())
                .hasSize(2);
    }

    @Test
    void shouldRejectGetOrderWhenNotFound() {

        when(orderRepository.findById(999L))
                .thenReturn(
                        java.util.Optional.empty()
                );

        assertThatThrownBy(() ->
                orderService.getOrder(999L)
        )
                .isInstanceOf(
                        RuntimeException.class
                )
                .hasMessage(
                        "Order not found: 999"
                );
    }

    // =========================================================
    // GET ALL ORDERS
    // =========================================================

    @Test
    void shouldGetAllOrders() {

        Order order1 =
                createOrderEntity();

        Order order2 =
                createOrderEntity();

        order2.setId(2L);

        when(orderRepository.findAll())
                .thenReturn(
                        List.of(
                                order1,
                                order2
                        )
                );

        List<OrderResponse> responses =
                orderService.getOrders();

        assertThat(responses)
                .hasSize(2);

        assertThat(responses.get(0).id())
                .isEqualTo(1L);

        assertThat(responses.get(1).id())
                .isEqualTo(2L);
    }

    // =========================================================
    // CUSTOMER ORDERS
    // =========================================================

    @Test
    void shouldGetCustomerOrders() {

        Order order =
                createOrderEntity();

        when(orderRepository.findByCustomerId(100L))
                .thenReturn(
                        List.of(order)
                );

        List<OrderResponse> responses =
                orderService.getCustomerOrders(100L);

        assertThat(responses)
                .hasSize(1);

        assertThat(responses.get(0).customerId())
                .isEqualTo(100L);
    }

    // =========================================================
    // CANCEL ORDER
    // =========================================================

    @Test
    void shouldCancelOrder() {

        Order order =
                createOrderEntity();

        order.setStatus(
                OrderStatus.CREATED
        );

        when(orderRepository.findById(1L))
                .thenReturn(
                        java.util.Optional.of(order)
                );

        when(orderRepository.save(any(Order.class)))
                .thenAnswer(invocation ->
                        invocation.getArgument(0)
                );

        OrderResponse response =
                orderService.cancelOrder(1L);

        assertThat(response.status())
                .isEqualTo(
                        OrderStatus.CANCELLED
                );

        verify(orderRepository)
                .save(any(Order.class));
    }

    @Test
    void shouldNotCancelPaidOrder() {

        Order order =
                createOrderEntity();

        order.setStatus(
                OrderStatus.PAID
        );

        when(orderRepository.findById(1L))
                .thenReturn(
                        java.util.Optional.of(order)
                );

        assertThatThrownBy(() ->
                orderService.cancelOrder(1L)
        )
                .isInstanceOf(
                        IllegalStateException.class
                )
                .hasMessage(
                        "Order cannot be cancelled"
                );
    }

    @Test
    void shouldNotCancelCompletedOrder() {

        Order order =
                createOrderEntity();

        order.setStatus(
                OrderStatus.COMPLETED
        );

        when(orderRepository.findById(1L))
                .thenReturn(
                        java.util.Optional.of(order)
                );

        assertThatThrownBy(() ->
                orderService.cancelOrder(1L)
        )
                .isInstanceOf(
                        IllegalStateException.class
                )
                .hasMessage(
                        "Order cannot be cancelled"
                );
    }

    // =========================================================
    // TEST DATA
    // =========================================================

    private CheckoutRequest createCheckoutRequest() {

        return new CheckoutRequest(
                100L,
                "EUR",
                "CARD",
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

    private DeliveryAddressRequest createDeliveryAddressRequest() {

        return new DeliveryAddressRequest(
                "Main Street 10",
                "74172",
                "Neckarsulm",
                "DE"
        );
    }

    private DeliveryAddress createAddress() {

        return DeliveryAddress.builder()
                .street("Main Street 10")
                .postalCode("74172")
                .city("Neckarsulm")
                .country("DE")
                .build();
    }

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

    private Order createOrderEntity() {

        Order order =
                Order.builder()
                        .id(1L)
                        .customerId(100L)
                        .currency("EUR")
                        .paymentMethod("CARD")
                        .status(OrderStatus.CREATED)
                        .deliveryAddress(
                                createAddress()
                        )
                        .build();

        OrderItem item1 =
                OrderItem.builder()
                        .productId(1L)
                        .sku("OUTBOX-001")
                        .productName(
                                "Outbox Test Product"
                        )
                        .unitPrice(
                                new BigDecimal("99.99")
                        )
                        .quantity(2)
                        .lineTotal(
                                new BigDecimal("199.98")
                        )
                        .build();

        OrderItem item2 =
                OrderItem.builder()
                        .productId(25L)
                        .sku("SKU-023")
                        .productName(
                                "Smart Accessories Product 023"
                        )
                        .unitPrice(
                                new BigDecimal("831.10")
                        )
                        .quantity(3)
                        .lineTotal(
                                new BigDecimal("2493.30")
                        )
                        .build();

        order.addItem(item1);
        order.addItem(item2);

        order.calculateTotal();

        return order;
    }
}