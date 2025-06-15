package com.qbitspark.buildwisebackend.accounting_service.documentflow.payment.enums;

public enum PaymentStatus {
    PENDING,        // Payment created but not processed
    PROCESSING,     // Payment being processed
    COMPLETED,      // Payment successfully completed
    FAILED,         // Payment failed
    CANCELLED       // Payment cancelled
}