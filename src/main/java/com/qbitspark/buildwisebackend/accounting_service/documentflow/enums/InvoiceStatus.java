package com.qbitspark.buildwisebackend.accounting_service.documentflow.enums;

import lombok.Getter;

@Getter
public enum InvoiceStatus {

    DRAFT,
    PENDING_APPROVAL,
    APPROVED,
    SENT,
    VIEWED,
    PAID,
    PARTIALLY_PAID,
    OVERDUE,
    CANCELLED,
    REFUNDED;

    public boolean canBeSent() {
        return this == APPROVED || this == DRAFT;
    }
}