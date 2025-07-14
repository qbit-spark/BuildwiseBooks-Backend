package com.qbitspark.buildwisebackend.accounting_service.receipt_mng.entity;

import com.qbitspark.buildwisebackend.accounting_service.bank_mng.entity.BankAccountEntity;
import com.qbitspark.buildwisebackend.accounting_service.documentflow.invoice.entity.InvoiceDocEntity;
import com.qbitspark.buildwisebackend.accounting_service.receipt_mng.enums.AllocationStatus;
import com.qbitspark.buildwisebackend.accounting_service.receipt_mng.enums.PaymentMethod;
import com.qbitspark.buildwisebackend.accounting_service.receipt_mng.enums.ReceiptStatus;
import com.qbitspark.buildwisebackend.clientsmng_service.entity.ClientEntity;
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
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "receipts")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ReceiptEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID receiptId;

    @Column(nullable = false, unique = true)
    private String receiptNumber;

    @Column(nullable = false)
    private LocalDate receiptDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organisation_id", nullable = false)
    private OrganisationEntity organisation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id")
    private ProjectEntity project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", nullable = false)
    private ClientEntity client;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_id", nullable = false)
    private InvoiceDocEntity invoice;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bank_account_id")
    private BankAccountEntity bankAccount;

    @Column(precision = 18, scale = 2, nullable = false)
    private BigDecimal totalAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentMethod paymentMethod;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReceiptStatus status = ReceiptStatus.DRAFT;

    private String reference;
    private String description;

    @ElementCollection
    @CollectionTable(name = "receipt_attachments", joinColumns = @JoinColumn(name = "receipt_id"))
    @Column(name = "attachment_id")
    private List<UUID> attachments;

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

    // New allocation system relationship
    @OneToMany(mappedBy = "receipt", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<ReceiptAllocationEntity> allocations = new ArrayList<>();

    // ==========================================
    // BUSINESS LOGIC METHODS
    // ==========================================

    /**
     * Get total amount from all APPROVED allocation details
     * Only counts allocations with status = APPROVED
     */
    public BigDecimal getTotalApprovedAllocations() {
        return allocations.stream()
                .flatMap(allocation -> allocation.getDetailAllocations().stream())
                .filter(detail -> detail.getStatus() == AllocationStatus.APPROVED)
                .map(ReceiptAllocationDetailEntity::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Get total amount from all DRAFT allocation details
     * These are pending approval and don't count in budget yet
     */
    public BigDecimal getTotalPendingAllocations() {
        return allocations.stream()
                .flatMap(allocation -> allocation.getDetailAllocations().stream())
                .filter(detail -> detail.getStatus() == AllocationStatus.DRAFT)
                .map(ReceiptAllocationDetailEntity::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Get total amount from all allocation details (approved + pending)
     */
    public BigDecimal getTotalAllocatedAmount() {
        return allocations.stream()
                .flatMap(allocation -> allocation.getDetailAllocations().stream())
                .map(ReceiptAllocationDetailEntity::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Calculate remaining amount that can still be allocated
     * Only considers approved allocations
     */
    public BigDecimal getRemainingAmountForAllocation() {
        return totalAmount.subtract(getTotalApprovedAllocations());
    }

    /**
     * Check if receipt has any approved allocations
     */
    public boolean hasApprovedAllocations() {
        return allocations.stream()
                .flatMap(allocation -> allocation.getDetailAllocations().stream())
                .anyMatch(detail -> detail.getStatus() == AllocationStatus.APPROVED);
    }

    /**
     * Check if receipt has any pending allocations
     */
    public boolean hasPendingAllocations() {
        return allocations.stream()
                .flatMap(allocation -> allocation.getDetailAllocations().stream())
                .anyMatch(detail -> detail.getStatus() == AllocationStatus.DRAFT);
    }

    /**
     * Check if new allocations can be created
     * Receipt must be approved and have remaining amount
     */
    public boolean canCreateNewAllocation() {
        return status == ReceiptStatus.APPROVED &&
                getRemainingAmountForAllocation().compareTo(BigDecimal.ZERO) > 0;
    }

    /**
     * Check if receipt is eligible for allocation creation
     */
    public boolean isEligibleForAllocation() {
        return status == ReceiptStatus.APPROVED;
    }

    /**
     * Get count of total allocation groups
     */
    public int getTotalAllocationCount() {
        return allocations.size();
    }

    /**
     * Get count of allocation groups that have at least one approved detail
     */
    public int getApprovedAllocationCount() {
        return (int) allocations.stream()
                .filter(allocation -> allocation.getDetailAllocations().stream()
                        .anyMatch(detail -> detail.getStatus() == AllocationStatus.APPROVED))
                .count();
    }

    /**
     * Get count of allocation groups that have only draft details
     */
    public int getPendingAllocationCount() {
        return (int) allocations.stream()
                .filter(allocation -> allocation.getDetailAllocations().stream()
                        .allMatch(detail -> detail.getStatus() == AllocationStatus.DRAFT))
                .count();
    }

    /**
     * Check if receipt amount is fully allocated (approved allocations = total amount)
     */
    public boolean isFullyAllocated() {
        return getTotalApprovedAllocations().compareTo(totalAmount) == 0;
    }

    /**
     * Get allocation percentage (approved allocations / total amount * 100)
     */
    public BigDecimal getAllocationPercentage() {
        if (totalAmount.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return getTotalApprovedAllocations()
                .divide(totalAmount, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
    }
}