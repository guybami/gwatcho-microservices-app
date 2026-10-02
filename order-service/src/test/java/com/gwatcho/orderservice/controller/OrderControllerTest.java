package com.gwatcho.orderservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gwatcho.orderservice.dto.*;
import com.gwatcho.orderservice.entity.OrderStatus;
import com.gwatcho.orderservice.service.CheckoutService;
import com.gwatcho.orderservice.service.OrderService;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import org.springframework.http.MediaType;

import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OrderController.class)
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private OrderService orderService;

    @MockBean
    private CheckoutService checkoutService;

    // =========================================================
    // CHECKOUT
    // =========================================================

    @Test
    void shouldCreateOrderFromCheckout() throws Exception {
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
                                new CheckoutItemRequest(25L, 3)
                        )
                );

        OrderResponse response = createOrderResponse();

        when(checkoutService.checkout(
                any(CheckoutRequest.class)
        ))
                .thenReturn(response);

        mockMvc.perform(
                        post("/orders/checkout")
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        objectMapper.writeValueAsString(
                                                request
                                        )
                                )
                )
                .andExpect(status().isCreated())

                .andExpect(
                        jsonPath("$.id")
                                .value(1)
                )
                .andExpect(
                        jsonPath("$.customerId")
                                .value(100)
                )
                .andExpect(
                        jsonPath("$.status")
                                .value("CREATED")
                )
                .andExpect(
                        jsonPath("$.currency")
                                .value("EUR")
                )
                .andExpect(
                        jsonPath("$.totalAmount")
                                .value(2425.00)
                )
                .andExpect(
                        jsonPath("$.items.length()")
                                .value(2)
                )

                .andExpect(
                        jsonPath("$.items[0].productId")
                                .value(1)
                )
                .andExpect(
                        jsonPath("$.items[0].sku")
                                .value("LAPTOP-001")
                )
                .andExpect(
                        jsonPath("$.items[0].productName")
                                .value("Laptop")
                )
                .andExpect(
                        jsonPath("$.items[0].unitPrice")
                                .value(1200.00)
                )
                .andExpect(
                        jsonPath("$.items[0].quantity")
                                .value(2)
                )
                .andExpect(
                        jsonPath("$.items[0].lineTotal")
                                .value(2400.00)
                )

                .andExpect(
                        jsonPath("$.items[1].productId")
                                .value(2)
                )
                .andExpect(
                        jsonPath("$.items[1].sku")
                                .value("MOUSE-001")
                )
                .andExpect(
                        jsonPath("$.items[1].productName")
                                .value("Mouse")
                )
                .andExpect(
                        jsonPath("$.items[1].unitPrice")
                                .value(25.00)
                )
                .andExpect(
                        jsonPath("$.items[1].quantity")
                                .value(1)
                )
                .andExpect(
                        jsonPath("$.items[1].lineTotal")
                                .value(25.00)
                );

        verify(checkoutService)
                .checkout(
                        any(CheckoutRequest.class)
                );
    }

    // =========================================================
    // GET ORDER
    // =========================================================

    @Test
    void shouldGetOrder()
            throws Exception {

        when(orderService.getOrder(1L))
                .thenReturn(
                        createOrderResponse()
                );

        mockMvc.perform(
                        get("/orders/1")
                                .accept(
                                        MediaType.APPLICATION_JSON
                                )
                )
                .andExpect(status().isOk())

                .andExpect(
                        jsonPath("$.id")
                                .value(1)
                )
                .andExpect(
                        jsonPath("$.customerId")
                                .value(100)
                )
                .andExpect(
                        jsonPath("$.status")
                                .value("CREATED")
                )
                .andExpect(
                        jsonPath("$.currency")
                                .value("EUR")
                )
                .andExpect(
                        jsonPath("$.totalAmount")
                                .value(2425.00)
                )
                .andExpect(
                        jsonPath("$.items.length()")
                                .value(2)
                );

        verify(orderService)
                .getOrder(1L);
    }

    // =========================================================
    // GET CUSTOMER ORDERS
    // =========================================================

    @Test
    void shouldGetCustomerOrders()
            throws Exception {

        when(orderService.getCustomerOrders(100L))
                .thenReturn(
                        List.of(
                                createOrderResponse()
                        )
                );

        mockMvc.perform(
                        get("/orders/customer/100")
                                .accept(
                                        MediaType.APPLICATION_JSON
                                )
                )
                .andExpect(status().isOk())

                .andExpect(
                        jsonPath("$.length()")
                                .value(1)
                )
                .andExpect(
                        jsonPath("$[0].id")
                                .value(1)
                )
                .andExpect(
                        jsonPath("$[0].customerId")
                                .value(100)
                )
                .andExpect(
                        jsonPath("$[0].status")
                                .value("CREATED")
                );

        verify(orderService)
                .getCustomerOrders(100L);
    }

    // =========================================================
    // GET ALL ORDERS
    // =========================================================

    @Test
    void shouldGetAllOrders()
            throws Exception {

        OrderResponse order1 =
                createOrderResponse();

        OrderResponse order2 =
                new OrderResponse(
                        2L,
                        200L,
                        OrderStatus.CREATED,
                        new BigDecimal("50.00"),
                        "EUR",
                        "CARD",

                        "Second Street 20",
                        "74072",
                        "Heilbronn",
                        "DE",

                        List.of(),

                        0L,
                        null,
                        null
                );

        when(orderService.getOrders())
                .thenReturn(
                        List.of(
                                order1,
                                order2
                        )
                );

        mockMvc.perform(
                        get("/orders")
                                .accept(
                                        MediaType.APPLICATION_JSON
                                )
                )
                .andExpect(status().isOk())

                .andExpect(
                        jsonPath("$.length()")
                                .value(2)
                )
                .andExpect(
                        jsonPath("$[0].id")
                                .value(1)
                )
                .andExpect(
                        jsonPath("$[1].id")
                                .value(2)
                )
                .andExpect(
                        jsonPath("$[0].customerId")
                                .value(100)
                )
                .andExpect(
                        jsonPath("$[1].customerId")
                                .value(200)
                );

        verify(orderService)
                .getOrders();
    }

    // =========================================================
    // CANCEL ORDER
    // =========================================================

    @Test
    void shouldCancelOrder()
            throws Exception {

        OrderResponse response =
                new OrderResponse(
                        1L,
                        100L,
                        OrderStatus.CANCELLED,
                        new BigDecimal("2425.00"),
                        "EUR",
                        "CARD",
                        "Main Street 10",
                        "74172",
                        "Neckarsulm",
                        "DE",

                        List.of(),

                        1L,
                        null,
                        null
                );

        when(orderService.cancelOrder(1L))
                .thenReturn(response);

        mockMvc.perform(
                        delete("/orders/1")
                                .accept(
                                        MediaType.APPLICATION_JSON
                                )
                )
                .andExpect(status().isOk())

                .andExpect(
                        jsonPath("$.id")
                                .value(1)
                )
                .andExpect(
                        jsonPath("$.status")
                                .value("CANCELLED")
                );

        verify(orderService)
                .cancelOrder(1L);
    }

    // =========================================================
    // VALIDATION - CUSTOMER ID
    // =========================================================

    @Test
    void shouldRejectCheckoutWithoutCustomerId()
            throws Exception {

        String request = """
                {
                    "items": [
                        {
                            "productId": 1,
                            "quantity": 2
                        }
                    ],
                    "deliveryAddress": {
                        "street": "Main Street 10",
                        "postalCode": "74172",
                        "city": "Neckarsulm",
                        "country": "DE"
                    }
                }
                """;

        mockMvc.perform(
                        post("/orders/checkout")
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(request)
                )
                .andExpect(
                        status().isBadRequest()
                );
    }

    // =========================================================
    // VALIDATION - EMPTY ITEMS
    // =========================================================

    @Test
    void shouldRejectCheckoutWithEmptyItems()
            throws Exception {

        String request = """
                {
                    "customerId": 100,
                    "items": [],
                    "deliveryAddress": {
                        "street": "Main Street 10",
                        "postalCode": "74172",
                        "city": "Neckarsulm",
                        "country": "DE"
                    }
                }
                """;

        mockMvc.perform(
                        post("/orders/checkout")
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(request)
                )
                .andExpect(
                        status().isBadRequest()
                );
    }

    // =========================================================
    // VALIDATION - QUANTITY
    // =========================================================

    @Test
    void shouldRejectCheckoutWithInvalidQuantity()
            throws Exception {

        String request = """
                {
                    "customerId": 100,
                    "items": [
                        {
                            "productId": 1,
                            "quantity": 0
                        }
                    ],
                    "deliveryAddress": {
                        "street": "Main Street 10",
                        "postalCode": "74172",
                        "city": "Neckarsulm",
                        "country": "DE"
                    }
                }
                """;

        mockMvc.perform(
                        post("/orders/checkout")
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(request)
                )
                .andExpect(
                        status().isBadRequest()
                );
    }

    // =========================================================
    // VALIDATION - DELIVERY ADDRESS
    // =========================================================

    @Test
    void shouldRejectCheckoutWithoutDeliveryAddress()
            throws Exception {

        String request = """
                {
                    "customerId": 100,
                    "items": [
                        {
                            "productId": 1,
                            "quantity": 2
                        }
                    ]
                }
                """;

        mockMvc.perform(
                        post("/orders/checkout")
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(request)
                )
                .andExpect(
                        status().isBadRequest()
                );
    }

    // =========================================================
    // TEST DATA
    // =========================================================

    private OrderResponse createOrderResponse() {

        return new OrderResponse(
                1L,
                100L,
                OrderStatus.CREATED,
                new BigDecimal("2425.00"),
                "EUR",
                "CARD",
                "Main Street 10",
                "74172",
                "Neckarsulm",
                "DE",
                List.of(
                        new OrderItemResponse(
                                1L,
                                "LAPTOP-001",
                                "Laptop",
                                new BigDecimal("1200.00"),
                                2,
                                new BigDecimal("2400.00")
                        ),

                        new OrderItemResponse(
                                2L,
                                "MOUSE-001",
                                "Mouse",
                                new BigDecimal("25.00"),
                                1,
                                new BigDecimal("25.00")
                        )
                ),

                0L,

                null,

                null
        );
    }
}