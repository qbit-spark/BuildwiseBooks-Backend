package com.qbitspark.buildwisebackend.accounting_service.documentflow.paylaod;

import com.qbitspark.buildwisebackend.accounting_service.documentflow.enums.InvoiceType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data

public class CreateInvoiceDocRequest {

    @NotNull(message = "Project ID is required")
    private UUID projectId;

    @NotNull(message = "Organisation ID is required")
    private UUID organisationId;

    @NotNull(message = "Client ID is required")
    private UUID clientId;

    @NotNull(message = "Invoice type is required")
    private InvoiceType invoiceType;

    @NotNull(message = "Date of issue is required")
    private LocalDate dateOfIssue;

    @NotNull(message = "Due date is required")
    private LocalDate dueDate;

    private String reference;

    @NotEmpty(message = "At least one line item is required")
    @Valid
    private List<InvoiceLineItemRequest> lineItems;

    private BigDecimal discountAmount = BigDecimal.ZERO;
    private BigDecimal taxAmount = BigDecimal.ZERO;
    private String currency = "TZS";
}
