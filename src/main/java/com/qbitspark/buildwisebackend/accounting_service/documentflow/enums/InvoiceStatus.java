package com.qbitspark.buildwisebackend.accounting_service.documentflow.enums;

import lombok.Getter;

@Getter
public enum InvoiceStatus {

    DRAFT("Draft", "Invoice is being created or edited"),
    PENDING_APPROVAL("Pending Approval", "Waiting for internal approval"),
    APPROVED("Approved", "Invoice has been approved and ready to send"),
    SENT("Sent", "Invoice has been sent to client"),
    VIEWED("Viewed", "Client has viewed the invoice"),
    PAID("Paid", "Invoice has been fully paid"),
    PARTIALLY_PAID("Partially Paid", "Invoice has been partially paid"),
    OVERDUE("Overdue", "Invoice is past due date"),
    CANCELLED("Cancelled", "Invoice has been cancelled"),
    REFUNDED("Refunded", "Invoice payment has been refunded");

    private final String displayName;
    private final String description;

    InvoiceStatus(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public boolean isPaid() {
        return this == PAID;
    }

    public boolean isOverdue() {
        return this == OVERDUE;
    }

    public boolean canBeEdited() {
        return this == DRAFT || this == PENDING_APPROVAL;
    }

    public boolean canBeSent() {
        return this == APPROVED || this == DRAFT;
    }
}