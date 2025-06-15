package com.qbitspark.buildwisebackend.accounting_service.documentflow.payment.service;

import com.qbitspark.buildwisebackend.accounting_service.documentflow.payment.payload.CreateVoucherPaymentRequest;
import com.qbitspark.buildwisebackend.accounting_service.documentflow.payment.payload.PaymentResponse;
import com.qbitspark.buildwisebackend.globeadvice.exceptions.AccessDeniedException;
import com.qbitspark.buildwisebackend.globeadvice.exceptions.ItemNotFoundException;

import java.util.List;
import java.util.UUID;

public interface PaymentService {

    /**
     * Create payment for vouchers
     */
    PaymentResponse createVoucherPayment(UUID organisationId, CreateVoucherPaymentRequest request)
            throws ItemNotFoundException, AccessDeniedException;

    /**
     * Get payment by ID
     */
    PaymentResponse getPaymentById(UUID organisationId, UUID paymentId)
            throws ItemNotFoundException, AccessDeniedException;

    /**
     * Get all payments for organisation
     */
    List<PaymentResponse> getOrganisationPayments(UUID organisationId)
            throws ItemNotFoundException, AccessDeniedException;

    /**
     * Process/execute the payment (marks as completed and creates accounting entries)
     */
    PaymentResponse processPayment(UUID organisationId, UUID paymentId)
            throws ItemNotFoundException, AccessDeniedException;
}
