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
import java.math.RoundingMode;
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

    @Column(length = 1000)
    private String notes;

    @Column(nullable = false)
    private UUID createdBy;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "allocation", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<ReceiptAllocationDetailEntity> detailAllocations = new ArrayList<>();

    // ==========================================
    // BUSINESS LOGIC METHODS
    // ==========================================

    /**
     * Get total amount from all detail allocations (approved + draft)
     */
    public BigDecimal getTotalAllocatedAmount() {
        return detailAllocations.stream()
                .map(ReceiptAllocationDetailEntity::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Get total amount from only APPROVED detail allocations
     */
    public BigDecimal getTotalApprovedAmount() {
        return detailAllocations.stream()
                .filter(detail -> detail.getStatus() == AllocationStatus.APPROVED)
                .map(ReceiptAllocationDetailEntity::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Get total amount from only DRAFT detail allocations
     */
    public BigDecimal getTotalPendingAmount() {
        return detailAllocations.stream()
                .filter(detail -> detail.getStatus() == AllocationStatus.DRAFT)
                .map(ReceiptAllocationDetailEntity::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Get count of total detail allocations
     */
    public int getTotalDetailCount() {
        return detailAllocations.size();
    }

    /**
     * Get count of approved detail allocations
     */
    public int getApprovedDetailCount() {
        return (int) detailAllocations.stream()
                .filter(detail -> detail.getStatus() == AllocationStatus.APPROVED)
                .count();
    }

    /**
     * Get count of pending detail allocations
     */
    public int getPendingDetailCount() {
        return (int) detailAllocations.stream()
                .filter(detail -> detail.getStatus() == AllocationStatus.DRAFT)
                .count();
    }

    /**
     * Check if all detail allocations are approved
     */
    public boolean isFullyApproved() {
        return !detailAllocations.isEmpty() &&
                detailAllocations.stream().allMatch(detail -> detail.getStatus() == AllocationStatus.APPROVED);
    }

    /**
     * Check if any detail allocations are approved
     */
    public boolean hasApprovedDetails() {
        return detailAllocations.stream()
                .anyMatch(detail -> detail.getStatus() == AllocationStatus.APPROVED);
    }

    /**
     * Check if all detail allocations are still pending
     */
    public boolean isAllPending() {
        return !detailAllocations.isEmpty() &&
                detailAllocations.stream().allMatch(detail -> detail.getStatus() == AllocationStatus.DRAFT);
    }

    /**
     * Check if allocation group can be edited (has any pending details)
     */
    public boolean canEdit() {
        return detailAllocations.stream()
                .anyMatch(detail -> detail.getStatus() == AllocationStatus.DRAFT);
    }

    /**
     * Approve all pending detail allocations in this group
     */
    public void approveAllDetails(UUID approvedByUserId) {
        detailAllocations.stream()
                .filter(detail -> detail.getStatus() == AllocationStatus.DRAFT)
                .forEach(detail -> detail.approve(approvedByUserId));
    }

    /**
     * Cancel all pending detail allocations in this group
     */
    public void cancelAllPendingDetails() {
        detailAllocations.stream()
                .filter(detail -> detail.getStatus() == AllocationStatus.DRAFT)
                .forEach(detail -> detail.setStatus(AllocationStatus.CANCELLED));
    }

    /**
     * Get approval progress percentage (approved details / total details * 100)
     */
    public BigDecimal getApprovalProgress() {
        if (detailAllocations.isEmpty()) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(getApprovedDetailCount())
                .divide(BigDecimal.valueOf(getTotalDetailCount()), 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
    }
}
