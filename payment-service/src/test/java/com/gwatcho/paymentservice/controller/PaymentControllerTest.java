package com.gwatcho.paymentservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gwatcho.paymentservice.config.SecurityConfig;
import com.gwatcho.paymentservice.dto.PaymentRequest;
import com.gwatcho.paymentservice.entity.Payment;
import com.gwatcho.paymentservice.repository.PaymentRepository;
import com.gwatcho.paymentservice.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = PaymentController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = SecurityConfig.class
        )
)
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PaymentRepository paymentRepository;


    // =========================================================
    // CREATE PAYMENT
    // =========================================================

    @Test
    void createPayment_returns201() throws Exception {

        PaymentRequest request = new PaymentRequest(
                100L,
                10L,
                new BigDecimal("149.99"),
                "EUR",
                "CARD"
        );

        Payment savedPayment = new Payment(
                100L,
                10L,
                new BigDecimal("149.99"),
                "EUR",
                "CARD"
        );

        // Simulate database-generated ID
        ReflectionTestUtils.setField(savedPayment, "id", 1L);

        savedPayment.complete("TX-123");

        when(paymentRepository.save(any(Payment.class)))
                .thenReturn(savedPayment);

        mockMvc.perform(
                        post("/payments")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.orderId").value(100))
                .andExpect(jsonPath("$.customerId").value(10))
                .andExpect(jsonPath("$.amount").value(149.99))
                .andExpect(jsonPath("$.currency").value("EUR"))
                .andExpect(jsonPath("$.paymentMethod").value("CARD"))
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.transactionId").value("TX-123"));
    }


    // =========================================================
    // VALIDATION - ORDER ID
    // =========================================================

    @Test
    void createPayment_withoutOrderId_returns400() throws Exception {

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
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }


    // =========================================================
    // VALIDATION - CUSTOMER ID
    // =========================================================

    @Test
    void createPayment_withoutCustomerId_returns400() throws Exception {

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


    // =========================================================
    // VALIDATION - AMOUNT
    // =========================================================

    @Test
    void createPayment_withInvalidAmount_returns400() throws Exception {

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


    // =========================================================
    // GET PAYMENT
    // =========================================================

    @Test
    void getPayment_returns200() throws Exception {

        Payment payment = new Payment(
                100L,
                10L,
                new BigDecimal("149.99"),
                "EUR",
                "CARD"
        );

        // Simulate database-generated ID
        ReflectionTestUtils.setField(payment, "id", 1L);

        payment.complete("TX-123");

        when(paymentRepository.findById(1L))
                .thenReturn(Optional.of(payment));

        mockMvc.perform(get("/payments/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.orderId").value(100))
                .andExpect(jsonPath("$.customerId").value(10))
                .andExpect(jsonPath("$.amount").value(149.99))
                .andExpect(jsonPath("$.currency").value("EUR"))
                .andExpect(jsonPath("$.paymentMethod").value("CARD"))
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.transactionId").value("TX-123"));
    }


    // =========================================================
    // GET PAYMENT - NOT FOUND
    // =========================================================

    @Test
    void getPayment_whenNotFound_returns404() throws Exception {

        when(paymentRepository.findById(999L))
                .thenReturn(Optional.empty());

        mockMvc.perform(get("/payments/999"))
                .andExpect(status().isNotFound());
    }
}