package com.qbitspark.buildwisebackend.accounting_service.receipt_mng.payload;

import com.qbitspark.buildwisebackend.accounting_service.receipt_mng.enums.AllocationStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ReceiptAllocationSummaryResponse {

    private UUID allocationId;
    private UUID receiptId;
    private String receiptNumber;
    private BigDecimal receiptAmount;
    private AllocationStatus status;
    private String notes;
    private BigDecimal totalAllocatedAmount;
    private boolean fullyAllocated;
    private UUID requestedBy;
    private LocalDateTime requestedAt;
    private LocalDateTime createdAt;
    private int totalAllocationDetails;
}