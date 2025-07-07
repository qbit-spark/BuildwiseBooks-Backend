package com.qbitspark.buildwisebackend.accounting_service.budget_mng.org_budget.entity;

import com.qbitspark.buildwisebackend.accounting_service.coa.entity.ChartOfAccounts;
import com.qbitspark.buildwisebackend.accounting_service.receipt_mng.entity.ReceiptAllocationFundingEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
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

    // Reference to header account line item (parent)
    @ManyToOne
    @JoinColumn(name = "header_line_item_id", nullable = false)
    private OrgBudgetLineItemEntity headerLineItem;

    // Reference to detail account (child) - using your preferred naming
    @ManyToOne
    @JoinColumn(name = "detail_account_id", nullable = false)
    private ChartOfAccounts detailAccount;

    // Real money allocated to this detail account
    @Column(precision = 18, scale = 2, nullable = false)
    private BigDecimal allocatedAmount = BigDecimal.ZERO;

    // Money spent from this allocation (from vouchers)
    @Column(precision = 18, scale = 2, nullable = false)
    private BigDecimal spentAmount = BigDecimal.ZERO;

    // Money committed but not yet spent (from pending vouchers)
    @Column(precision = 18, scale = 2, nullable = false)
    private BigDecimal committedAmount = BigDecimal.ZERO;

    @Column(name = "allocation_notes", columnDefinition = "TEXT")
    private String allocationNotes;

    @Column(nullable = false)
    private LocalDateTime createdDate;

    private LocalDateTime modifiedDate;

    private UUID createdBy;

    private UUID modifiedBy;

    // Business methods

    /**
     * Get remaining amount available for spending
     */
    public BigDecimal getRemainingAmount() {
        return allocatedAmount.subtract(spentAmount).subtract(committedAmount);
    }

    /**
     * Check if this allocation can cover the requested amount
     */
    public boolean canSpend(BigDecimal amount) {
        return getRemainingAmount().compareTo(amount) >= 0;
    }

    /**
     * Check if any money has been allocated to this detail account
     */
    public boolean hasAllocation() {
        return allocatedAmount.compareTo(BigDecimal.ZERO) > 0;
    }

    /**
     * Get utilization percentage of this allocation
     */
    public BigDecimal getUtilizationPercentage() {
        if (allocatedAmount.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return spentAmount.add(committedAmount)
                .multiply(BigDecimal.valueOf(100))
                .divide(allocatedAmount, 2, BigDecimal.ROUND_HALF_UP);
    }

    /**
     * Add spent amount (when voucher is paid)
     */
    public void addSpentAmount(BigDecimal amount) {
        if (amount != null && amount.compareTo(BigDecimal.ZERO) > 0) {
            this.spentAmount = this.spentAmount.add(amount);
        }
    }

    /**
     * Add committed amount (when voucher is created/approved)
     */
    public void addCommittedAmount(BigDecimal amount) {
        if (amount != null && amount.compareTo(BigDecimal.ZERO) > 0) {
            this.committedAmount = this.committedAmount.add(amount);
        }
    }

    /**
     * Remove committed amount (when voucher is paid or cancelled)
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
     * Get the header account information
     */
    public String getHeaderAccountName() {
        return headerLineItem != null && headerLineItem.getChartOfAccount() != null
                ? headerLineItem.getChartOfAccount().getName()
                : "Unknown Header";
    }

    /**
     * Get the header account code
     */
    public String getHeaderAccountCode() {
        return headerLineItem != null && headerLineItem.getChartOfAccount() != null
                ? headerLineItem.getChartOfAccount().getAccountCode()
                : "N/A";
    }

    /**
     * Get the detail account name
     */
    public String getDetailAccountName() {
        return detailAccount != null ? detailAccount.getName() : "Unknown Detail Account";
    }

    /**
     * Get the detail account code
     */
    public String getDetailAccountCode() {
        return detailAccount != null ? detailAccount.getAccountCode() : "N/A";
    }

    /**
     * Check if allocation is over-utilized (spent + committed > allocated)
     */
    public boolean isOverAllocated() {
        return spentAmount.add(committedAmount).compareTo(allocatedAmount) > 0;
    }

    /**
     * Get allocation status
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

    @OneToMany(mappedBy = "allocation", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<ReceiptAllocationFundingEntity> receivedFundings = new ArrayList<>();

    public BigDecimal getTotalFundingReceived() {
        return receivedFundings.stream()
                .map(ReceiptAllocationFundingEntity::getFundedAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal getUnfundedAmount() {
        return allocatedAmount.subtract(getTotalFundingReceived());
    }

    public String getFundingStatus() {
        BigDecimal funded = getTotalFundingReceived();
        if (funded.compareTo(BigDecimal.ZERO) == 0) return "Unfunded";
        if (funded.compareTo(allocatedAmount) >= 0) return "Fully Funded";
        return "Partially Funded";
    }

    // Helper method to calculate from repo list
    public static BigDecimal calculateTotalFunding(List<ReceiptAllocationFundingEntity> fundings) {
        return fundings.stream()
                .map(ReceiptAllocationFundingEntity::getFundedAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}