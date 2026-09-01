package com.gwatcho.paymentservice.controller;

import com.gwatcho.paymentservice.dto.PaymentRequest;
import com.gwatcho.paymentservice.dto.PaymentResponse;
import com.gwatcho.paymentservice.service.PaymentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.gwatcho.paymentservice.entity.Payment;
import com.gwatcho.paymentservice.repository.PaymentRepository;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/payments")
public class PaymentController {

    private final PaymentRepository paymentRepository;

    public PaymentController(
            PaymentRepository paymentRepository
    ) {
        this.paymentRepository = paymentRepository;
    }

    // =========================================================
    // GET ALL PAYMENTS
    // =========================================================

    @GetMapping
    public ResponseEntity<List<Payment>> getAllPayments() {

        return ResponseEntity.ok(
                paymentRepository.findAll()
        );
    }

    // =========================================================
    // GET PAYMENT BY ID
    // =========================================================

    @GetMapping("/{paymentId}")
    public ResponseEntity<Payment> getPayment(
            @PathVariable Long paymentId
    ) {

        return paymentRepository
                .findById(paymentId)
                .map(ResponseEntity::ok)
                .orElseGet(
                        () -> ResponseEntity
                                .notFound()
                                .build()
                );
    }

    // =========================================================
    // GET PAYMENT BY ORDER ID
    // =========================================================

    @GetMapping("/order/{orderId}")
    public ResponseEntity<Payment> getPaymentByOrderId(
            @PathVariable Long orderId
    ) {

        return paymentRepository
                .findByOrderId(orderId)
                .map(ResponseEntity::ok)
                .orElseGet(
                        () -> ResponseEntity
                                .notFound()
                                .build()
                );
    }

    // =========================================================
    // CREATE PAYMENT MANUALLY
    // =========================================================

    @PostMapping
    public ResponseEntity<Payment> createPayment(
            @RequestBody PaymentRequest request
    ) {

        Payment payment =
                new Payment(
                        request.orderId(),
                        request.customerId(),
                        request.amount(),
                        request.currency(),
                        request.paymentMethod()
                );

        Payment saved =
                paymentRepository.save(payment);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(saved);
    }




}
