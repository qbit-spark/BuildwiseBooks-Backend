package com.qbitspark.buildwisebackend.accounting_service.documentflow.payment.entity;

import com.qbitspark.buildwisebackend.accounting_service.documentflow.voucher.entity.VoucherEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "payment_line_items", indexes = {
        @Index(name = "idx_payment_line_payment", columnList = "payment_id"),
        @Index(name = "idx_payment_line_voucher", columnList = "voucher_id"),
        @Index(name = "idx_payment_line_created", columnList = "created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentLineItemEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_id", nullable = false)
    private PaymentDocEntity payment;

    // For voucher payments
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "voucher_id")
    private VoucherEntity voucher;

    // For future invoice payments
    // @ManyToOne(fetch = FetchType.LAZY)
    // @JoinColumn(name = "invoice_id")
    // private InvoiceDocEntity invoice;

    @Column(name = "amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "line_order")
    @Builder.Default
    private Integer lineOrder = 0;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}