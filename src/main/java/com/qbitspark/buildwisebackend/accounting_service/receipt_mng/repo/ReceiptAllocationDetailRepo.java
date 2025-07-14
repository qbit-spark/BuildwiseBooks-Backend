package com.qbitspark.buildwisebackend.accounting_service.receipt_mng.repo;

import com.qbitspark.buildwisebackend.accounting_service.receipt_mng.entity.ReceiptAllocationDetailEntity;
import com.qbitspark.buildwisebackend.accounting_service.receipt_mng.entity.ReceiptAllocationEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ReceiptAllocationDetailRepo extends JpaRepository<ReceiptAllocationDetailEntity, UUID> {

    List<ReceiptAllocationDetailEntity> findByAllocation(ReceiptAllocationEntity allocation);

    List<ReceiptAllocationDetailEntity> findByAllocationAllocationId(UUID allocationId);

    void deleteByAllocation(ReceiptAllocationEntity allocation);

    long countByAllocation(ReceiptAllocationEntity allocation);
}