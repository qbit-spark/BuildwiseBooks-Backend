package com.qbitspark.buildwisebackend.accounting_service.documentflow.voucher.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.qbitspark.buildwisebackend.accounting_service.budget_mng.org_budget.entity.OrgBudgetDetailAllocationEntity;
import com.qbitspark.buildwisebackend.accounting_service.documentflow.voucher.enums.PaymentMode;
import com.qbitspark.buildwisebackend.accounting_service.documentflow.voucher.enums.VoucherStatus;
import com.qbitspark.buildwisebackend.accounting_service.documentflow.voucher.utils.UUIDListConverter;
import com.qbitspark.buildwisebackend.organisation_service.organisation_mng.entity.OrganisationEntity;
import com.qbitspark.buildwisebackend.organisation_service.orgnisation_members_mng.entities.OrganisationMember;
import com.qbitspark.buildwisebackend.projectmng_service.entity.ProjectEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "vouchers_tb",
        indexes = {

                @Index(name = "idx_voucher_organisation", columnList = "organisation_id"),
                @Index(name = "idx_voucher_project", columnList = "project_id"),
                @Index(name = "idx_voucher_number", columnList = "voucher_number"),
                @Index(name = "idx_voucher_org_status", columnList = "organisation_id, status"),
                @Index(name = "idx_voucher_created_by", columnList = "created_by_id")
        })
public class VoucherEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "voucher_number", nullable = false, unique = true, columnDefinition = "TEXT")
    private String voucherNumber;

    @Column(name = "voucher_date", nullable = false)
    private LocalDateTime voucherDate;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "detail_allocation_id", nullable = false)
    private OrgBudgetDetailAllocationEntity detailAllocation;

    @Column(name = "overall_description", columnDefinition = "TEXT")
    private String overallDescription;

    @Column(name = "payment_mode", nullable = false)
    @Enumerated(EnumType.STRING)
    private PaymentMode paymentMode = PaymentMode.BANK_TRANSFER;

    @Column(name = "total_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency = "TSh";

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private VoucherStatus status;

    // Creator relationship
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_id", nullable = false)
    private OrganisationMember createdBy;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organisation_id", nullable = false)
    private OrganisationEntity organisation;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private ProjectEntity project;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "voucher", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<VoucherBeneficiaryEntity> beneficiaries = new ArrayList<>();

    @Column(name = "attachments", columnDefinition = "JSON")
    @Convert(converter = UUIDListConverter.class)
    private List<UUID> attachments = new ArrayList<>();

}