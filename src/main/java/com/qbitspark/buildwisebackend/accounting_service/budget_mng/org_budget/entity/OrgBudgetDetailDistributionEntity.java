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
@Table(name = "org_budget_detail_distribution")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class OrgBudgetDetailDistributionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID distributionId;

    @ManyToOne
    @JoinColumn(name = "budget_id", nullable = false)
    private OrgBudgetEntity budget;

    @ManyToOne
    @JoinColumn(name = "detail_account_id", nullable = false)
    private ChartOfAccounts detailAccount;

    @Column(precision = 18, scale = 2, nullable = false)
    private BigDecimal distributedAmount;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private LocalDateTime createdDate;

    private UUID createdBy;
}