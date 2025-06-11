package com.qbitspark.buildwisebackend.accounting_service.documentflow.invoice.enums;

import lombok.Getter;

@Getter
public enum InvoiceType {
    PROGRESS_PAYMENT("Progress Payment", "For construction work completed to date"),
    MILESTONE_PAYMENT("Milestone Payment", "Payment for reaching specific project milestones"),
    MATERIAL_ADVANCE("Material Advance", "Advance payment for materials and supplies"),
    RETENTION_RELEASE("Retention Release", "Release of previously held retention amounts");

    private final String displayName;
    private final String description;

    InvoiceType(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }
}
