package com.qbitspark.buildwisebackend.accounting_service.receipt_mng.repo;


import com.qbitspark.buildwisebackend.accounting_service.receipt_mng.entity.ReceiptAllocationEntity;
import com.qbitspark.buildwisebackend.accounting_service.receipt_mng.entity.ReceiptEntity;
import com.qbitspark.buildwisebackend.accounting_service.receipt_mng.enums.AllocationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ReceiptAllocationRepo extends JpaRepository<ReceiptAllocationEntity, UUID> {

    List<ReceiptAllocationEntity> findByReceipt(ReceiptEntity receipt);

    List<ReceiptAllocationEntity> findByStatus(AllocationStatus status);

    List<ReceiptAllocationEntity> findByRequestedBy(UUID requestedBy);
}