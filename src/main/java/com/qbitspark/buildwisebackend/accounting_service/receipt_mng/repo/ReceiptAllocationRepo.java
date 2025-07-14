package com.qbitspark.buildwisebackend.accounting_service.receipt_mng.repo;

import com.qbitspark.buildwisebackend.accounting_service.receipt_mng.entity.ReceiptAllocationEntity;
import com.qbitspark.buildwisebackend.accounting_service.receipt_mng.entity.ReceiptEntity;
import com.qbitspark.buildwisebackend.accounting_service.receipt_mng.enums.AllocationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReceiptAllocationRepo extends JpaRepository<ReceiptAllocationEntity, UUID> {

    List<ReceiptAllocationEntity> findByReceipt(ReceiptEntity receipt);

    List<ReceiptAllocationEntity> findByReceiptAndStatus(ReceiptEntity receipt, AllocationStatus status);

    List<ReceiptAllocationEntity> findByReceiptReceiptId(UUID receiptId);

    Optional<ReceiptAllocationEntity> findByAllocationIdAndReceipt(UUID allocationId, ReceiptEntity receipt);

    List<ReceiptAllocationEntity> findByStatus(AllocationStatus status);

    void deleteByReceipt(ReceiptEntity receipt);

    boolean existsByReceiptAndStatus(ReceiptEntity receipt, AllocationStatus status);
}