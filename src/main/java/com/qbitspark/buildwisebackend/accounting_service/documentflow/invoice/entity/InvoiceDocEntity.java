package com.qbitspark.buildwisebackend.accounting_service.documentflow.invoice.entity;

import com.qbitspark.buildwisebackend.accounting_service.documentflow.invoice.entity.InvoiceLineItemEntity;
import com.qbitspark.buildwisebackend.accounting_service.documentflow.invoice.entity.embedings.InvoiceTaxDetail;
import com.qbitspark.buildwisebackend.accounting_service.documentflow.invoice.enums.InvoiceStatus;
import com.qbitspark.buildwisebackend.clientsmng_service.entity.ClientEntity;
import com.qbitspark.buildwisebackend.organisation_service.organisation_mng.entity.OrganisationEntity;
import com.qbitspark.buildwisebackend.projectmng_service.entity.ProjectEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;
import org.javamoney.moneta.Money;

import javax.money.MonetaryAmount;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "invoice_business_docs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InvoiceDocEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "invoice_number", unique = true, nullable = false)
    private String invoiceNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id")
    private ProjectEntity project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id")
    private ClientEntity client;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organisation_id")
    private OrganisationEntity organisation;

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

    @Column(name = "subtotal", precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal subtotal = BigDecimal.ZERO;

    @Column(name = "total_tax_amount", precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal totalTaxAmount = BigDecimal.ZERO;

    @Column(name = "total_amount", precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Column(name = "amount_paid", precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal amountPaid = BigDecimal.ZERO;

    @Column(name = "amount_due", precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal amountDue = BigDecimal.ZERO;

    @ElementCollection
    @CollectionTable(name = "invoice_tax_details", joinColumns = @JoinColumn(name = "invoice_id"))
    @Column(name = "tax_details", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    @Builder.Default
    private List<InvoiceTaxDetail> taxDetails = new ArrayList<>();

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

    // JAVA MONEY METHODS (TZS)
    public MonetaryAmount getSubtotalMoney() {
        return Money.of(subtotal, "TZS");
    }

    public MonetaryAmount getTotalAmountMoney() {
        return Money.of(totalAmount, "TZS");
    }

    public MonetaryAmount getAmountDueMoney() {
        return Money.of(amountDue, "TZS");
    }

    public void setSubtotalMoney(MonetaryAmount amount) {
        this.subtotal = amount.getNumber().numberValue(BigDecimal.class);
    }

    public void setTotalAmountMoney(MonetaryAmount amount) {
        this.totalAmount = amount.getNumber().numberValue(BigDecimal.class);
    }

    public void setAmountDueMoney(MonetaryAmount amount) {
        this.amountDue = amount.getNumber().numberValue(BigDecimal.class);
    }
}