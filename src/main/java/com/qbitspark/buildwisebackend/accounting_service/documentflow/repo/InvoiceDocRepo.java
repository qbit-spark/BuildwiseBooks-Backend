package com.qbitspark.buildwisebackend.accounting_service.documentflow.repo;

import com.qbitspark.buildwisebackend.accounting_service.documentflow.entity.InvoiceDocEntity;
import com.qbitspark.buildwisebackend.accounting_service.documentflow.enums.InvoiceStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InvoiceDocRepo extends JpaRepository<InvoiceDocEntity, UUID> {

    List<InvoiceDocEntity> findByProjectProjectId(UUID projectId);

    List<InvoiceDocEntity> findByOrganisationOrganisationId(UUID organisationId);

    List<InvoiceDocEntity> findByInvoiceStatus(InvoiceStatus status);

    List<InvoiceDocEntity> findByClientId(UUID clientId);

    Optional<InvoiceDocEntity> findByInvoiceNumber(String invoiceNumber);

    @Query("SELECT i FROM InvoiceDocEntity i WHERE i.dueDate < :date AND i.invoiceStatus = :status")
    List<InvoiceDocEntity> findOverdueInvoices(@Param("date") LocalDate date, @Param("status") InvoiceStatus status);

    @Query("SELECT i FROM InvoiceDocEntity i WHERE i.organisation.organisationId = :orgId AND i.invoiceStatus = :status")
    List<InvoiceDocEntity> findByOrganisationAndStatus(@Param("orgId") UUID organisationId, @Param("status") InvoiceStatus status);

    @Query("SELECT i FROM InvoiceDocEntity i WHERE i.project.projectId = :projectId AND i.invoiceStatus = :status")
    List<InvoiceDocEntity> findByProjectAndStatus(@Param("projectId") UUID projectId, @Param("status") InvoiceStatus status);
}