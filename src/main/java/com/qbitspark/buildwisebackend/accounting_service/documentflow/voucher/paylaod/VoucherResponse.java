package com.qbitspark.buildwisebackend.accounting_service.documentflow.voucher.paylaod;

import com.qbitspark.buildwisebackend.accounting_service.documentflow.voucher.enums.PaymentMode;
import com.qbitspark.buildwisebackend.accounting_service.documentflow.voucher.enums.VoucherStatus;
import com.qbitspark.buildwisebackend.accounting_service.documentflow.voucher.enums.VoucherType;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
public class VoucherResponse {
    private UUID id;
    private String voucherNumber;
    private String generalDescription;
    private VoucherStatus status;
    private BigDecimal totalAmount;
    private BigDecimal totalDeductions;
    private BigDecimal netAmount;
    private String currency;

    private UUID detailAllocationId;
    private String headerAccountName;
    private String headerAccountCode;
    private String detailAccountName;
    private String detailAccountCode;
    private BigDecimal allocationRemaining;

    // Organisation info
    private UUID organisationId;
    private String organisationName;

    // Project info (always present)
    private UUID projectId;
    private String projectName;
    private String projectCode;

    // Creator info
    private UUID createdById;
    private String createdByName;

    // Beneficiaries and attachments
    private List<VoucherBeneficiaryResponse> beneficiaries;
    private List<VoucherAttachmentResponse> attachments;

    // Timestamps
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}