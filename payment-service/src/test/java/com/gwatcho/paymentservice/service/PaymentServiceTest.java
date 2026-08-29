package com.gwatcho.paymentservice.service;

import com.gwatcho.paymentservice.dto.PaymentRequest;
import com.gwatcho.paymentservice.dto.PaymentResponse;
import com.gwatcho.paymentservice.entity.Payment;
import com.gwatcho.paymentservice.entity.PaymentStatus;
import com.gwatcho.paymentservice.event.PaymentEventPublisher;
import com.gwatcho.paymentservice.exception.BusinessException;
import com.gwatcho.paymentservice.exception.ResourceNotFoundException;
import com.gwatcho.paymentservice.payment.PaymentProvider;
import com.gwatcho.paymentservice.payment.PaymentResult;
import com.gwatcho.paymentservice.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private PaymentProvider paymentProvider;

    @Mock
    private PaymentEventPublisher eventPublisher;

    private PaymentService paymentService;

    @BeforeEach
    void setUp() {
        paymentService = new PaymentService(
                paymentRepository,
                paymentProvider,
                eventPublisher
        );
    }

    @Test
    void createPayment_successfulPayment() {

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
                PaymentStatus.PENDING,
                "CARD",
                null
        );

        PaymentResult result = new PaymentResult(
                true,
                "TX-123",
                null
        );

        when(paymentRepository.existsByOrderId(100L))
                .thenReturn(false);

        when(paymentRepository.save(any(Payment.class)))
                .thenReturn(savedPayment);

        when(paymentProvider.processPayment(
                request.amount(),
                request.currency(),
                request.paymentMethod()
        )).thenReturn(result);

        PaymentResponse response =
                paymentService.createPayment(request);

        assertNotNull(response);
        assertEquals(100L, response.orderId());
        assertEquals(10L, response.customerId());
        assertEquals(
                new BigDecimal("149.99"),
                response.amount()
        );
        assertEquals("EUR", response.currency());
        assertEquals(
                PaymentStatus.COMPLETED,
                response.status()
        );

        verify(paymentRepository, times(2))
                .save(any(Payment.class));

        verify(eventPublisher)
                .publishCompleted(savedPayment);
    }

    @Test
    void createPayment_failedPayment() {

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
                PaymentStatus.PENDING,
                "CARD",
                null
        );

        PaymentResult result = new PaymentResult(
                false,
                null,
                "Card declined"
        );

        when(paymentRepository.existsByOrderId(100L))
                .thenReturn(false);

        when(paymentRepository.save(any(Payment.class)))
                .thenReturn(savedPayment);

        when(paymentProvider.processPayment(
                request.amount(),
                request.currency(),
                request.paymentMethod()
        )).thenReturn(result);

        PaymentResponse response =
                paymentService.createPayment(request);

        assertNotNull(response);
        assertEquals(
                PaymentStatus.FAILED,
                response.status()
        );

        verify(eventPublisher)
                .publishFailed(
                        savedPayment,
                        "Card declined"
                );
    }

    @Test
    void createPayment_whenOrderAlreadyHasPayment_throwsException() {

        PaymentRequest request = new PaymentRequest(
                100L,
                10L,
                new BigDecimal("149.99"),
                "EUR",
                "CARD"
        );

        when(paymentRepository.existsByOrderId(100L))
                .thenReturn(true);

        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () -> paymentService.createPayment(request)
                );

        assertEquals(
                "Payment already exists for order 100",
                exception.getMessage()
        );

        verify(paymentRepository, never())
                .save(any());

        verifyNoInteractions(paymentProvider);
        verifyNoInteractions(eventPublisher);
    }

    @Test
    void getPayment_returnsPayment() {

        Payment payment = new Payment(
                100L,
                10L,
                new BigDecimal("149.99"),
                "EUR",
                PaymentStatus.COMPLETED,
                "CARD",
                "TX-123"
        );

        when(paymentRepository.findById(1L))
                .thenReturn(Optional.of(payment));

        PaymentResponse response =
                paymentService.getPayment(1L);

        assertNotNull(response);
        assertEquals(100L, response.orderId());
        assertEquals(
                PaymentStatus.COMPLETED,
                response.status()
        );
        assertEquals(
                "TX-123",
                response.transactionId()
        );
    }

    @Test
    void getPayment_whenNotFound_throwsException() {

        when(paymentRepository.findById(999L))
                .thenReturn(Optional.empty());

        ResourceNotFoundException exception =
                assertThrows(
                        ResourceNotFoundException.class,
                        () -> paymentService.getPayment(999L)
                );

        assertEquals(
                "Payment with id 999 not found",
                exception.getMessage()
        );
    }
}