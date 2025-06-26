package com.qbitspark.buildwisebackend.accounting_service.documentflow.invoice.enums;

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
    ARCHIVE;
    public boolean canBeSent() {
        return this == APPROVED || this == DRAFT;
    }
}