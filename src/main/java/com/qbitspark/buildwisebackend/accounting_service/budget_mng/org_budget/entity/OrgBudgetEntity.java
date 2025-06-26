package com.qbitspark.buildwisebackend.accounting_service.budget_mng.org_budget.entity;

import com.qbitspark.buildwisebackend.accounting_service.budget_mng.org_budget.enums.BudgetStatus;
import com.qbitspark.buildwisebackend.organisation_service.organisation_mng.entity.OrganisationEntity;
import jakarta.persistence.*;

import java.util.UUID;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "org_budget")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class OrgBudgetEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID budgetId;

    @ManyToOne
    @JoinColumn(name = "organisation_id", nullable = false)
    private OrganisationEntity organisation;

    @Column(nullable = false, length = 100)
    private String budgetName;

    @Column(nullable = false)
    private LocalDate financialYearStart;

    @Column(nullable = false)
    private LocalDate financialYearEnd;

    @Column(precision = 18, scale = 2, nullable = false)
    private BigDecimal totalBudgetAmount;

    @Column(precision = 18, scale = 2, nullable = false)
    private BigDecimal allocatedAmount = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BudgetStatus status = BudgetStatus.DRAFT;

    private String description;

    @Column(nullable = false)
    private UUID createdBy;

    @Column(nullable = false)
    private LocalDateTime createdDate;

    private UUID modifiedBy;
    private LocalDateTime modifiedDate;

    @Column(nullable = false)
    private Integer budgetVersion = 1;

    private UUID parentBudgetId;

    private boolean isActive = true;
    private boolean isDeleted = false;

    public BigDecimal getAvailableAmount() {
        return this.totalBudgetAmount.subtract(this.allocatedAmount);
    }

}
