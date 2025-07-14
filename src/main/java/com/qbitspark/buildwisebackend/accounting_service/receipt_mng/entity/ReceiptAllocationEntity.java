package com.qbitspark.buildwisebackend.accounting_service.receipt_mng.entity;

import com.qbitspark.buildwisebackend.accounting_service.receipt_mng.enums.AllocationStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "receipt_allocations")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ReceiptAllocationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID allocationId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receipt_id", nullable = false)
    private ReceiptEntity receipt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AllocationStatus status = AllocationStatus.DRAFT;

    @Column(columnDefinition = "TEXT")
    private String notes;

    // NEW: Relationship to allocation details
    @OneToMany(mappedBy = "allocation", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<ReceiptAllocationDetailEntity> allocationDetails = new ArrayList<>();

    // Workflow tracking
    @Column(name = "requested_by", nullable = false)
    private UUID requestedBy;

    @CreationTimestamp
    @Column(name = "requested_at", nullable = false)
    private LocalDateTime requestedAt;

    @Column(name = "approved_by")
    private UUID approvedBy;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "approval_notes", columnDefinition = "TEXT")
    private String approvalNotes;

    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // ==========================================
    // BUSINESS LOGIC METHODS
    // ==========================================

    public boolean canEdit() {
        return status == AllocationStatus.DRAFT;
    }

    public boolean canApprove() {
        return status == AllocationStatus.PENDING_APPROVAL;
    }

    public boolean canReject() {
        return status == AllocationStatus.PENDING_APPROVAL;
    }

    public boolean canCancel() {
        return status == AllocationStatus.DRAFT || status == AllocationStatus.PENDING_APPROVAL;
    }

    public boolean isApproved() {
        return status == AllocationStatus.APPROVED;
    }

    public boolean isPending() {
        return status == AllocationStatus.PENDING_APPROVAL;
    }

    public boolean isDraft() {
        return status == AllocationStatus.DRAFT;
    }

    // ==========================================
    // NEW: ALLOCATION DETAIL METHODS
    // ==========================================

    public BigDecimal getTotalAllocatedAmount() {
        return allocationDetails.stream()
                .map(ReceiptAllocationDetailEntity::getAllocatedAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public boolean isFullyAllocated() {
        BigDecimal receiptAmount = receipt != null ? receipt.getTotalAmount() : BigDecimal.ZERO;
        return getTotalAllocatedAmount().compareTo(receiptAmount) == 0;
    }

    public boolean isOverAllocated() {
        BigDecimal receiptAmount = receipt != null ? receipt.getTotalAmount() : BigDecimal.ZERO;
        return getTotalAllocatedAmount().compareTo(receiptAmount) > 0;
    }

    public BigDecimal getRemainingAmount() {
        BigDecimal receiptAmount = receipt != null ? receipt.getTotalAmount() : BigDecimal.ZERO;
        return receiptAmount.subtract(getTotalAllocatedAmount());
    }

    public int getAllocationDetailCount() {
        return allocationDetails != null ? allocationDetails.size() : 0;
    }

    public void addAllocationDetail(ReceiptAllocationDetailEntity detail) {
        if (allocationDetails == null) {
            allocationDetails = new ArrayList<>();
        }
        allocationDetails.add(detail);
        detail.setAllocation(this);
    }

    public void removeAllocationDetail(ReceiptAllocationDetailEntity detail) {
        if (allocationDetails != null) {
            allocationDetails.remove(detail);
            detail.setAllocation(null);
        }
    }
}