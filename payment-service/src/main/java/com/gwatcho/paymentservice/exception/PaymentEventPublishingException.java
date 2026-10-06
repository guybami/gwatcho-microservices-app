package com.gwatcho.paymentservice.exception;

public class PaymentEventPublishingException extends RuntimeException {

    public PaymentEventPublishingException(String message) {
        super(message);
    }

    public PaymentEventPublishingException(
            String message,
            Throwable cause) {
        super(message, cause);
    }
}