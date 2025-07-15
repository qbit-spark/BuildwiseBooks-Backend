package com.qbitspark.buildwisebackend.accounting_service.receipt_mng.entity;

import com.qbitspark.buildwisebackend.accounting_service.receipt_mng.enums.AllocationStatus;
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

@Entity
@Table(name = "receipt_allocations")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ReceiptAllocationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID allocationId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receipt_id", nullable = false)
    private ReceiptEntity receipt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AllocationStatus status;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "requested_by", nullable = false)
    private UUID requestedBy;

    @Column(name = "requested_at", nullable = false)
    private LocalDateTime requestedAt;

    @Column(name = "approved_by")
    private UUID approvedBy;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // IMPORTANT: Use EAGER fetching to ensure details are loaded
    @OneToMany(mappedBy = "allocation", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private List<ReceiptAllocationDetailEntity> allocationDetails = new ArrayList<>();

    // Calculate total allocated amount from details
    public BigDecimal getTotalAllocatedAmount() {
        if (allocationDetails == null || allocationDetails.isEmpty()) {
            return BigDecimal.ZERO;
        }
        return allocationDetails.stream()
                .map(ReceiptAllocationDetailEntity::getAllocatedAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    // Calculate remaining amount
    public BigDecimal getRemainingAmount() {
        if (receipt == null) {
            return BigDecimal.ZERO;
        }
        return receipt.getTotalAmount().subtract(getTotalAllocatedAmount());
    }

    // Check if fully allocated
    public boolean isFullyAllocated() {
        if (receipt == null) {
            return false;
        }
        return getTotalAllocatedAmount().compareTo(receipt.getTotalAmount()) == 0;
    }

    // Helper method to add allocation detail
    public void addAllocationDetail(ReceiptAllocationDetailEntity detail) {
        if (allocationDetails == null) {
            allocationDetails = new ArrayList<>();
        }
        allocationDetails.add(detail);
        detail.setAllocation(this);
    }
}