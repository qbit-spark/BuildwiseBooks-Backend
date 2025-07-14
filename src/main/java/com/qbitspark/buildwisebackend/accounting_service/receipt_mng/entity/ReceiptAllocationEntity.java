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
    // WORKFLOW ACTIONS
    // ==========================================

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

    public String getStatusDescription() {
        return switch (status) {
            case DRAFT -> "Draft - Being prepared";
            case PENDING_APPROVAL -> "Pending approval from finance team";
            case APPROVED -> "Approved - Budget has been funded";
            case REJECTED -> "Rejected - " + (rejectionReason != null ? rejectionReason : "No reason provided");
            case CANCELLED -> "Cancelled by requester";
        };
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