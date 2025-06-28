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

    private String generalDescription;

    @NotNull(message = "Project ID is required")
    private UUID projectId;

    @NotNull(message = "Project Budget Line Item ID is required")
    private UUID projectBudgetAccountId;

    @NotEmpty(message = "At least one beneficiary is required")
    @Valid
    private List<VoucherBeneficiaryRequest> beneficiaries;

    @Valid
    private List<UUID> attachments;

}