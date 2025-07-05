package com.qbitspark.buildwisebackend.accounting_service.budget_mng.org_budget.entity;

import com.qbitspark.buildwisebackend.accounting_service.coa.entity.ChartOfAccounts;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "org_budget_line_item")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class OrgBudgetLineItemEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID lineItemId;

    @ManyToOne
    @JoinColumn(name = "org_budget_id", nullable = false)
    private OrgBudgetEntity orgBudget;

    @ManyToOne
    @JoinColumn(name = "chart_of_account_id", nullable = false)
    private ChartOfAccounts chartOfAccount;

    @Column(precision = 18, scale = 2, nullable = false)
    private BigDecimal budgetAmount = BigDecimal.ZERO;

    @Column(precision = 18, scale = 2, nullable = false)
    private BigDecimal spentAmount = BigDecimal.ZERO;

    @Column(precision = 18, scale = 2, nullable = false)
    private BigDecimal committedAmount = BigDecimal.ZERO;

    @Column(name = "line_item_notes", columnDefinition = "TEXT")
    private String lineItemNotes;

    @Column(nullable = false)
    private LocalDateTime createdDate;

    private LocalDateTime modifiedDate;

    private UUID createdBy;

    private UUID modifiedBy;

    // Business methods
    public BigDecimal getRemainingAmount() {
        return budgetAmount.subtract(spentAmount).subtract(committedAmount);
    }

    public boolean canSpend(BigDecimal amount) {
        return getRemainingAmount().compareTo(amount) >= 0;
    }

    public boolean hasBudgetAllocated() {
        return budgetAmount.compareTo(BigDecimal.ZERO) > 0;
    }

    public BigDecimal getUtilizationPercentage() {
        if (budgetAmount.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return spentAmount.add(committedAmount)
                .multiply(BigDecimal.valueOf(100))
                .divide(budgetAmount, 2, BigDecimal.ROUND_HALF_UP);
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
}