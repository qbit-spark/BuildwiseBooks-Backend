package com.qbitspark.buildwisebackend.accounting_service.budget_mng.org_budget.entity;

import com.qbitspark.buildwisebackend.accounting_service.budget_mng.org_budget.enums.FundingType;
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
@Table(name = "budget_funding_allocations")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class BudgetFundingAllocationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID fundingId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "budget_id", nullable = false)
    private OrgBudgetEntity budget;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private ChartOfAccounts account;

    @Column(precision = 18, scale = 2, nullable = false)
    private BigDecimal fundedAmount;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FundingType fundingType = FundingType.RECEIPT_ALLOCATION;

    @Column(name = "source_receipt_allocation_detail_id")
    private UUID sourceReceiptAllocationDetailId;

    @Column(name = "source_receipt_id")
    private UUID sourceReceiptId;

    @CreationTimestamp
    @Column(name = "funded_date", nullable = false, updatable = false)
    private LocalDateTime fundedDate;

    @Column(name = "funded_by", nullable = false)
    private UUID fundedBy;

    @Column(name = "reference_number")
    private String referenceNumber;

    // ==========================================
    // ESSENTIAL BUSINESS LOGIC METHODS (KEEP)
    // ==========================================

    public boolean isReceiptFunding() {
        return FundingType.RECEIPT_ALLOCATION.equals(fundingType);
    }

    public String getAccountCode() {
        return account != null ? account.getAccountCode() : null;
    }

    public String getAccountName() {
        return account != null ? account.getName() : null;
    }

    // ==========================================
    // VALIDATION METHODS (KEEP)
    // ==========================================

    public boolean isValidFunding() {
        return fundedAmount != null &&
                fundedAmount.compareTo(BigDecimal.ZERO) > 0 &&
                account != null &&
                budget != null;
    }
}