package com.qbitspark.buildwisebackend.accounting_service.transactions_pipeline.dto;

import lombok.Data;

import java.util.UUID;

@Data
public class ApprovalRequest {
    private UUID approverId;
    private String comments;
}
