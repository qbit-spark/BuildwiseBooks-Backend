package com.qbitspark.buildwisebackend.accounting_service.documentflow.invoice.enums;

import lombok.Getter;

@Getter
public enum InvoiceStatus {
    DRAFT,
    PENDING_APPROVAL,
    REJECTED,
    CANCELLED,
    APPROVED,
    PAID,
    PARTIALLY_PAID,
    OVERDUE;

    public boolean canReceivePayment() {
        return this == APPROVED || this == PARTIALLY_PAID || this == OVERDUE;
    }
}