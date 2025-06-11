package com.qbitspark.buildwisebackend.accounting_service.documentflow.voucher.paylaod;

import com.qbitspark.buildwisebackend.accounting_service.documentflow.voucher.enums.PaymentMode;
import com.qbitspark.buildwisebackend.accounting_service.documentflow.voucher.enums.VoucherType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
public class UpdateVoucherRequest {

    private LocalDate voucherDate;

    private VoucherType voucherType;

    private PaymentMode paymentMode;

    @Size(max = 1000, message = "Overall description cannot exceed 1000 characters")
    private String overallDescription;

    @Valid
    private List<VoucherPayeeRequest> payees;

    // Attachment handling - provide list of attachment IDs to keep
    // Any existing attachments not in this list will be removed
    private List<UUID> attachmentIdsToKeep;

    // New attachments to add (if file upload is handled separately)
    @Valid
    private List<VoucherAttachmentRequest> newAttachments;

}