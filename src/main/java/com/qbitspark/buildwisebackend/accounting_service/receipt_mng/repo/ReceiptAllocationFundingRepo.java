package com.qbitspark.buildwisebackend.accounting_service.receipt_mng.repo;

import com.qbitspark.buildwisebackend.accounting_service.budget_mng.org_budget.entity.OrgBudgetDetailAllocationEntity;
import com.qbitspark.buildwisebackend.accounting_service.receipt_mng.entity.ReceiptAllocationFundingEntity;
import com.qbitspark.buildwisebackend.accounting_service.receipt_mng.entity.ReceiptEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ReceiptAllocationFundingRepo extends JpaRepository<ReceiptAllocationFundingEntity, UUID> {

    List<ReceiptAllocationFundingEntity> findByReceipt(ReceiptEntity receipt);

    List<ReceiptAllocationFundingEntity> findByAllocation(OrgBudgetDetailAllocationEntity allocation);

    List<ReceiptAllocationFundingEntity> findByReceiptReceiptId(UUID receiptId);

    List<ReceiptAllocationFundingEntity> findByAllocationAllocationId(UUID allocationId);

    void deleteByReceipt(ReceiptEntity receipt);
}