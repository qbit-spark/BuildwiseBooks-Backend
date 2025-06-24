package com.qbitspark.buildwisebackend.accounting_service.documentflow.invoice.repo;

import com.qbitspark.buildwisebackend.accounting_service.documentflow.invoice.entity.InvoiceDocEntity;
import com.qbitspark.buildwisebackend.clientsmng_service.entity.ClientEntity;
import com.qbitspark.buildwisebackend.organisation_service.organisation_mng.entity.OrganisationEntity;
import com.qbitspark.buildwisebackend.projectmng_service.entity.ProjectEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InvoiceDocRepo extends JpaRepository<InvoiceDocEntity, UUID> {
 Optional<InvoiceDocEntity> findByInvoiceNumber(String invoiceNumber);
 Optional<InvoiceDocEntity> findByIdAndOrganisation(UUID invoiceId, OrganisationEntity organisation);
 Optional<InvoiceDocEntity> findByInvoiceNumberAndOrganisation(String invoiceNumber, OrganisationEntity organisation);
 Page<InvoiceDocEntity> findAllByProject(ProjectEntity project, Pageable pageable);
 List<InvoiceDocEntity> findAllByClient(ClientEntity client);
 Page<InvoiceDocEntity> findAllByOrganisation(OrganisationEntity organisation, Pageable pageable);
}