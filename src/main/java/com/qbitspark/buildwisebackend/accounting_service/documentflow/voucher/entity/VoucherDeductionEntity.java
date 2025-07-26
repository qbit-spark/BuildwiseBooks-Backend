package com.qbitspark.buildwisebackend.accounting_service.documentflow.voucher.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "voucher_deductions_tb")
public class VoucherDeductionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id ;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "beneficiary_id", nullable = false)
    private VoucherBeneficiaryEntity beneficiary;

    @Column(name = "percentage", nullable = false, precision = 5, scale = 2)
    private BigDecimal percentage;

    @Column(name = "deduction_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal deductionAmount;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "deduct_id")
    private UUID deductId;

    private String deductName;
}