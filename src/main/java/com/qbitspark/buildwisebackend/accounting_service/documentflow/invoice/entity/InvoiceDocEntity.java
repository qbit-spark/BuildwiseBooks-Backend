package com.qbitspark.buildwisebackend.accounting_service.documentflow.invoice.entity;

import com.qbitspark.buildwisebackend.accounting_service.documentflow.invoice.enums.InvoiceStatus;
import com.qbitspark.buildwisebackend.accounting_service.documentflow.invoice.enums.InvoiceType;
import com.qbitspark.buildwisebackend.clientsmng_service.entity.ClientEntity;
import com.qbitspark.buildwisebackend.organisation_service.organisation_mng.entity.OrganisationEntity;
import com.qbitspark.buildwisebackend.projectmng_service.entity.ProjectEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "invoice_business_docs", indexes = {
        @Index(name = "idx_invoice_number", columnList = "invoice_number", unique = true),
        @Index(name = "idx_invoice_project_id", columnList = "project_id"),
        @Index(name = "idx_invoice_client_id", columnList = "client_id"),
        @Index(name = "idx_invoice_organisation_id", columnList = "organisation_id"),
        @Index(name = "idx_invoice_status", columnList = "invoice_status"),
        @Index(name = "idx_invoice_type", columnList = "invoice_type"),
        @Index(name = "idx_invoice_date_of_issue", columnList = "date_of_issue"),
        @Index(name = "idx_invoice_due_date", columnList = "due_date"),
        @Index(name = "idx_invoice_created_at", columnList = "created_at"),
        @Index(name = "idx_invoice_created_by", columnList = "created_by"),
        // Composite indexes for common query patterns
        @Index(name = "idx_invoice_org_status", columnList = "organisation_id, invoice_status"),
        @Index(name = "idx_invoice_client_status", columnList = "client_id, invoice_status"),
        @Index(name = "idx_invoice_project_status", columnList = "project_id, invoice_status"),
        @Index(name = "idx_invoice_status_date", columnList = "invoice_status, date_of_issue"),
        @Index(name = "idx_invoice_org_date", columnList = "organisation_id, date_of_issue"),
        @Index(name = "idx_invoice_client_date", columnList = "client_id, date_of_issue"),
        @Index(name = "idx_invoice_client_due_date", columnList = "client_id, due_date"),
        @Index(name = "idx_invoice_status_due_date", columnList = "invoice_status, due_date"),
        @Index(name = "idx_invoice_org_status_date", columnList = "organisation_id, invoice_status, date_of_issue")
})

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"lineItems"})
@EqualsAndHashCode(of = {"id", "invoiceNumber"})
public class InvoiceDocEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "invoice_number", unique = true, nullable = false, columnDefinition = "TEXT")
    private String invoiceNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id")
    private ProjectEntity project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id")
    private ClientEntity client;

    @Enumerated(EnumType.STRING)
    @Column(name = "invoice_type")
    private InvoiceType invoiceType;

    @Enumerated(EnumType.STRING)
    @Column(name = "invoice_status")
    @Builder.Default
    private InvoiceStatus invoiceStatus = InvoiceStatus.DRAFT;

    @Column(name = "date_of_issue")
    private LocalDate dateOfIssue;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(name = "reference")
    private String reference;

    // Association: Many invoices belong to one organisation
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organisation_id")
    private OrganisationEntity organisation;

    @Column(name = "subtotal", precision = 19, scale = 2)
    @Builder.Default
    private BigDecimal subtotal = BigDecimal.ZERO;

    @Column(name = "discount_amount", precision = 19, scale = 2)
    @Builder.Default
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Column(name = "tax_amount", precision = 19, scale = 2)
    @Builder.Default
    private BigDecimal taxAmount = BigDecimal.ZERO;

    @Column(name = "amount_before_tax", precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal amountBefore = BigDecimal.ZERO;

    @Column(name = "total_amount", precision = 19, scale = 2)
    @Builder.Default
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Column(name = "amount_paid", precision = 19, scale = 2)
    @Builder.Default
    private BigDecimal amountPaid = BigDecimal.ZERO;

    @Column(name = "credit_applied", precision = 19, scale = 2)
    @Builder.Default
    private BigDecimal creditApplied = BigDecimal.ZERO;

    @Column(name = "amount_due", precision = 19, scale = 2)
    @Builder.Default
    private BigDecimal amountDue = BigDecimal.ZERO;

    @Column(name = "currency")
    @Builder.Default
    private String currency = "TZS";

    @OneToMany(mappedBy = "invoice", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    @Builder.Default
    private List<InvoiceLineItemEntity> lineItems = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "created_by")
    private UUID createdBy;

    @Column(name = "updated_by")
    private UUID updatedBy;

}