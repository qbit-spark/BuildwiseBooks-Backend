package com.qbitspark.buildwisebackend.accounting_service.receipt_mng.entity;

import com.qbitspark.buildwisebackend.accounting_service.budget_mng.org_budget.entity.OrgBudgetDetailAllocationEntity;
import com.qbitspark.buildwisebackend.accounting_service.receipt_mng.enums.AllocationStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

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
    @JoinColumn(name = "budget_detail_allocation_id", nullable = false)
    private OrgBudgetDetailAllocationEntity budgetDetailAllocation;

    @Column(precision = 18, scale = 2, nullable = false)
    private BigDecimal amount;

    @Column(columnDefinition = "TEXT")
    private String description;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    // ==========================================
    // BUSINESS LOGIC METHODS
    // ==========================================

    /**
     * Check if this detail line affects budget calculations.
     * Only approved allocations impact budget funding.
     */
    public boolean affectsBudget() {
        return allocation != null && allocation.getStatus() == AllocationStatus.APPROVED;
    }

    public boolean isPending() {
        return allocation != null && allocation.getStatus() == AllocationStatus.PENDING_APPROVAL;
    }

    public boolean isDraft() {
        return allocation != null && allocation.getStatus() == AllocationStatus.DRAFT;
    }

    public boolean isApproved() {
        return allocation != null && allocation.getStatus() == AllocationStatus.APPROVED;
    }

    public boolean isRejected() {
        return allocation != null && allocation.getStatus() == AllocationStatus.REJECTED;
    }

    // ==========================================
    // ACCOUNT INFORMATION METHODS
    // ==========================================

    public String getHeaderAccountName() {
        return budgetDetailAllocation != null ?
                budgetDetailAllocation.getHeaderAccountName() : "Unknown";
    }

    public String getHeaderAccountCode() {
        return budgetDetailAllocation != null ?
                budgetDetailAllocation.getHeaderAccountCode() : "N/A";
    }

    public String getDetailAccountName() {
        return budgetDetailAllocation != null ?
                budgetDetailAllocation.getDetailAccountName() : "Unknown";
    }

    public String getDetailAccountCode() {
        return budgetDetailAllocation != null ?
                budgetDetailAllocation.getDetailAccountCode() : "N/A";
    }

    // ==========================================
    // RECEIPT INFORMATION METHODS
    // ==========================================

    public String getReceiptNumber() {
        return allocation != null && allocation.getReceipt() != null ?
                allocation.getReceipt().getReceiptNumber() : "Unknown";
    }

    public BigDecimal getReceiptTotalAmount() {
        return allocation != null && allocation.getReceipt() != null ?
                allocation.getReceipt().getTotalAmount() : BigDecimal.ZERO;
    }

    public String getProjectName() {
        return allocation != null && allocation.getReceipt() != null &&
                allocation.getReceipt().getProject() != null ?
                allocation.getReceipt().getProject().getName() : "Unknown";
    }

    public String getClientName() {
        return allocation != null && allocation.getReceipt() != null &&
                allocation.getReceipt().getClient() != null ?
                allocation.getReceipt().getClient().getName() : "Unknown";
    }

    // ==========================================
    // VALIDATION AND STATUS METHODS
    // ==========================================

    /**
     * Validates that this allocation detail doesn't exceed the budget's unfunded amount.
     * Critical business rule: can't over-fund a budget line.
     */
    public void validateAgainstBudget() {
        if (budgetDetailAllocation == null) {
            throw new IllegalStateException("Budget detail allocation is required");
        }

        BigDecimal unfundedBudget = budgetDetailAllocation.getUnfundedBudget();
        if (amount.compareTo(unfundedBudget) > 0) {
            throw new IllegalArgumentException(String.format(
                    "Allocation amount (%s) exceeds unfunded budget (%s) for account %s - %s",
                    amount, unfundedBudget,
                    getDetailAccountCode(), getDetailAccountName()
            ));
        }
    }

    public String getStatusDescription() {
        if (allocation == null) {
            return "Invalid - No allocation";
        }

        return switch (allocation.getStatus()) {
            case DRAFT -> "Draft allocation";
            case PENDING_APPROVAL -> "Pending approval";
            case APPROVED -> "Approved - Budget funded";
            case REJECTED -> "Rejected";
            case CANCELLED -> "Cancelled";
        };
    }

    public String getAllocationSummary() {
        return String.format(
                "%s: %s → %s - %s (%s)",
                getReceiptNumber(),
                amount,
                getDetailAccountCode(),
                getDetailAccountName(),
                allocation != null ? allocation.getStatus() : "No Status"
        );
    }

    /**
     * Check if this allocation can be edited.
     * Only draft allocations can be modified.
     */
    public boolean canEdit() {
        return allocation != null && allocation.canEdit();
    }

    public UUID getOrganisationId() {
        return allocation != null && allocation.getReceipt() != null &&
                allocation.getReceipt().getOrganisation() != null ?
                allocation.getReceipt().getOrganisation().getOrganisationId() : null;
    }

    public UUID getProjectId() {
        return allocation != null && allocation.getReceipt() != null &&
                allocation.getReceipt().getProject() != null ?
                allocation.getReceipt().getProject().getProjectId() : null;
    }

    public long getDaysSinceCreation() {
        return java.time.Duration.between(createdAt, LocalDateTime.now()).toDays();
    }

    /**
     * Get the current funded status of the target budget account.
     * Useful for validation and display purposes.
     */
    public String getBudgetFundingStatus() {
        return budgetDetailAllocation != null ?
                budgetDetailAllocation.getFundingStatus() : "Unknown";
    }

    public BigDecimal getBudgetAvailableToSpend() {
        return budgetDetailAllocation != null ?
                budgetDetailAllocation.getAvailableToSpend() : BigDecimal.ZERO;
    }
}