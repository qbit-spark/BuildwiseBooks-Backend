package com.qbitspark.buildwisebackend.accounting_service.receipt_mng.entity;

import com.qbitspark.buildwisebackend.accounting_service.receipt_mng.entity.ReceiptAllocationDetailEntity;
import com.qbitspark.buildwisebackend.accounting_service.receipt_mng.entity.ReceiptEntity;
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

import static com.qbitspark.buildwisebackend.accounting_service.receipt_mng.enums.AllocationStatus.*;

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

    @Column(columnDefinition = "TEXT")
    private String justification;

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

    @OneToMany(mappedBy = "allocation", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private List<ReceiptAllocationDetailEntity> details = new ArrayList<>();

    // ==========================================
    // BUSINESS LOGIC METHODS
    // ==========================================

    public BigDecimal getTotalAllocatedAmount() {
        return details.stream()
                .map(ReceiptAllocationDetailEntity::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public int getDetailCount() {
        return details.size();
    }

    public boolean canEdit() {
        return status == AllocationStatus.DRAFT;
    }

    public boolean canSubmitForApproval() {
        return status == AllocationStatus.DRAFT && !details.isEmpty() &&
                getTotalAllocatedAmount().compareTo(BigDecimal.ZERO) > 0;
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
    // WORKFLOW ACTIONS
    // ==========================================

    public void submitForApproval() {
        if (!canSubmitForApproval()) {
            throw new IllegalStateException("Cannot submit allocation for approval in status: " + status);
        }

        this.status = AllocationStatus.PENDING_APPROVAL;
    }

    public void approve(UUID approvedByUserId, String notes) {
        if (!canApprove()) {
            throw new IllegalStateException("Cannot approve allocation in status: " + status);
        }

        this.status = AllocationStatus.APPROVED;
        this.approvedBy = approvedByUserId;
        this.approvedAt = LocalDateTime.now();
        this.approvalNotes = notes;
    }

    public void reject(UUID rejectedByUserId, String reason) {
        if (!canReject()) {
            throw new IllegalStateException("Cannot reject allocation in status: " + status);
        }

        this.status = AllocationStatus.REJECTED;
        this.approvedBy = rejectedByUserId;
        this.approvedAt = LocalDateTime.now();
        this.rejectionReason = reason;
    }

    public void cancel() {
        if (!canCancel()) {
            throw new IllegalStateException("Cannot cancel allocation in status: " + status);
        }

        this.status = AllocationStatus.CANCELLED;
    }

    // ==========================================
    // VALIDATION METHODS
    // ==========================================

    /**
     * Validates that total allocation doesn't exceed receipt amount.
     * This is a critical business rule validation.
     */
    public void validateAllocationAmount() {
        BigDecimal receiptAmount = receipt.getTotalAmount();
        BigDecimal totalAllocated = getTotalAllocatedAmount();

        if (totalAllocated.compareTo(receiptAmount) > 0) {
            throw new IllegalArgumentException(String.format(
                    "Total allocation (%s) cannot exceed receipt amount (%s)",
                    totalAllocated, receiptAmount
            ));
        }
    }

    /**
     * Validates that receipt can accommodate new allocation considering existing allocations.
     */
    public boolean canAllocateAmount(BigDecimal amount) {
        // Get other allocations for same receipt (excluding this one)
        BigDecimal otherAllocations = receipt.getFundingAllocations().stream()
                .filter(alloc -> !alloc.getAllocationId().equals(this.allocationId))
                .flatMap(alloc -> alloc.getDetails().stream())
                .map(ReceiptAllocationDetailEntity::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalWithNewAmount = otherAllocations.add(amount);
        return totalWithNewAmount.compareTo(receipt.getTotalAmount()) <= 0;
    }

    public String getStatusDescription() {
        return switch (status) {
            case DRAFT -> "Draft - Being prepared";
            case PENDING_APPROVAL -> "Pending approval from finance team";
            case APPROVED -> "Approved - Budget has been funded";
            case REJECTED -> "Rejected - " + (rejectionReason != null ? rejectionReason : "No reason provided");
            case CANCELLED -> "Cancelled by requester";
        };
    }

    public String getAllocationSummary() {
        return String.format(
                "Allocation %s: %s details totaling %s (%s)",
                allocationId.toString().substring(0, 8),
                details.size(),
                getTotalAllocatedAmount(),
                status
        );
    }

    public long getDaysSinceCreation() {
        return java.time.Duration.between(createdAt, LocalDateTime.now()).toDays();
    }

    public Long getDaysSinceApproval() {
        if (approvedAt == null) {
            return null;
        }
        return java.time.Duration.between(approvedAt, LocalDateTime.now()).toDays();
    }
}