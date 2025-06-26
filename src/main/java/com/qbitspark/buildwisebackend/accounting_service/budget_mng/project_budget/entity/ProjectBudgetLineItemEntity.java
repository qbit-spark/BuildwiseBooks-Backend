package com.qbitspark.buildwisebackend.accounting_service.budget_mng.project_budget.entity;

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
@Table(name = "project_budget_line_item")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ProjectBudgetLineItemEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID lineItemId;

    @ManyToOne
    @JoinColumn(name = "project_budget_id", nullable = false)
    private ProjectBudgetEntity projectBudget;

    @ManyToOne
    @JoinColumn(name = "chart_of_account_id", nullable = false)
    private ChartOfAccounts chartOfAccount;


    @Column(precision = 18, scale = 2, nullable = false)
    private BigDecimal budgetAmount = BigDecimal.ZERO;


    @Column(precision = 18, scale = 2, nullable = false)
    private BigDecimal spentAmount = BigDecimal.ZERO;


    @Column(precision = 18, scale = 2, nullable = false)
    private BigDecimal committedAmount = BigDecimal.ZERO;

    private String lineItemNotes;

    @Column(nullable = false)
    private LocalDateTime createdDate;

    private LocalDateTime modifiedDate;


    public BigDecimal getRemainingAmount() {
        return budgetAmount.subtract(spentAmount).subtract(committedAmount);
    }

    public boolean canSpend(BigDecimal amount) {
        return getRemainingAmount().compareTo(amount) >= 0;
    }
}