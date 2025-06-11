package com.qbitspark.buildwisebackend.accounting_service.documentflow.voucher.entity;

import com.qbitspark.buildwisebackend.accounting_service.documentflow.voucher.enums.PaymentStatus;
import com.qbitspark.buildwisebackend.vendormng_service.entity.VendorEntity;
import com.qbitspark.buildwisebackend.vendormng_service.enums.VendorType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "voucher_payees_tb",
        indexes = {
                @Index(name = "idx_payee_voucher", columnList = "voucher_id"),
                @Index(name = "idx_payee_vendor", columnList = "vendor_id"),
                @Index(name = "idx_payee_payment_status", columnList = "payment_status"),
                @Index(name = "idx_payee_paid_at", columnList = "paid_at"),
                @Index(name = "idx_payee_payment_reference", columnList = "payment_reference"),
                @Index(name = "idx_payee_voucher_status", columnList = "voucher_id, payment_status"),
                @Index(name = "idx_payee_vendor_status", columnList = "vendor_id, payment_status"),
                @Index(name = "idx_payee_status_paid_at", columnList = "payment_status, paid_at"),
                @Index(name = "idx_payee_created_at", columnList = "created_at")
        })
public class VoucherPayeeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "voucher_id", nullable = false)
    private VoucherEntity voucher;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vendor_id", nullable = false)
    private VendorEntity vendor;

    @Column(name = "amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "payment_status")
    @Enumerated(EnumType.STRING)
    private PaymentStatus paymentStatus = PaymentStatus.PENDING;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @Column(name = "payment_reference", columnDefinition = "TEXT")
    private String paymentReference;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

}