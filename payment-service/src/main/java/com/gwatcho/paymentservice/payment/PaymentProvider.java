package com.gwatcho.paymentservice.payment;

import java.math.BigDecimal;

public interface PaymentProvider {

    PaymentResult processPayment(
            BigDecimal amount,
            String currency,
            String paymentMethod
    );
}