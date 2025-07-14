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

    // NEW: Relationship to track receipt funding - will be populated by new allocation system
    @OneToMany(mappedBy = "budgetDetailAllocation", fetch = FetchType.LAZY)
    private List<ReceiptAllocationDetailEntity> fundingSources = new ArrayList<>();

    // ==========================================
    // BUDGET CALCULATION METHODS
    // ==========================================

    public BigDecimal getRemainingAmount() {
        return allocatedAmount.subtract(spentAmount).subtract(committedAmount);
    }

    public boolean hasAllocation() {
        return allocatedAmount.compareTo(BigDecimal.ZERO) > 0;
    }

    public BigDecimal getUtilizationPercentage() {
        if (allocatedAmount.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return spentAmount.add(committedAmount)
                .multiply(BigDecimal.valueOf(100))
                .divide(allocatedAmount, 2, RoundingMode.HALF_UP);
    }

    public void addSpentAmount(BigDecimal amount) {
        if (amount != null && amount.compareTo(BigDecimal.ZERO) > 0) {
            this.spentAmount = this.spentAmount.add(amount);
        }
    }

    public void addCommittedAmount(BigDecimal amount) {
        if (amount != null && amount.compareTo(BigDecimal.ZERO) > 0) {
            this.committedAmount = this.committedAmount.add(amount);
        }
    }

    public void subtractCommittedAmount(BigDecimal amount) {
        if (amount != null && amount.compareTo(BigDecimal.ZERO) > 0) {
            this.committedAmount = this.committedAmount.subtract(amount);
            if (this.committedAmount.compareTo(BigDecimal.ZERO) < 0) {
                this.committedAmount = BigDecimal.ZERO;
            }
        }
    }

    public boolean isOverAllocated() {
        return spentAmount.add(committedAmount).compareTo(allocatedAmount) > 0;
    }

    // ==========================================
    // NEW FUNDING SYSTEM METHODS
    // ==========================================

    /**
     * Get total amount funded by APPROVED receipt allocations.
     * This is the actual cash available for spending from receipts.
     */
    public BigDecimal getFundedAmount() {
        return fundingSources.stream()
                .filter(source -> source.getAllocation().getStatus() == AllocationStatus.APPROVED)
                .map(ReceiptAllocationDetailEntity::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Get total amount from PENDING receipt allocations.
     * This money is requested but not yet approved for funding.
     */
    public BigDecimal getPendingFunding() {
        return fundingSources.stream()
                .filter(source -> source.getAllocation().getStatus() == AllocationStatus.PENDING_APPROVAL)
                .map(ReceiptAllocationDetailEntity::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * CRITICAL: This is the amount available for actual spending.
     * Only funded amounts (from approved receipts) can be spent.
     */
    public BigDecimal getAvailableToSpend() {
        return getFundedAmount().subtract(spentAmount).subtract(committedAmount);
    }

    /**
     * Amount of budget that has been allocated but not yet funded by receipts.
     * This represents unfunded budget that cannot be spent yet.
     */
    public BigDecimal getUnfundedBudget() {
        return allocatedAmount.subtract(getFundedAmount());
    }

    /**
     * CRITICAL: Check if we can spend the specified amount.
     * This validates against funded amount, not allocated amount.
     */
    public boolean canSpend(BigDecimal amount) {
        return getAvailableToSpend().compareTo(amount) >= 0;
    }

    public BigDecimal getFundingPercentage() {
        if (allocatedAmount.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return getFundedAmount()
                .multiply(BigDecimal.valueOf(100))
                .divide(allocatedAmount, 2, RoundingMode.HALF_UP);
    }

    public String getFundingStatus() {
        BigDecimal funded = getFundedAmount();
        if (funded.compareTo(BigDecimal.ZERO) == 0) {
            return "Unfunded";
        }
        if (funded.compareTo(allocatedAmount) >= 0) {
            return "Fully Funded";
        }
        return "Partially Funded";
    }

    public int getApprovedFundingSourcesCount() {
        return (int) fundingSources.stream()
                .filter(source -> source.getAllocation().getStatus() == AllocationStatus.APPROVED)
                .count();
    }

    public int getPendingFundingSourcesCount() {
        return (int) fundingSources.stream()
                .filter(source -> source.getAllocation().getStatus() == AllocationStatus.PENDING_APPROVAL)
                .count();
    }

    // ==========================================
    // ACCOUNT INFORMATION METHODS
    // ==========================================

    public String getHeaderAccountName() {
        return headerLineItem != null && headerLineItem.getChartOfAccount() != null
                ? headerLineItem.getChartOfAccount().getName()
                : "Unknown Header";
    }

    public String getHeaderAccountCode() {
        return headerLineItem != null && headerLineItem.getChartOfAccount() != null
                ? headerLineItem.getChartOfAccount().getAccountCode()
                : "N/A";
    }

    public String getDetailAccountName() {
        return detailAccount != null ? detailAccount.getName() : "Unknown Detail Account";
    }

    public String getDetailAccountCode() {
        return detailAccount != null ? detailAccount.getAccountCode() : "N/A";
    }

    public String getAccountName() {
        return getDetailAccountName();
    }

    public String getAccountCode() {
        return getDetailAccountCode();
    }

    // ==========================================
    // STATUS AND VALIDATION METHODS
    // ==========================================

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

    public String getAllocationSummary() {
        return String.format(
                "Allocated: %s, Funded: %s, Spent: %s, Committed: %s, Available: %s (%s)",
                allocatedAmount,
                getFundedAmount(),
                spentAmount,
                committedAmount,
                getAvailableToSpend(),
                getFundingStatus()
        );
    }

    public boolean hasActivity() {
        return spentAmount.compareTo(BigDecimal.ZERO) > 0 ||
                committedAmount.compareTo(BigDecimal.ZERO) > 0 ||
                !fundingSources.isEmpty();
    }

    /**
     * Check if this allocation is over-funded (funded > allocated).
     * This shouldn't normally happen but can occur if allocations are reduced after funding.
     */
    public boolean isOverFunded() {
        return getFundedAmount().compareTo(allocatedAmount) > 0;
    }
}