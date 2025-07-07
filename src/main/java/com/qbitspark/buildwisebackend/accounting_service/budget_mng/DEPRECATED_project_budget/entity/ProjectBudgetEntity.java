package com.qbitspark.buildwisebackend.accounting_service.budget_mng.DEPRECATED_project_budget.entity;

import com.qbitspark.buildwisebackend.accounting_service.budget_mng.org_budget.entity.OrgBudgetEntity;
import com.qbitspark.buildwisebackend.accounting_service.budget_mng.DEPRECATED_project_budget.enums.ProjectBudgetStatus;
import com.qbitspark.buildwisebackend.projectmng_service.entity.ProjectEntity;
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

    @OneToOne
    @JoinColumn(name = "project_id", nullable = false)
    private ProjectEntity project;

    @Column(precision = 18, scale = 2, nullable = false)
    private BigDecimal totalBudgetAmount = BigDecimal.ZERO;


    @Column(precision = 18, scale = 2, nullable = false)
    private BigDecimal totalSpentAmount = BigDecimal.ZERO;

    @Column(precision = 18, scale = 2, nullable = false)
    private BigDecimal totalCommittedAmount = BigDecimal.ZERO;

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

    private UUID approvedBy;
    private LocalDateTime approvedDate;

    private boolean isActive = true;
    private boolean isDeleted = false;


    @OneToMany(mappedBy = "projectBudget", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<ProjectBudgetLineItemEntity> lineItems = new ArrayList<>();


    public BigDecimal getTotalRemainingAmount() {
        return totalBudgetAmount.subtract(totalSpentAmount).subtract(totalCommittedAmount);
    }


    public void addLineItem(ProjectBudgetLineItemEntity lineItem) {
        lineItems.add(lineItem);
        lineItem.setProjectBudget(this);
        recalculateTotalBudgetAmount();
    }

    public void recalculateTotalBudgetAmount() {
        this.totalBudgetAmount = lineItems.stream()
                .map(ProjectBudgetLineItemEntity::getBudgetAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

}
