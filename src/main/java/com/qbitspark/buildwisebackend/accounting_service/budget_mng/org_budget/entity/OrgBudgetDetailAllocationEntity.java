package com.qbitspark.buildwisebackend.accounting_service.budget_mng.org_budget.entity;

import com.qbitspark.buildwisebackend.accounting_service.coa.entity.ChartOfAccounts;
import com.qbitspark.buildwisebackend.accounting_service.receipt_mng.entity.ReceiptAllocationDetailEntity;
import com.qbitspark.buildwisebackend.accounting_service.receipt_mng.enums.AllocationStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "org_budget_detail_allocation")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class OrgBudgetDetailAllocationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID allocationId;

    @ManyToOne
    @JoinColumn(name = "header_line_item_id", nullable = false)
    private OrgBudgetLineItemEntity headerLineItem;

    @ManyToOne
    @JoinColumn(name = "detail_account_id", nullable = false)
    private ChartOfAccounts detailAccount;

    @Column(precision = 18, scale = 2, nullable = false)
    private BigDecimal allocatedAmount = BigDecimal.ZERO;

    @Column(precision = 18, scale = 2, nullable = false)
    private BigDecimal spentAmount = BigDecimal.ZERO;

    @Column(precision = 18, scale = 2, nullable = false)
    private BigDecimal committedAmount = BigDecimal.ZERO;

    @Column(name = "allocation_notes", columnDefinition = "TEXT")
    private String allocationNotes;

    @Column(nullable = false)
    private LocalDateTime createdDate;

    private LocalDateTime modifiedDate;

    private UUID createdBy;

    private UUID modifiedBy;

    // New relationship to track receipt allocations
    @OneToMany(mappedBy = "detailAccount", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<ReceiptAllocationDetailEntity> receiptAllocations = new ArrayList<>();

    // ==========================================
    // BUDGET CALCULATION METHODS
    // ==========================================

    /**
     * Get remaining amount available for allocation
     */
    public BigDecimal getRemainingAmount() {
        return allocatedAmount.subtract(spentAmount).subtract(committedAmount);
    }

    /**
     * Check if we can spend the specified amount from this allocation
     */
    public boolean canSpend(BigDecimal amount) {
        return getRemainingAmount().compareTo(amount) >= 0;
    }

    /**
     * Check if this allocation has any budget allocated
     */
    public boolean hasAllocation() {
        return allocatedAmount.compareTo(BigDecimal.ZERO) > 0;
    }

    /**
     * Get utilization percentage (spent + committed / allocated * 100)
     */
    public BigDecimal getUtilizationPercentage() {
        if (allocatedAmount.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return spentAmount.add(committedAmount)
                .multiply(BigDecimal.valueOf(100))
                .divide(allocatedAmount, 2, RoundingMode.HALF_UP);
    }

    /**
     * Add to spent amount
     */
    public void addSpentAmount(BigDecimal amount) {
        if (amount != null && amount.compareTo(BigDecimal.ZERO) > 0) {
            this.spentAmount = this.spentAmount.add(amount);
        }
    }

    /**
     * Add to committed amount
     */
    public void addCommittedAmount(BigDecimal amount) {
        if (amount != null && amount.compareTo(BigDecimal.ZERO) > 0) {
            this.committedAmount = this.committedAmount.add(amount);
        }
    }

    /**
     * Subtract from committed amount
     */
    public void subtractCommittedAmount(BigDecimal amount) {
        if (amount != null && amount.compareTo(BigDecimal.ZERO) > 0) {
            this.committedAmount = this.committedAmount.subtract(amount);
            if (this.committedAmount.compareTo(BigDecimal.ZERO) < 0) {
                this.committedAmount = BigDecimal.ZERO;
            }
        }
    }

    /**
     * Check if this allocation is over-allocated
     */
    public boolean isOverAllocated() {
        return spentAmount.add(committedAmount).compareTo(allocatedAmount) > 0;
    }

    // ==========================================
    // RECEIPT ALLOCATION METHODS (NEW SYSTEM)
    // ==========================================

    /**
     * Get total amount from all APPROVED receipt allocations
     */
    public BigDecimal getTotalApprovedReceiptAllocations() {
        return receiptAllocations.stream()
                .filter(allocation -> allocation.getStatus() == AllocationStatus.APPROVED)
                .map(ReceiptAllocationDetailEntity::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Get total amount from all PENDING receipt allocations
     */
    public BigDecimal getTotalPendingReceiptAllocations() {
        return receiptAllocations.stream()
                .filter(allocation -> allocation.getStatus() == AllocationStatus.DRAFT)
                .map(ReceiptAllocationDetailEntity::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Get total amount from all receipt allocations (approved + pending)
     */
    public BigDecimal getTotalReceiptAllocations() {
        return receiptAllocations.stream()
                .map(ReceiptAllocationDetailEntity::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Get unfunded amount (allocated - approved receipt allocations)
     */
    public BigDecimal getUnfundedAmount() {
        return allocatedAmount.subtract(getTotalApprovedReceiptAllocations());
    }

    /**
     * Get funding percentage (approved receipt allocations / allocated * 100)
     */
    public BigDecimal getFundingPercentage() {
        if (allocatedAmount.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return getTotalApprovedReceiptAllocations()
                .multiply(BigDecimal.valueOf(100))
                .divide(allocatedAmount, 2, RoundingMode.HALF_UP);
    }

    /**
     * Check if we can allocate the specified amount from receipts
     */
    public boolean canAllocateFromReceipts(BigDecimal amount) {
        return getUnfundedAmount().compareTo(amount) >= 0;
    }

    /**
     * Get count of approved receipt allocations
     */
    public int getApprovedReceiptAllocationCount() {
        return (int) receiptAllocations.stream()
                .filter(allocation -> allocation.getStatus() == AllocationStatus.APPROVED)
                .count();
    }

    /**
     * Get count of pending receipt allocations
     */
    public int getPendingReceiptAllocationCount() {
        return (int) receiptAllocations.stream()
                .filter(allocation -> allocation.getStatus() == AllocationStatus.DRAFT)
                .count();
    }

    // ==========================================
    // STATUS AND DESCRIPTION METHODS
    // ==========================================

    /**
     * Get funding status description
     */
    public String getFundingStatus() {
        BigDecimal approvedAllocations = getTotalApprovedReceiptAllocations();
        if (approvedAllocations.compareTo(BigDecimal.ZERO) == 0) {
            return "Unfunded";
        }
        if (approvedAllocations.compareTo(allocatedAmount) >= 0) {
            return "Fully Funded";
        }
        return "Partially Funded";
    }

    /**
     * Get allocation status description
     */
    public String getAllocationStatus() {
        if (allocatedAmount.compareTo(BigDecimal.ZERO) == 0) {
            return "No Allocation";
        }

        BigDecimal utilization = getUtilizationPercentage();

        if (utilization.compareTo(BigDecimal.valueOf(100)) > 0) {
            return "Over Allocated";
        } else if (utilization.compareTo(BigDecimal.valueOf(90)) > 0) {
            return "Near Limit";
        } else if (utilization.compareTo(BigDecimal.valueOf(50)) > 0) {
            return "Active";
        } else {
            return "Available";
        }
    }

    // ==========================================
    // ACCOUNT INFORMATION METHODS
    // ==========================================

    /**
     * Get header account name
     */
    public String getHeaderAccountName() {
        return headerLineItem != null && headerLineItem.getChartOfAccount() != null
                ? headerLineItem.getChartOfAccount().getName()
                : "Unknown Header";
    }

    /**
     * Get header account code
     */
    public String getHeaderAccountCode() {
        return headerLineItem != null && headerLineItem.getChartOfAccount() != null
                ? headerLineItem.getChartOfAccount().getAccountCode()
                : "N/A";
    }

    /**
     * Get detail account name
     */
    public String getDetailAccountName() {
        return detailAccount != null ? detailAccount.getName() : "Unknown Detail Account";
    }

    /**
     * Get detail account code
     */
    public String getDetailAccountCode() {
        return detailAccount != null ? detailAccount.getAccountCode() : "N/A";
    }

    /**
     * Get account name for display purposes
     */
    public String getAccountName() {
        return getDetailAccountName();
    }

    /**
     * Get account code for display purposes
     */
    public String getAccountCode() {
        return getDetailAccountCode();
    }

    // ==========================================
    // SUMMARY METHODS
    // ==========================================

    /**
     * Get comprehensive allocation summary
     */
    public String getAllocationSummary() {
        return String.format(
                "Allocated: %s, Spent: %s, Committed: %s, Remaining: %s, Funded: %s (%s)",
                allocatedAmount,
                spentAmount,
                committedAmount,
                getRemainingAmount(),
                getTotalApprovedReceiptAllocations(),
                getFundingStatus()
        );
    }

    /**
     * Check if this allocation has any activity (spending, commitments, or receipt allocations)
     */
    public boolean hasActivity() {
        return spentAmount.compareTo(BigDecimal.ZERO) > 0 ||
                committedAmount.compareTo(BigDecimal.ZERO) > 0 ||
                !receiptAllocations.isEmpty();
    }
}