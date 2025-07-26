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


}