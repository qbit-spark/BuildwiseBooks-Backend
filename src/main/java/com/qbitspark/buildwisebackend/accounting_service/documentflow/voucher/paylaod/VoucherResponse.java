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
    private LocalDateTime voucherDate;
    private VoucherType voucherType;
    private VoucherStatus status;
    private UUID projectId;
    private String projectName;
    private PaymentMode paymentMode;
    private BigDecimal totalAmount;
    private String currency;
    private String overallDescription;
    private UUID createdById;
    private String createdByName;
    private UUID organisationId;
    private String organisationName;
    private List<VoucherPayeeResponse> payees;
    private List<VoucherAttachmentResponse> attachments; // Add this line
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}