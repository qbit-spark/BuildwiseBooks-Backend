package com.qbitspark.buildwisebackend.accounting_service.receipt_mng.entity;

import com.qbitspark.buildwisebackend.accounting_service.coa.entity.ChartOfAccounts;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "receipt_allocation_details")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ReceiptAllocationDetailEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID detailId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "allocation_id", nullable = false)
    private ReceiptAllocationEntity allocation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private ChartOfAccounts account;

    @Column(precision = 18, scale = 2, nullable = false)
    private BigDecimal allocatedAmount;

    @Column(columnDefinition = "TEXT")
    private String description;

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

    // ==========================================
    // BUSINESS LOGIC METHODS
    // ==========================================

    public boolean canEdit() {
        return allocation != null && allocation.canEdit();
    }

    public boolean isValidAmount() {
        return allocatedAmount != null && allocatedAmount.compareTo(BigDecimal.ZERO) > 0;
    }

    public String getAccountCode() {
        return account != null ? account.getAccountCode() : null;
    }

    public String getAccountName() {
        return account != null ? account.getName() : null;
    }

    // ==========================================
    // VALIDATION METHODS
    // ==========================================

    public boolean belongsToExpenseAccount() {
        return account != null &&
                account.getAccountType() != null &&
                account.getAccountType().name().equals("EXPENSE");
    }

    public boolean isDetailAccount() {
        return account != null && !account.getIsHeader();
    }
}