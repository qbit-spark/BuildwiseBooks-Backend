package com.qbitspark.buildwisebackend.accounting_service.receipt_mng.repo;

import com.qbitspark.buildwisebackend.accounting_service.documentflow.invoice.entity.InvoiceDocEntity;
import com.qbitspark.buildwisebackend.accounting_service.receipt_mng.entity.ReceiptEntity;
import com.qbitspark.buildwisebackend.accounting_service.receipt_mng.enums.ReceiptStatus;
import com.qbitspark.buildwisebackend.organisation_service.organisation_mng.entity.OrganisationEntity;
import com.qbitspark.buildwisebackend.clientsmng_service.entity.ClientEntity;
import com.qbitspark.buildwisebackend.projectmng_service.entity.ProjectEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReceiptRepo extends JpaRepository<ReceiptEntity, UUID> {

    Page<ReceiptEntity> findByOrganisation(OrganisationEntity organisation, Pageable pageable);

    List<ReceiptEntity> findByOrganisation(OrganisationEntity organisation);

    List<ReceiptEntity> findByInvoiceOrderByReceiptDateDesc(InvoiceDocEntity invoice);

    Page<ReceiptEntity> findByClient(ClientEntity client, Pageable pageable);

    Page<ReceiptEntity> findByProject(ProjectEntity project, Pageable pageable);

    List<ReceiptEntity> findByReceiptDate(LocalDate receiptDate);

    Optional<ReceiptEntity> findByReceiptNumberAndOrganisation(String receiptNumber, OrganisationEntity organisation);

    Optional<ReceiptEntity> findByReceiptIdAndOrganisation(UUID receiptId, OrganisationEntity organisation);

    List<ReceiptEntity> findByOrganisationAndReceiptDateBetween(
            OrganisationEntity organisation, LocalDate startDate, LocalDate endDate);

    List<ReceiptEntity> findByReceiptNumberContainingAndOrganisation(String pattern, OrganisationEntity organisation);

    List<ReceiptEntity> findByInvoiceIdAndStatus(UUID invoiceId, ReceiptStatus receiptStatus);
}