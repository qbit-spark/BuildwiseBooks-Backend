package com.qbitspark.buildwisebackend.accounting_service.documentflow.payment.controller;

import com.qbitspark.buildwisebackend.accounting_service.documentflow.payment.payload.CreateVoucherPaymentRequest;
import com.qbitspark.buildwisebackend.accounting_service.documentflow.payment.payload.PaymentResponse;
import com.qbitspark.buildwisebackend.accounting_service.documentflow.payment.service.PaymentService;
import com.qbitspark.buildwisebackend.globeadvice.exceptions.AccessDeniedException;
import com.qbitspark.buildwisebackend.globeadvice.exceptions.ItemNotFoundException;
import com.qbitspark.buildwisebackend.globeresponsebody.GlobeSuccessResponseBuilder;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/doc-transactions/{organisationId}/payments")
@RequiredArgsConstructor
@Slf4j
public class PaymentController {

    private final PaymentService paymentService;

    /**
     * Create payment for vouchers
     */
    @PostMapping("/vouchers")
    public ResponseEntity<GlobeSuccessResponseBuilder> createVoucherPayment(
            @PathVariable UUID organisationId,
            @Valid @RequestBody CreateVoucherPaymentRequest request)
            throws ItemNotFoundException, AccessDeniedException {

        PaymentResponse response = paymentService.createVoucherPayment(organisationId, request);

        return ResponseEntity.ok(
                GlobeSuccessResponseBuilder.success(
                        "Voucher payment created successfully",
                        response
                )
        );
    }

    /**
     * Get payment by ID
     */
    @GetMapping("/{paymentId}")
    public ResponseEntity<GlobeSuccessResponseBuilder> getPaymentById(
            @PathVariable UUID organisationId,
            @PathVariable UUID paymentId)
            throws ItemNotFoundException, AccessDeniedException {

        PaymentResponse response = paymentService.getPaymentById(organisationId, paymentId);

        return ResponseEntity.ok(
                GlobeSuccessResponseBuilder.success(
                        "Payment retrieved successfully",
                        response
                )
        );
    }

    /**
     * Get all payments for organisation
     */
    @GetMapping
    public ResponseEntity<GlobeSuccessResponseBuilder> getOrganisationPayments(
            @PathVariable UUID organisationId)
            throws ItemNotFoundException, AccessDeniedException {

        List<PaymentResponse> responses = paymentService.getOrganisationPayments(organisationId);

        return ResponseEntity.ok(
                GlobeSuccessResponseBuilder.success(
                        "Organisation payments retrieved successfully",
                        responses
                )
        );
    }

    /**
     * Process/execute the payment
     */
    @PostMapping("/{paymentId}/process")
    public ResponseEntity<GlobeSuccessResponseBuilder> processPayment(
            @PathVariable UUID organisationId,
            @PathVariable UUID paymentId)
            throws ItemNotFoundException, AccessDeniedException {

        PaymentResponse response = paymentService.processPayment(organisationId, paymentId);

        return ResponseEntity.ok(
                GlobeSuccessResponseBuilder.success(
                        "Payment processed successfully",
                        response
                )
        );
    }}
