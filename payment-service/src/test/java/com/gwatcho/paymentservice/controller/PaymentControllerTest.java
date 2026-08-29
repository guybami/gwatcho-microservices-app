package com.gwatcho.paymentservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gwatcho.paymentservice.dto.PaymentRequest;
import com.gwatcho.paymentservice.dto.PaymentResponse;
import com.gwatcho.paymentservice.entity.PaymentStatus;
import com.gwatcho.paymentservice.exception.GlobalExceptionHandler;
import com.gwatcho.paymentservice.exception.ResourceNotFoundException;
import com.gwatcho.paymentservice.service.PaymentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PaymentController.class)
@Import(GlobalExceptionHandler.class)
class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PaymentService paymentService;

    @Test
    void createPayment_returns201() throws Exception {

        PaymentRequest request = new PaymentRequest(
                100L,
                10L,
                new BigDecimal("149.99"),
                "EUR",
                "CARD"
        );

        PaymentResponse response = new PaymentResponse(
                1L,
                100L,
                10L,
                new BigDecimal("149.99"),
                "EUR",
                PaymentStatus.COMPLETED,
                "CARD",
                "TX-123",
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        when(paymentService.createPayment(request))
                .thenReturn(response);

        mockMvc.perform(
                        post("/payments")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.orderId").value(100))
                .andExpect(jsonPath("$.customerId").value(10))
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    @Test
    void createPayment_withoutOrderId_returns400()
            throws Exception {

        String request = """
                {
                    "customerId": 10,
                    "amount": 149.99,
                    "currency": "EUR",
                    "paymentMethod": "CARD"
                }
                """;

        mockMvc.perform(
                        post("/payments")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(request)
                )
                .andExpect(status().isBadRequest())
                .andExpect(
                        jsonPath("$.error")
                                .value("VALIDATION_ERROR")
                );
    }

    @Test
    void createPayment_withoutCustomerId_returns400()
            throws Exception {

        String request = """
                {
                    "orderId": 100,
                    "amount": 149.99,
                    "currency": "EUR",
                    "paymentMethod": "CARD"
                }
                """;

        mockMvc.perform(
                        post("/payments")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(request)
                )
                .andExpect(status().isBadRequest());
    }

    @Test
    void createPayment_withInvalidAmount_returns400()
            throws Exception {

        String request = """
                {
                    "orderId": 100,
                    "customerId": 10,
                    "amount": 0,
                    "currency": "EUR",
                    "paymentMethod": "CARD"
                }
                """;

        mockMvc.perform(
                        post("/payments")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(request)
                )
                .andExpect(status().isBadRequest());
    }

    @Test
    void getPayment_returns200() throws Exception {

        PaymentResponse response = new PaymentResponse(
                1L,
                100L,
                10L,
                new BigDecimal("149.99"),
                "EUR",
                PaymentStatus.COMPLETED,
                "CARD",
                "TX-123",
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        when(paymentService.getPayment(1L))
                .thenReturn(response);

        mockMvc.perform(get("/payments/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.orderId").value(100))
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    @Test
    void getPayment_whenNotFound_returns404()
            throws Exception {

        when(paymentService.getPayment(999L))
                .thenThrow(
                        new ResourceNotFoundException(
                                "Payment with id 999 not found"
                        )
                );

        mockMvc.perform(get("/payments/999"))
                .andExpect(status().isNotFound())
                .andExpect(
                        jsonPath("$.error")
                                .value("NOT_FOUND")
                )
                .andExpect(
                        jsonPath("$.message")
                                .value(
                                        "Payment with id 999 not found"
                                )
                );
    }
}