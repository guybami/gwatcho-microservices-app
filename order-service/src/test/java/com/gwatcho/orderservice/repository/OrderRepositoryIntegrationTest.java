package com.gwatcho.orderservice.repository;

import com.gwatcho.orderservice.entity.DeliveryAddress;
import com.gwatcho.orderservice.entity.Order;
import com.gwatcho.orderservice.entity.OrderItem;
import com.gwatcho.orderservice.entity.OrderStatus;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class OrderRepositoryIntegrationTest {

    @Autowired
    private OrderRepository orderRepository;

    @Test
    void shouldSaveOrderWithItems() {

        Order order =
                Order.builder()
                        .customerId(100L)
                        .status(OrderStatus.CREATED)
                        .currency("EUR")
                        .paymentMethod("CARD")
                        .totalAmount(
                                new BigDecimal("125.00")
                        )
                        .deliveryAddress(
                                DeliveryAddress.builder()
                                        .street("Street 1")
                                        .postalCode("74172")
                                        .city("Neckarsulm")
                                        .country("DE")
                                        .build()
                        )
                        .build();

        OrderItem item =
                OrderItem.builder()
                        .productId(1L)
                        .sku("TEST-001")
                        .productName("Test Product")
                        .unitPrice(
                                new BigDecimal("125.00")
                        )
                        .quantity(1)
                        .lineTotal(
                                new BigDecimal("125.00")
                        )
                        .build();

        order.addItem(item);

        Order saved =
                orderRepository.saveAndFlush(order);

        assertThat(saved.getId())
                .isNotNull();

        assertThat(saved.getItems())
                .hasSize(1);

        assertThat(saved.getItems().get(0).getSku())
                .isEqualTo("TEST-001");

        assertThat(saved.getItems().get(0).getUnitPrice())
                .isEqualByComparingTo("125.00");
    }

    @Test
    void shouldFindOrdersByCustomerId() {

        Order order =
                Order.builder()
                        .customerId(200L)
                        .status(OrderStatus.CREATED)
                        .currency("EUR")
                        .paymentMethod("CARD")
                        .totalAmount(
                                new BigDecimal("50.00")
                        )
                        .deliveryAddress(
                                DeliveryAddress.builder()
                                        .street("Street 2")
                                        .postalCode("74172")
                                        .city("Neckarsulm")
                                        .country("DE")
                                        .build()
                        )
                        .build();

        orderRepository.saveAndFlush(order);

        var orders =
                orderRepository.findByCustomerId(200L);

        assertThat(orders)
                .hasSize(1);

        assertThat(orders.get(0).getCustomerId())
                .isEqualTo(200L);
    }
}