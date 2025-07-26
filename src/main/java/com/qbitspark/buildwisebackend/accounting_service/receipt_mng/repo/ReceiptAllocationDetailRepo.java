package com.qbitspark.buildwisebackend.accounting_service.receipt_mng.repo;

import com.qbitspark.buildwisebackend.accounting_service.coa.entity.ChartOfAccounts;
import com.qbitspark.buildwisebackend.accounting_service.receipt_mng.entity.ReceiptAllocationDetailEntity;
import com.qbitspark.buildwisebackend.accounting_service.receipt_mng.entity.ReceiptAllocationEntity;
import com.qbitspark.buildwisebackend.accounting_service.receipt_mng.enums.AllocationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ReceiptAllocationDetailRepo extends JpaRepository<ReceiptAllocationDetailEntity, UUID> {

    List<ReceiptAllocationDetailEntity> findByAllocation(ReceiptAllocationEntity allocation);

    List<ReceiptAllocationDetailEntity> findByAccount(ChartOfAccounts account);

    List<ReceiptAllocationDetailEntity> findByAllocation_Status(AllocationStatus status);

    boolean existsByAllocationAndAccount(ReceiptAllocationEntity allocation, ChartOfAccounts account);
}