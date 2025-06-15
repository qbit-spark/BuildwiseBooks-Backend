package com.qbitspark.buildwisebackend.accounting_service.documentflow.payment.enums;

public enum PaymentType {
    VOUCHER,        // Payment for approved vouchers
    INVOICE,        // Customer payment for invoices (future)
    SALARY,         // Employee salary payment (future)
    ASSET,          // Asset purchase payment (future)
    LOAN,           // Loan payment (future)
    REFUND,         // Refund payment (future)
    OTHER           // Miscellaneous payments (future)
}