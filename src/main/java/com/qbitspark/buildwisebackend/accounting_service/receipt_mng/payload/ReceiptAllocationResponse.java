package com.qbitspark.buildwisebackend.accounting_service.receipt_mng.payload;

import com.qbitspark.buildwisebackend.accounting_service.receipt_mng.enums.AllocationStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
public class ReceiptAllocationResponse {
    private UUID allocationId;
    private UUID receiptId;
    private String receiptNumber;
    private String notes;
    private AllocationStatus status;
    private UUID requestedBy;
    private String requestedByName;
    private UUID approvedBy;
    private String approvedByName;
    private LocalDateTime approvedAt;
    private String approvalNotes;
    private String rejectionReason;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private BigDecimal totalAllocatedAmount;
    private int totalDetailAllocations;
    private boolean canApprove;
    private boolean canReject;
    private boolean canEdit;

    private List<DetailAllocationResponse> detailAllocations;

    @Data
    public static class DetailAllocationResponse {
        private UUID detailId;
        private UUID detailAccountId;
        private String detailAccountName;
        private String detailAccountCode;
        private BigDecimal amount;
        private String description;
        private LocalDateTime createdAt;
    }
}