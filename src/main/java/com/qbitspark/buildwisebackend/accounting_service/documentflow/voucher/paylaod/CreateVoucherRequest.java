package com.qbitspark.buildwisebackend.accounting_service.documentflow.voucher.paylaod;

import com.qbitspark.buildwisebackend.accounting_service.documentflow.voucher.enums.PaymentMode;
import com.qbitspark.buildwisebackend.accounting_service.documentflow.voucher.enums.VoucherType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
public class CreateVoucherRequest {

    @NotNull(message = "Voucher date is required")
    private LocalDate voucherDate;

    @NotNull(message = "Voucher type is required")
    private VoucherType voucherType;

    private UUID projectId; // Optional

    @NotNull(message = "Payment mode is required")
    private PaymentMode paymentMode;

    private String overallDescription;

    @NotEmpty(message = "At least one payee is required")
    @Valid
    private List<VoucherPayeeRequest> payees;

    @Valid
    private List<VoucherAttachmentRequest> attachments;

}