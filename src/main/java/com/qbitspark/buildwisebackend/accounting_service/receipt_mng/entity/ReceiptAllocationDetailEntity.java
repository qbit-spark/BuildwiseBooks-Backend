package com.qbitspark.buildwisebackend.accounting_service.receipt_mng.entity;

import com.qbitspark.buildwisebackend.accounting_service.budget_mng.org_budget.entity.OrgBudgetDetailAllocationEntity;
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
import java.util.UUID;

@Entity
@Table(name = "receipt_allocation_details")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ReceiptAllocationDetailEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID detailId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "allocation_id", nullable = false)
    private ReceiptAllocationEntity allocation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "detail_account_id", nullable = false)
    private OrgBudgetDetailAllocationEntity detailAccount;

    @Column(precision = 18, scale = 2, nullable = false)
    private BigDecimal amount;

    @Column(length = 500)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AllocationStatus status = AllocationStatus.DRAFT;

    @Column(name = "created_by")
    private UUID createdBy;

    @Column(name = "approved_by")
    private UUID approvedBy;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // ==========================================
    // BUSINESS LOGIC METHODS
    // ==========================================

    /**
     * Check if this allocation detail counts in budget calculations
     * Only approved allocations count
     */
    public boolean countsInCalculations() {
        return status == AllocationStatus.APPROVED;
    }

    /**
     * Check if this allocation detail is still pending approval
     */
    public boolean isPending() {
        return status == AllocationStatus.DRAFT;
    }

    /**
     * Check if this allocation detail is approved
     */
    public boolean isApproved() {
        return status == AllocationStatus.APPROVED;
    }

    /**
     * Check if this allocation detail is cancelled
     */
    public boolean isCancelled() {
        return status == AllocationStatus.CANCELLED;
    }

    /**
     * Check if this allocation detail can be edited
     * Only draft allocations can be edited
     */
    public boolean canEdit() {
        return status == AllocationStatus.DRAFT;
    }

    /**
     * Check if this allocation detail can be approved
     * Only draft allocations can be approved
     */
    public boolean canApprove() {
        return status == AllocationStatus.DRAFT;
    }

    /**
     * Approve this allocation detail
     */
    public void approve(UUID approvedByUserId) {
        if (!canApprove()) {
            throw new IllegalStateException("Cannot approve allocation detail with status: " + status);
        }
        this.status = AllocationStatus.APPROVED;
        this.approvedBy = approvedByUserId;
        this.approvedAt = LocalDateTime.now();
    }

    /**
     * Cancel this allocation detail
     */
    public void cancel() {
        this.status = AllocationStatus.CANCELLED;
    }

    /**
     * Reset this allocation detail back to draft
     * Can only be done if currently approved and not yet processed in budget
     */
    public void resetToDraft() {
        if (status != AllocationStatus.APPROVED) {
            throw new IllegalStateException("Cannot reset allocation detail with status: " + status);
        }
        this.status = AllocationStatus.DRAFT;
        this.approvedBy = null;
        this.approvedAt = null;
    }

    /**
     * Get the receipt this allocation detail belongs to
     */
    public ReceiptEntity getReceipt() {
        return allocation != null ? allocation.getReceipt() : null;
    }

    /**
     * Get the organization this allocation detail belongs to
     */
    public UUID getOrganisationId() {
        ReceiptEntity receipt = getReceipt();
        return receipt != null && receipt.getOrganisation() != null ?
                receipt.getOrganisation().getOrganisationId() : null;
    }



    /**
     * Get days since creation
     */
    public long getDaysSinceCreation() {
        return java.time.Duration.between(createdAt, LocalDateTime.now()).toDays();
    }

    /**
     * Get days since approval (if approved)
     */
    public Long getDaysSinceApproval() {
        if (approvedAt == null) {
            return null;
        }
        return java.time.Duration.between(approvedAt, LocalDateTime.now()).toDays();
    }
}