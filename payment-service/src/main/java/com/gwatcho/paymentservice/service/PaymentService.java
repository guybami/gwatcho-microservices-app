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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentProvider paymentProvider;
    private final PaymentEventPublisher eventPublisher;

    public PaymentService(
            PaymentRepository paymentRepository,
            PaymentProvider paymentProvider,
            PaymentEventPublisher eventPublisher) {

        this.paymentRepository = paymentRepository;
        this.paymentProvider = paymentProvider;
        this.eventPublisher = eventPublisher;
    }

    public PaymentResponse createPayment(
            PaymentRequest request) {

        if (paymentRepository.existsByOrderId(
                request.orderId())) {

            throw new BusinessException(
                    "Payment already exists for order "
                            + request.orderId()
            );
        }

        Payment payment = new Payment(
                request.orderId(),
                request.customerId(),
                request.amount(),
                request.currency(),
                PaymentStatus.PENDING,
                request.paymentMethod(),
                null
        );

        Payment savedPayment =
                paymentRepository.save(payment);

        PaymentResult result =
                paymentProvider.processPayment(
                        request.amount(),
                        request.currency(),
                        request.paymentMethod()
                );

        if (result.successful()) {

            savedPayment.setStatus(
                    PaymentStatus.COMPLETED
            );

            savedPayment.setTransactionId(
                    result.transactionId()
            );

            Payment completedPayment =
                    paymentRepository.save(savedPayment);

            eventPublisher.publishCompleted(
                    completedPayment
            );

            return toResponse(completedPayment);
        }

        savedPayment.setStatus(
                PaymentStatus.FAILED
        );

        Payment failedPayment =
                paymentRepository.save(savedPayment);

        eventPublisher.publishFailed(
                failedPayment,
                result.failureReason()
        );

        return toResponse(failedPayment);
    }

    @Transactional(readOnly = true)
    public PaymentResponse getPayment(Long id) {

        Payment payment =
                paymentRepository.findById(id)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Payment with id "
                                                + id
                                                + " not found"
                                )
                        );

        return toResponse(payment);
    }

    private PaymentResponse toResponse(
            Payment payment) {

        return new PaymentResponse(
                payment.getId(),
                payment.getOrderId(),
                payment.getCustomerId(),
                payment.getAmount(),
                payment.getCurrency(),
                payment.getStatus(),
                payment.getPaymentMethod(),
                payment.getTransactionId(),
                payment.getCreatedAt(),
                payment.getUpdatedAt()
        );
    }
}