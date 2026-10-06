package com.gwatcho.orderservice.service;

import com.gwatcho.orderservice.dto.CheckoutItemRequest;
import com.gwatcho.orderservice.dto.CheckoutRequest;
import com.gwatcho.orderservice.dto.OrderItemResponse;
import com.gwatcho.orderservice.dto.OrderResponse;
import com.gwatcho.orderservice.dto.ProductSnapshot;
import com.gwatcho.orderservice.entity.DeliveryAddress;
import com.gwatcho.orderservice.entity.Order;
import com.gwatcho.orderservice.entity.OrderItem;
import com.gwatcho.orderservice.entity.OrderStatus;
import com.gwatcho.orderservice.repository.OrderRepository;

import lombok.RequiredArgsConstructor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gwatcho.orderservice.entity.OrderOutboxEvent;
import com.gwatcho.orderservice.entity.OutboxStatus;
import com.gwatcho.orderservice.event.OrderCreatedEvent;
import com.gwatcho.orderservice.repository.OrderOutboxEventRepository;

import org.springframework.beans.factory.annotation.Value;

@Service
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderOutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;
    private final String orderCreatedTopic;

    public OrderService(
            OrderRepository orderRepository,
            OrderOutboxEventRepository outboxEventRepository,
            ObjectMapper objectMapper,
            @Value("${app.kafka.topics.order-created}")
            String orderCreatedTopic) {

        this.orderRepository = orderRepository;
        this.outboxEventRepository = outboxEventRepository;
        this.objectMapper = objectMapper;
        this.orderCreatedTopic = orderCreatedTopic;
    }

    // =========================================================
    // CHECKOUT / CREATE ORDER
    // =========================================================

    @Transactional
    public OrderResponse createOrder(
            CheckoutRequest request,
            List<ProductSnapshot> products) {

        if (request == null) {
            throw new IllegalArgumentException(
                    "Checkout request must not be null"
            );
        }

        if (products == null || products.isEmpty()) {
            throw new IllegalArgumentException(
                    "Products must not be empty"
            );
        }

        if (request.items() == null || request.items().isEmpty()) {

            throw new IllegalArgumentException(
                    "Checkout must contain at least one item"
            );
        }

        if (request.items().size() != products.size()) {

            throw new IllegalArgumentException(
                    "Product information does not match checkout items"
            );
        }

        // =====================================================
        // CREATE ORDER
        // =====================================================

        Order order =
                Order.builder()
                        .customerId(
                                request.customerId()
                        )
                        .status(
                                OrderStatus.CREATED
                        )
                        .paymentMethod(request.paymentMethod())
                        .deliveryAddress(
                                DeliveryAddress.builder()
                                        .street(
                                                request.deliveryAddress().street()
                                        )
                                        .postalCode(
                                                request.deliveryAddress().postalCode()
                                        )
                                        .city(
                                                request.deliveryAddress().city()
                                        )
                                        .country(
                                                request.deliveryAddress().country()
                                        )
                                        .build()
                        )
                        .build();

        String currency = null;

        // =====================================================
        // CREATE ORDER ITEMS
        // =====================================================

        for (int i = 0; i < request.items().size(); i++) {

            CheckoutItemRequest itemRequest = request.items().get(i);
            ProductSnapshot productSnapshotct = products.get(i);
            // -------------------------------------------------
            // Product validation
            // -------------------------------------------------
            if (productSnapshotct == null) {
                throw new IllegalArgumentException(
                        "Product information must not be null"
                );
            }

            if (productSnapshotct.productId() == null) {
                throw new IllegalArgumentException(
                        "Product ID must not be null"
                );
            }

            // -------------------------------------------------
            // Verify productSnapshotct matches checkout item
            // -------------------------------------------------

            if (!productSnapshotct.productId()
                    .equals(itemRequest.productId())) {

                throw new IllegalArgumentException(
                        "Product mismatch for checkout item: " +
                                itemRequest.productId()
                );
            }

            // -------------------------------------------------
            // Validate productSnapshotct currency
            // -------------------------------------------------

            String productCurrency = productSnapshotct.currency();
            if (productCurrency == null ||
                    productCurrency.isBlank()) {
                throw new IllegalStateException(
                        "Product currency is missing: " +
                                productSnapshotct.productId()
                );
            }

            // -------------------------------------------------
            // Validate productSnapshotct price
            // -------------------------------------------------

            if (productSnapshotct.unitPrice() == null) {

                throw new IllegalStateException(
                        "Product price is missing: " +
                                productSnapshotct.productId()
                );
            }

            // -------------------------------------------------
            // Validate quantity
            // -------------------------------------------------

            if (itemRequest.quantity() == null ||
                    itemRequest.quantity() <= 0) {

                throw new IllegalArgumentException(
                        "Quantity must be greater than zero"
                );
            }

            // -------------------------------------------------
            // Validate stock
            // -------------------------------------------------
            if (productSnapshotct.stockQuantity() == null) {

                throw new IllegalStateException(
                        "Product stock quantity is missing: " +
                                productSnapshotct.productId()
                );
            }

            if (productSnapshotct.stockQuantity() <
                    itemRequest.quantity()) {

                throw new IllegalStateException(
                        "Insufficient stock for productSnapshotct " +
                                productSnapshotct.productId()
                );
            }

            // -------------------------------------------------
            // Determine order currency
            // -------------------------------------------------

            if (currency == null) {
                currency = productCurrency;
            } else if (!currency.equals(productCurrency)) {

                throw new IllegalArgumentException(
                        "All products must use the same currency"
                );
            }

            // -------------------------------------------------
            // Calculate line total
            // -------------------------------------------------

            BigDecimal lineTotal =
                    productSnapshotct.unitPrice()
                            .multiply(
                                    BigDecimal.valueOf(
                                            itemRequest.quantity()
                                    )
                            );

            // -------------------------------------------------
            // Create OrderItem
            //
            // Product information is stored as a snapshot.
            // -------------------------------------------------

            OrderItem orderItem =
                    OrderItem.builder()
                            .productId(
                                    productSnapshotct.productId()
                            )
                            .sku(
                                    productSnapshotct.sku()
                            )
                            .productName(
                                    productSnapshotct.productName()
                            )
                            .unitPrice(
                                    productSnapshotct.unitPrice()
                            )
                            .quantity(
                                    itemRequest.quantity()
                            )
                            .lineTotal(
                                    lineTotal
                            )
                            .build();

            order.addItem(orderItem);
        }

        // =====================================================
        // SET ORDER TOTAL / CURRENCY
        // =====================================================

        if (currency == null ||
                currency.isBlank()) {

            throw new IllegalStateException(
                    "Order currency could not be determined"
            );
        }
        order.setCurrency(currency);
        order.calculateTotal();
        // =====================================================
        // SAVE
        // =====================================================
        Order saved = orderRepository.save(order);
        createOutboxEvent(saved);
        return toResponse(saved);
    }

    // =========================================================
    // GET ORDER
    // =========================================================
    @Transactional(readOnly = true)
    public OrderResponse getOrder(Long id) {
        return toResponse(
                findOrder(id)
        );
    }

    // =========================================================
    // GET ALL ORDERS
    // =========================================================
    @Transactional(readOnly = true)
    public List<OrderResponse> getOrders() {

        return orderRepository
                .findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    // =========================================================
    // GET CUSTOMER ORDERS
    // =========================================================
    @Transactional(readOnly = true)
    public List<OrderResponse> getCustomerOrders(
            Long customerId) {

        return orderRepository
                .findByCustomerId(customerId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    // =========================================================
    // CANCEL ORDER
    // =========================================================
    @Transactional
    public OrderResponse cancelOrder(Long id) {

        Order order = findOrder(id);

        if (order.getStatus() == OrderStatus.PAID ||
                order.getStatus() == OrderStatus.COMPLETED) {

            throw new IllegalStateException(
                    "Order cannot be cancelled"
            );
        }

        order.setStatus(
                OrderStatus.CANCELLED
        );

        Order saved = orderRepository.save(order);

        return toResponse(saved);
    }

    // =========================================================
    // FIND ORDER
    // =========================================================
    private Order findOrder(Long id) {
        return orderRepository
                .findById(id)
                .orElseThrow(
                        () -> new RuntimeException(
                                "Order not found: " + id
                        )
                );
    }

    // =========================================================
    // ENTITY -> RESPONSE
    // =========================================================
    private OrderResponse toResponse(Order order) {
        List<OrderItemResponse> items =
                order.getItems()
                        .stream()
                        .map(item ->
                                new OrderItemResponse(
                                        item.getProductId(),
                                        item.getSku(),
                                        item.getProductName(),
                                        item.getUnitPrice(),
                                        item.getQuantity(),
                                        item.getLineTotal()
                                )
                        )
                        .toList();

        DeliveryAddress address = order.getDeliveryAddress();
        return new OrderResponse(
                order.getId(),
                order.getCustomerId(),
                order.getStatus(),
                order.getTotalAmount(),
                order.getCurrency(),
                order.getPaymentMethod(),
                address.getStreet(),
                address.getPostalCode(),
                address.getCity(),
                address.getCountry(),
                items,
                order.getVersion(),
                order.getCreatedAt(),
                order.getUpdatedAt()
        );
    }

    private void createOutboxEvent(Order order) {

        String eventId = UUID.randomUUID().toString();
        OrderCreatedEvent event =
                new OrderCreatedEvent(
                        eventId,
                        "OrderCreated",
                        order.getId(),
                        order.getCustomerId(),
                        order.getStatus(),
                        order.getTotalAmount(),
                        order.getCurrency(),
                        order.getPaymentMethod(),
                        order.getDeliveryAddress().getStreet(),
                        order.getDeliveryAddress().getPostalCode(),
                        order.getDeliveryAddress().getCity(),
                        order.getDeliveryAddress().getCountry(),
                        order.getItems()
                                .stream()
                                .map(item ->
                                        new OrderItemResponse(
                                                item.getProductId(),
                                                item.getSku(),
                                                item.getProductName(),
                                                item.getUnitPrice(),
                                                item.getQuantity(),
                                                item.getLineTotal()
                                        )
                                )
                                .toList(),
                        order.getCreatedAt()
                );

        try {

            String payload = objectMapper.writeValueAsString(event);

            OrderOutboxEvent outboxEvent =
                    OrderOutboxEvent.builder()
                            .eventId(eventId)
                            .aggregateId(order.getId())
                            .aggregateType("Order")
                            .eventType("OrderCreated")
                            .topic(orderCreatedTopic)
                            .payload(objectMapper.writeValueAsString(event))
                            .status(OutboxStatus.PENDING)
                            .attempts(0)
                            .build();

            outboxEventRepository.save(outboxEvent);

        } catch (JsonProcessingException ex) {
            throw new IllegalStateException(
                    "Failed to serialize OrderCreatedEvent",
                    ex
            );
        }
    }

    @Transactional
    public Order completeOrder(Long orderId) {
        Order order =
                orderRepository.findById(orderId).orElseThrow(()
                        -> new IllegalArgumentException("Order not found: " + orderId));

        if (order.getStatus() == OrderStatus.COMPLETED) {
            log.info("Order already completed: orderId={}", orderId);
            return order;
        }
        order.setStatus(OrderStatus.COMPLETED);
        Order saved = orderRepository.save(order);
        log.info("Order completed successfully: orderId={}", orderId);
        return saved;
    }
}