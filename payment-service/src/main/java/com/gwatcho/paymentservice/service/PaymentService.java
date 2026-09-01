package com.gwatcho.paymentservice.service;

import com.gwatcho.paymentservice.controller.PaymentController;
import com.gwatcho.paymentservice.dto.DeliveryAddress;
import com.gwatcho.paymentservice.dto.PaymentRequest;
import com.gwatcho.paymentservice.entity.Payment;
import com.gwatcho.paymentservice.entity.PaymentStatus;
import com.gwatcho.paymentservice.event.OrderCreatedEvent;
import com.gwatcho.paymentservice.event.PaymentCompletedEvent;
import com.gwatcho.paymentservice.kafka.PaymentCompletedEventProducer;
import com.gwatcho.paymentservice.repository.PaymentRepository;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentService {
    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    private final PaymentRepository paymentRepository;

    private final PaymentCompletedEventProducer paymentCompletedEventProducer;

    public PaymentService(
            PaymentRepository paymentRepository, PaymentCompletedEventProducer paymentCompletedEventProducer) {
        this.paymentRepository = paymentRepository;
        this.paymentCompletedEventProducer = paymentCompletedEventProducer;
    }

    public Payment createPayment(
            PaymentRequest request
    ) {

        Payment payment =
                new Payment(
                        request.orderId(),
                        request.customerId(),
                        request.amount(),
                        request.currency(),
                        request.paymentMethod()
                );

        return paymentRepository.save(payment);
    }

    @Transactional
    public Payment processOrderCreated(OrderCreatedEvent event) {
        log.info("Processing order.created: orderId={}, customerId={}, amount={}", event.orderId(), event.customerId(),
                event.totalAmount());

        /*
         * Idempotency:
         *
         * Kafka can deliver the same event more than once.
         */
        var existing = paymentRepository.findByOrderId(event.orderId());

        if (existing.isPresent()) {
            Payment payment = existing.get();

            log.info("Payment already exists for orderId={}, paymentId={}, status={}", payment.getOrderId(), payment.getId(),
                    payment.getStatus());

            return payment;
        }

        Payment payment =
                new Payment(event.orderId(), event.customerId(), event.totalAmount(), event.currency(), event.paymentMethod());

        paymentRepository.save(payment);

        /*
         * In this demo we simulate successful payment.
         */
        String transactionId = "TXN-" + UUID.randomUUID();

        payment.complete(transactionId);

        paymentRepository.save(payment);

        PaymentCompletedEvent completedEvent = new PaymentCompletedEvent(payment.getId(), payment.getOrderId(),
                payment.getCustomerId(), payment.getAmount(), payment.getCurrency(),
                payment.getTransactionId(),  new DeliveryAddress(
                event.street(),
                event.postalCode(),
                event.city(),
                event.country()
        ));

        paymentCompletedEventProducer.publish(completedEvent);

        log.info("Payment completed: paymentId={}, orderId={}, transactionId={}", payment.getId(), payment.getOrderId(),
                payment.getTransactionId());

        return payment;
    }


    @Transactional(readOnly = true)
    public Payment getPayment( Long paymentId ) {
        return paymentRepository .findById(paymentId) .orElseThrow(
            () -> new IllegalArgumentException( "Payment not found: " + paymentId ) );
    }
}