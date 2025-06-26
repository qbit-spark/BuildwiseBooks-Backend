package com.qbitspark.buildwisebackend.accounting_service.budget_mng.project_budget.entity;

import com.qbitspark.buildwisebackend.accounting_service.budget_mng.org_budget.entity.OrgBudgetEntity;
import com.qbitspark.buildwisebackend.accounting_service.budget_mng.project_budget.enums.ProjectBudgetStatus;
import com.qbitspark.buildwisebackend.accounting_service.coa.entity.ChartOfAccounts;
import com.qbitspark.buildwisebackend.projectmng_service.entity.ProjectEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "project_budget")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ProjectBudgetEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID projectBudgetId;

    @ManyToOne
    @JoinColumn(name = "org_budget_id", nullable = false)
    private OrgBudgetEntity orgBudget;


    @ManyToOne
    @JoinColumn(name = "project_id", nullable = false)
    private ProjectEntity project;


    @ManyToOne
    @JoinColumn(name = "chart_of_account_id", nullable = false)
    private ChartOfAccounts chartOfAccount;


    @Column(precision = 18, scale = 2, nullable = false)
    private BigDecimal budgetAmount;


    @Column(precision = 18, scale = 2, nullable = false)
    private BigDecimal spentAmount = BigDecimal.ZERO;


    @Column(precision = 18, scale = 2, nullable = false)
    private BigDecimal committedAmount = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProjectBudgetStatus status = ProjectBudgetStatus.DRAFT;

    private String budgetNotes;

    @Column(nullable = false)
    private UUID createdBy;

    @Column(nullable = false)
    private LocalDateTime createdDate;

    private UUID modifiedBy;
    private LocalDateTime modifiedDate;

    private boolean isActive = true;
    private boolean isDeleted = false;


    public BigDecimal getRemainingAmount() {
        return budgetAmount.subtract(spentAmount).subtract(committedAmount);
    }


    public boolean canSpend(BigDecimal amount) {
        return getRemainingAmount().compareTo(amount) >= 0;
    }

}