package com.qbitspark.buildwisebackend.accounting_service.budget_mng.org_budget.entity;

import com.qbitspark.buildwisebackend.accounting_service.coa.entity.ChartOfAccounts;
import com.qbitspark.buildwisebackend.accounting_service.receipt_mng.entity.ReceiptAllocationFundingEntity;
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


    public BigDecimal getRemainingAmount() {
        return allocatedAmount.subtract(spentAmount).subtract(committedAmount);
    }


    public boolean canSpend(BigDecimal amount) {
        return getRemainingAmount().compareTo(amount) >= 0;
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


    public boolean isOverAllocated() {
        return spentAmount.add(committedAmount).compareTo(allocatedAmount) > 0;
    }

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


    public static BigDecimal calculateTotalFunding(List<ReceiptAllocationFundingEntity> fundings) {
        return fundings.stream()
                .map(ReceiptAllocationFundingEntity::getFundedAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}