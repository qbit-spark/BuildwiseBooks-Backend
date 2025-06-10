package com.qbitspark.buildwisebackend.accounting_service.documentflow.paylaod;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PreviewInvoiceNumberResponse {

    private String nextInvoiceNumber;
    private String projectCode;
    private String projectName;
    private String clientName;
    private String organisationName;
}
