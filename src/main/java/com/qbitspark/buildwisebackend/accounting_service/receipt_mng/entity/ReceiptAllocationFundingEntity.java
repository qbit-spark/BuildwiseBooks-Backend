package com.qbitspark.buildwisebackend.accounting_service.receipt_mng.entity;

import com.qbitspark.buildwisebackend.accounting_service.budget_mng.org_budget.entity.OrgBudgetDetailAllocationEntity;
import com.qbitspark.buildwisebackend.accounting_service.receipt_mng.entity.ReceiptEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "receipt_allocation_funding")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ReceiptAllocationFundingEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID fundingId;

    @ManyToOne
    @JoinColumn(name = "receipt_id", nullable = false)
    private ReceiptEntity receipt;

    @ManyToOne
    @JoinColumn(name = "allocation_id", nullable = false)
    private OrgBudgetDetailAllocationEntity allocation;

    @Column(precision = 18, scale = 2, nullable = false)
    private BigDecimal fundedAmount;

    @Column(nullable = false)
    private LocalDateTime fundingDate;

    private String fundingNotes;

    @Column(nullable = false)
    private UUID authorizedBy;

    @Column(nullable = false)
    private LocalDateTime createdDate;
}