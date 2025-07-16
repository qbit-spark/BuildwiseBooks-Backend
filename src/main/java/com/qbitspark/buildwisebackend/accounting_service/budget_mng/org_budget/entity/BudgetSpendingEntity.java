package com.qbitspark.buildwisebackend.accounting_service.budget_mng.org_budget.entity;

import com.qbitspark.buildwisebackend.accounting_service.budget_mng.org_budget.enums.SpendingType;
import com.qbitspark.buildwisebackend.accounting_service.coa.entity.ChartOfAccounts;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "budget_spending")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class BudgetSpendingEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID spendingId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private ChartOfAccounts account;

    @Column(precision = 18, scale = 2, nullable = false)
    private BigDecimal spentAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SpendingType spendingType = SpendingType.VOUCHER_SPENDING;

    @Column(name = "source_voucher_id")
    private UUID sourceVoucherId;

    @Column(name = "source_voucher_beneficiary_id")
    private UUID sourceVoucherBeneficiaryId;

    @CreationTimestamp
    @Column(name = "spent_date", nullable = false, updatable = false)
    private LocalDateTime spentDate;

    @Column(name = "spent_by", nullable = false)
    private UUID spentBy;

    @Column(name = "reference_number")
    private String referenceNumber;

    @Column(columnDefinition = "TEXT")
    private String description;
}