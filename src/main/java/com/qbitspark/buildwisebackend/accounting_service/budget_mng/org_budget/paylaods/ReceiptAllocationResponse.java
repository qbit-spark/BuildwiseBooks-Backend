package com.qbitspark.buildwisebackend.accounting_service.budget_mng.org_budget.paylaods;

import com.qbitspark.buildwisebackend.accounting_service.receipt_mng.enums.AllocationStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ReceiptAllocationResponse {

    private UUID allocationId;
    private UUID receiptId;
    private String receiptNumber;
    private BigDecimal receiptAmount;
    private AllocationStatus status;
    private String notes;

    private BigDecimal totalAllocatedAmount;
    private BigDecimal remainingAmount;
    private boolean isFullyAllocated;

    private UUID requestedBy;
    private LocalDateTime createdAt;
    private UUID approvedBy;
    private LocalDateTime approvedAt;


    private List<AllocationDetailResponse> allocationDetails;

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class AllocationDetailResponse {

        private UUID detailId;
        private UUID accountId;
        private String accountCode;
        private String accountName;
        private BigDecimal allocatedAmount;
        private String description;
    }
}