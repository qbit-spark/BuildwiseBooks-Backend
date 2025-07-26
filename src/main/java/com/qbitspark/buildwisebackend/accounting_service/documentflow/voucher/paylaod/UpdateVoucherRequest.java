package com.qbitspark.buildwisebackend.accounting_service.documentflow.voucher.paylaod;

import jakarta.validation.Valid;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class UpdateVoucherRequest {

    private String generalDescription;

    private UUID accountId;

    @Valid
    private List<VoucherBeneficiaryRequest> beneficiaries;

    private List<UUID> attachments;
}