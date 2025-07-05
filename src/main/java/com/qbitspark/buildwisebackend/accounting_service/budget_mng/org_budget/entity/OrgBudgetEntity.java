package com.qbitspark.buildwisebackend.accounting_service.budget_mng.org_budget.entity;

import com.qbitspark.buildwisebackend.accounting_service.budget_mng.org_budget.enums.OrgBudgetStatus;
import com.qbitspark.buildwisebackend.organisation_service.organisation_mng.entity.OrganisationEntity;
import jakarta.persistence.*;

import java.math.RoundingMode;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

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


    @Column(precision = 18, scale = 2, nullable = true)
    private BigDecimal spentAmount = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrgBudgetStatus status = OrgBudgetStatus.DRAFT;

    private String description;

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

    // One-to-Many relationship with budget line items
    @OneToMany(mappedBy = "orgBudget", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private List<OrgBudgetLineItemEntity> lineItems = new ArrayList<>();


    public BigDecimal getTotalBudgetAmount() {
        return getDistributedAmount();
    }


    public BigDecimal getAvailableAmount() {
        return BigDecimal.ZERO;
    }


    public BigDecimal getDistributedAmount() {
        return lineItems.stream()
                .map(OrgBudgetLineItemEntity::getBudgetAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }


    public BigDecimal getTotalSpentFromLineItems() {
        return lineItems.stream()
                .map(OrgBudgetLineItemEntity::getSpentAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }


    public BigDecimal getTotalCommittedAmount() {
        return lineItems.stream()
                .map(OrgBudgetLineItemEntity::getCommittedAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }


    public BigDecimal getTotalRemainingAmount() {
        return lineItems.stream()
                .map(OrgBudgetLineItemEntity::getRemainingAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }


    public BigDecimal getBudgetUtilizationPercentage() {
        return BigDecimal.valueOf(100);
    }

    public BigDecimal getSpendingPercentage() {
        BigDecimal distributedAmount = getDistributedAmount();
        if (distributedAmount.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return getTotalSpentFromLineItems()
                .multiply(BigDecimal.valueOf(100)).divide(distributedAmount, 2, RoundingMode.HALF_UP);
    }



    public long getLineItemsWithBudgetCount() {
        return lineItems.stream()
                .filter(OrgBudgetLineItemEntity::hasBudgetAllocated)
                .count();
    }


    public long getLineItemsWithoutBudgetCount() {
        return lineItems.stream()
                .filter(item -> !item.hasBudgetAllocated())
                .count();
    }


}