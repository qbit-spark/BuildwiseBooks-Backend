package com.qbitspark.buildwisebackend.accounting_service.documentflow.invoice.service.impl;

import com.qbitspark.buildwisebackend.accounting_service.documentflow.invoice.entity.ClientInvoiceSequenceEntity;
import com.qbitspark.buildwisebackend.accounting_service.documentflow.invoice.repo.ClientInvoiceSequenceRepository;
import com.qbitspark.buildwisebackend.accounting_service.documentflow.invoice.service.InvoiceNumberService;
import com.qbitspark.buildwisebackend.clientsmng_service.entity.ClientEntity;
import com.qbitspark.buildwisebackend.organisation_service.organisation_mng.entity.OrganisationEntity;
import com.qbitspark.buildwisebackend.projectmng_service.entity.ProjectEntity;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.UUID;

@Service
@AllArgsConstructor
public class InvoiceNumberServiceImpl implements InvoiceNumberService {


    private final ClientInvoiceSequenceRepository sequenceRepository;

    /**
     * Generates invoice number in format: [PROJECT-CODE]-[YEAR]-[CLIENT-SEQUENCE]
     * Example: PROJ0001-25-001
     */

    @Override
    public String generateInvoiceNumber(ProjectEntity project, ClientEntity client, OrganisationEntity organisation) {

        // Get project code (already formatted as PROJ0001, PROJ0002, etc.)
        String projectCode = project.getProjectCode();

        // Get current year (2-digit format)
        String year = String.valueOf(LocalDate.now().getYear()).substring(2); // "25" from "2025"

        // Get the next sequence for this client
        int clientSequence = getNextClientSequence(organisation.getOrganisationId(), client.getClientId());

        // Format sequence with leading zeros (0001, 0002, etc.)
        String paddedSequence = String.format("%04d", clientSequence);

        // Combine into the final invoice number
        String invoiceNumber = String.format("%s-%s-%s", projectCode, year, paddedSequence);

        return invoiceNumber;
    }


    @Override
    public String previewNextInvoiceNumber(ProjectEntity project, ClientEntity client, OrganisationEntity organisation) {
        // Same logic as generateInvoiceNumber() but WITHOUT incrementing
        String projectCode = project.getProjectCode();
        String year = String.valueOf(LocalDate.now().getYear()).substring(2);

        // Preview the next sequence without changing a database
        int nextSequence = sequenceRepository.findByOrganisationIdAndClientId(organisation.getOrganisationId(), client.getClientId())
                .map(seq -> seq.getCurrentSequence() + 1)
                .orElse(1);

        String paddedSequence = String.format("%04d", nextSequence);
        return String.format("%s-%s-%s", projectCode, year, paddedSequence);
    }


    /**
     * Gets the next sequence number for a client within an organisation
     * Creates new sequence record if doesn't exist
     */
    private int getNextClientSequence(UUID organisationId, UUID clientId) {

        // Find or create a sequence for this organisation-client combination
        ClientInvoiceSequenceEntity sequence = sequenceRepository
                .findByOrganisationIdAndClientId(organisationId, clientId)
                .orElseGet(() -> createNewClientSequence(organisationId, clientId));

        // Increment sequence atomically
        sequenceRepository.incrementSequence(organisationId, clientId);
        // Return the next sequence number
        return sequence.getCurrentSequence() + 1;
    }

    /**
     * Creates a new sequence record for a client
     */
    private ClientInvoiceSequenceEntity createNewClientSequence(UUID organisationId, UUID clientId) {

        ClientInvoiceSequenceEntity newSequence = ClientInvoiceSequenceEntity.builder()
                .organisationId(organisationId)
                .clientId(clientId)
                .currentSequence(0)
                .build();

        ClientInvoiceSequenceEntity saved = sequenceRepository.save(newSequence);

        return saved;
    }


}
