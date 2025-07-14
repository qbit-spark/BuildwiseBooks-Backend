package com.qbitspark.buildwisebackend.accounting_service.receipt_mng.repo;

import com.qbitspark.buildwisebackend.accounting_service.receipt_mng.entity.ReceiptAllocationEntity;
import com.qbitspark.buildwisebackend.accounting_service.receipt_mng.entity.ReceiptEntity;
import com.qbitspark.buildwisebackend.accounting_service.receipt_mng.enums.AllocationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;


public interface ReceiptAllocationRepo extends JpaRepository<ReceiptAllocationEntity, UUID> {

    List<ReceiptAllocationEntity> findByReceipt(ReceiptEntity receipt);

    List<ReceiptAllocationEntity> findByStatus(AllocationStatus status);

    List<ReceiptAllocationEntity> findByRequestedBy(UUID requestedBy);

    Optional<ReceiptAllocationEntity> findByReceiptAndStatus(ReceiptEntity receipt, AllocationStatus status);

    boolean existsByReceipt(ReceiptEntity receipt);

    List<ReceiptAllocationEntity> findByStatusOrderByCreatedAtAsc(AllocationStatus status);

    List<ReceiptAllocationEntity> findByStatusIn(List<AllocationStatus> statuses);

}