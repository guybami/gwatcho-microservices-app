package com.gwatcho.paymentservice.payment;

public record PaymentResult(
        boolean successful,
        String transactionId,
        String failureReason
) {
}