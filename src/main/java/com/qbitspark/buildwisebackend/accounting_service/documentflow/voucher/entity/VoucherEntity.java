package com.qbitspark.buildwisebackend.accounting_service.documentflow.voucher.entity;

import com.qbitspark.buildwisebackend.accounting_service.documentflow.voucher.enums.PaymentMode;
import com.qbitspark.buildwisebackend.accounting_service.documentflow.voucher.enums.VoucherStatus;
import com.qbitspark.buildwisebackend.accounting_service.documentflow.voucher.enums.VoucherType;
import com.qbitspark.buildwisebackend.organisation_service.organisation_mng.entity.OrganisationEntity;
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
                @Index(name = "idx_voucher_status", columnList = "status"),
                @Index(name = "idx_voucher_number", columnList = "voucher_number"),
                @Index(name = "idx_voucher_date", columnList = "voucher_date"),
                @Index(name = "idx_voucher_created_at", columnList = "created_at")
        })
public class VoucherEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "voucher_number", nullable = false, unique = true, columnDefinition = "TEXT")
    private String voucherNumber;

    @Column(name = "voucher_date", nullable = false)
    private LocalDateTime voucherDate;

    @Column(name = "voucher_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private VoucherType voucherType;

    @Column(name = "overall_description", columnDefinition = "TEXT")
    private String overallDescription;

    @Column(name = "payment_mode", nullable = false)
    @Enumerated(EnumType.STRING)
    private PaymentMode paymentMode;

    @Column(name = "total_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency = "TSh";

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private VoucherStatus status;

    @Column(name = "prepared_by", nullable = false, columnDefinition = "TEXT")
    private String preparedBy;

    @Column(name = "requested_by", columnDefinition = "TEXT")
    private String requestedBy;

    @Column(name = "department", columnDefinition = "TEXT")
    private String department;

    @Column(name = "approved_by", columnDefinition = "TEXT")
    private String approvedBy;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    // Link to organisation (same as VendorEntity pattern)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organisation_id", nullable = false)
    private OrganisationEntity organisation;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = true) // nullable = true for non-project vouchers
    private ProjectEntity project;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Relationships
    @OneToMany(mappedBy = "voucher", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<VoucherPayeeEntity> payees = new ArrayList<>();

    @OneToMany(mappedBy = "voucher", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<VoucherAttachmentEntity> attachments = new ArrayList<>();

    // Helper methods
    public void addPayee(VoucherPayeeEntity payee) {
        payees.add(payee);
        payee.setVoucher(this);
    }

    public void addAttachment(VoucherAttachmentEntity attachment) {
        attachments.add(attachment);
        attachment.setVoucher(this);
    }

    // Project-related helper methods
    public boolean isProjectVoucher() {
        return project != null;
    }

    public String getProjectName() {
        return project != null ? project.getName() : null;
    }

    public String getProjectCode() {
        return project != null ? project.getProjectCode() : null;
    }
}