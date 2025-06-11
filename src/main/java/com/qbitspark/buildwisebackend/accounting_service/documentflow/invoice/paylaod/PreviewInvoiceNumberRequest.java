package com.qbitspark.buildwisebackend.accounting_service.documentflow.invoice.paylaod;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PreviewInvoiceNumberRequest {

    @NotNull(message = "Project ID is required")
    private UUID projectId;

    @NotNull(message = "Client ID is required")
    private UUID clientId;

}
