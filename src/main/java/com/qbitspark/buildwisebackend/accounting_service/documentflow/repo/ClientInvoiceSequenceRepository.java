package com.qbitspark.buildwisebackend.accounting_service.documentflow.repo;

import com.qbitspark.buildwisebackend.accounting_service.documentflow.entity.ClientInvoiceSequenceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ClientInvoiceSequenceRepository extends JpaRepository<ClientInvoiceSequenceEntity, UUID> {

    Optional<ClientInvoiceSequenceEntity> findByOrganisationIdAndClientId(UUID organisationId, UUID clientId);

    @Modifying
    @Query("UPDATE ClientInvoiceSequenceEntity c SET c.currentSequence = c.currentSequence + 1 " +
            "WHERE c.organisationId = :orgId AND c.clientId = :clientId")
    int incrementSequence(@Param("orgId") UUID organisationId, @Param("clientId") UUID clientId);

    @Query("SELECT c FROM ClientInvoiceSequenceEntity c WHERE c.currentSequence > :threshold ORDER BY c.updatedAt DESC")
    Optional<ClientInvoiceSequenceEntity> findHighVolumeClients(@Param("threshold") int threshold);
}