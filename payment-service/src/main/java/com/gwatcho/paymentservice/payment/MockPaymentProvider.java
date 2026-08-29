package com.gwatcho.paymentservice.payment;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

@Component
public class MockPaymentProvider implements PaymentProvider {

    @Override
    public PaymentResult processPayment(
            BigDecimal amount,
            String currency,
            String paymentMethod) {

        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            return new PaymentResult(
                    false,
                    null,
                    "Payment amount must be greater than zero"
            );
        }

        return new PaymentResult(
                true,
                "TX-" + UUID.randomUUID(),
                null
        );
    }
}