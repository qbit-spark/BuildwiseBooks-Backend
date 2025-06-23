package com.qbitspark.buildwisebackend.accounting_service.documentflow.invoice.entity;

import com.qbitspark.buildwisebackend.accounting_service.documentflow.invoice.enums.TaxType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "invoice_line_items", indexes = {
        @Index(name = "idx_line_invoice_id", columnList = "invoice_id"),
        @Index(name = "idx_line_invoice_order", columnList = "line_order"),
        @Index(name = "idx_line_invoice_tax_type", columnList = "tax_type"),
        @Index(name = "idx_line_invoice_created_at", columnList = "created_at"),
        // Composite indexes
        @Index(name = "idx_line_invoice_order", columnList = "invoice_id, line_order"),
        @Index(name = "idx_line_invoice_tax", columnList = "invoice_id, tax_type")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"invoice"}) // Exclude to prevent circular reference
@EqualsAndHashCode(of = {"id"})
public class InvoiceLineItemEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_id", nullable = false)
    private InvoiceDocEntity invoice;

    @Column(name = "description", nullable = false)
    private String description;

    @Column(name = "rate", precision = 19, scale = 2)
    @Builder.Default
    private BigDecimal rate = BigDecimal.ZERO;

    @Column(name = "quantity", precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal quantity = BigDecimal.ONE;

    @Column(name = "line_total", precision = 19, scale = 2)
    @Builder.Default
    private BigDecimal lineTotal = BigDecimal.ZERO;

    @Column(name = "unit_of_measure")
    private String unitOfMeasure;

    @Column(name = "line_order")
    @Builder.Default
    private Integer lineOrder = 0;

    // Audit fields
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

}
