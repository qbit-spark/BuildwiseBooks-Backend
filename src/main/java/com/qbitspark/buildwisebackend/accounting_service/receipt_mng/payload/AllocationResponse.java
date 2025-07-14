package com.qbitspark.buildwisebackend.accounting_service.receipt_mng.payload;

import com.qbitspark.buildwisebackend.accounting_service.receipt_mng.enums.AllocationStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
public class AllocationResponse {

    private UUID allocationId;
    private UUID receiptId;
    private String receiptNumber;
    private BigDecimal receiptAmount;
    private AllocationStatus status;
    private String notes;
    private List<UUID> attachments;


    private UUID requestedBy;
    private String requestedByName;
    private LocalDateTime requestedAt;

    private UUID approvedBy;
    private String approvedByName;
    private LocalDateTime approvedAt;
    private String approvalNotes;
    private String rejectionReason;

    private BigDecimal totalAllocatedAmount;
    private int detailCount;


    private List<AllocationDetailResponse> details;

    @Data
    public static class AllocationDetailResponse {
        private UUID detailId;
        private UUID budgetDetailAllocationId;
        private String headerAccountName;
        private String headerAccountCode;
        private String detailAccountName;
        private String detailAccountCode;
        private BigDecimal amount;

    }
}