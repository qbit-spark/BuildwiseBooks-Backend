package com.qbitspark.buildwisebackend.accounting_service.documentflow.voucher.paylaod;

import lombok.Data;

@Data
public class ApproveVoucherRequest {
    private String approvedBy;
    private String comments;
}