package com.qbitspark.buildwisebackend.accounting_service.receipt_mng.repo;

import com.qbitspark.buildwisebackend.accounting_service.receipt_mng.entity.ReceiptAllocationEntity;
import com.qbitspark.buildwisebackend.accounting_service.receipt_mng.entity.ReceiptEntity;
import com.qbitspark.buildwisebackend.accounting_service.receipt_mng.enums.AllocationStatus;
import com.qbitspark.buildwisebackend.organisation_service.organisation_mng.entity.OrganisationEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ReceiptAllocationRepo extends JpaRepository<ReceiptAllocationEntity, UUID> {

    List<ReceiptAllocationEntity> findByReceipt(ReceiptEntity receipt);

    Page<ReceiptAllocationEntity> findAllByReceiptOrganisation(OrganisationEntity organisation, Pageable pageable);

    Page<ReceiptAllocationEntity> findAllByReceiptOrganisationAndStatus(OrganisationEntity organisation, AllocationStatus status, Pageable pageable);

    Optional<ReceiptAllocationEntity> findByAllocationIdAndReceiptOrganisation(UUID allocationId, OrganisationEntity organisation);
}