package com.gwatcho.paymentservice.repository;

import com.gwatcho.paymentservice.entity.Payment;
import com.gwatcho.paymentservice.entity.PaymentStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;


@ActiveProfiles("test")
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class PaymentRepositoryIntegrationTest {

    @Autowired
    private PaymentRepository paymentRepository;

    @Test
    void contextLoads() {
        assertThat(paymentRepository).isNotNull();
    }

    @Test
    void savePayment() {

        Payment payment = new Payment(
                100L,
                10L,
                new BigDecimal("149.99"),
                "EUR",
                "CARD"
        );

        Payment saved =
                paymentRepository.save(payment);

        assertNotNull(saved.getId());
        assertEquals(100L, saved.getOrderId());
        assertEquals(
                PaymentStatus.PENDING,
                saved.getStatus()
        );
    }

    @Test
    void findByOrderId() {

        Payment payment = new Payment(
                100L,
                10L,
                new BigDecimal("149.99"),
                "EUR",
                "CARD"

        );

        paymentRepository.save(payment);

        Optional<Payment> result =
                paymentRepository.findByOrderId(100L);

        assertTrue(result.isPresent());
        assertEquals(
                100L,
                result.get().getOrderId()
        );
        assertEquals(
                PaymentStatus.PENDING,
                result.get().getStatus()
        );
    }

    @Test
    void existsByOrderId_returnsTrue() {

        Payment payment = new Payment(
                100L,
                10L,
                new BigDecimal("149.99"),
                "EUR",
                "CARD"
        );
        payment.pending("TX-123");
        paymentRepository.save(payment);

        assertTrue(
                paymentRepository.existsByOrderId(100L)
        );
    }
}