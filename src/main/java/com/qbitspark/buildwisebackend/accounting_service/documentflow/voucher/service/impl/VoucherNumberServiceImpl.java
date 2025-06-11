package com.qbitspark.buildwisebackend.accounting_service.documentflow.voucher.service.impl;

import com.qbitspark.buildwisebackend.accounting_service.documentflow.voucher.entity.OrganisationVoucherSequenceEntity;
import com.qbitspark.buildwisebackend.accounting_service.documentflow.voucher.repo.OrganisationVoucherSequenceRepository;
import com.qbitspark.buildwisebackend.accounting_service.documentflow.voucher.service.VoucherNumberService;
import com.qbitspark.buildwisebackend.organisation_service.organisation_mng.entity.OrganisationEntity;
import com.qbitspark.buildwisebackend.projectmng_service.entity.ProjectEntity;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.UUID;

@Service
@AllArgsConstructor
public class VoucherNumberServiceImpl implements VoucherNumberService {

    private final OrganisationVoucherSequenceRepository sequenceRepository;

    /**
     * Generates voucher number in format: [PROJECT-CODE]-VCH-[YEAR]-[ORG-SEQUENCE]
     * Example: PROJ0001-VCH-25-001
     */
    @Override
    public String generateVoucherNumber(ProjectEntity project, OrganisationEntity organisation) {

        // Get project code (already formatted as PROJ0001, PROJ0002, etc.)
        String projectCode = project.getProjectCode();

        // Get current year (2-digit format)
        String year = String.valueOf(LocalDate.now().getYear()).substring(2); // "25" from "2025"

        // Get the next sequence for this organisation
        int orgSequence = getNextOrganisationSequence(organisation.getOrganisationId());

        // Format sequence with leading zeros (001, 002, etc.)
        String paddedSequence = String.format("%03d", orgSequence);

        // Combine into the final voucher number
        String voucherNumber = String.format("%s-VCH-%s-%s", projectCode, year, paddedSequence);

        return voucherNumber;
    }

    @Override
    public String previewNextVoucherNumber(ProjectEntity project, OrganisationEntity organisation) {
        // Same logic as generateVoucherNumber() but WITHOUT incrementing
        String projectCode = project.getProjectCode();
        String year = String.valueOf(LocalDate.now().getYear()).substring(2);

        // Preview the next sequence without changing the database
        int nextSequence = sequenceRepository.findByOrganisationId(organisation.getOrganisationId())
                .map(seq -> seq.getCurrentSequence() + 1)
                .orElse(1);

        String paddedSequence = String.format("%03d", nextSequence);
        return String.format("%s-VCH-%s-%s", projectCode, year, paddedSequence);
    }

    /**
     * Gets the next sequence number for an organisation
     * Creates new sequence record if doesn't exist
     */
    private int getNextOrganisationSequence(UUID organisationId) {

        // Find or create a sequence for this organisation
        OrganisationVoucherSequenceEntity sequence = sequenceRepository
                .findByOrganisationId(organisationId)
                .orElseGet(() -> createNewOrganisationSequence(organisationId));

        // Increment sequence atomically
        sequenceRepository.incrementSequence(organisationId);

        // Return the next sequence number
        return sequence.getCurrentSequence() + 1;
    }

    /**
     * Creates a new sequence record for an organisation
     */
    private OrganisationVoucherSequenceEntity createNewOrganisationSequence(UUID organisationId) {

        OrganisationVoucherSequenceEntity newSequence = OrganisationVoucherSequenceEntity.builder()
                .organisationId(organisationId)
                .currentSequence(0)
                .build();

        return sequenceRepository.save(newSequence);
    }

    /**
     * Gets the current sequence number for an organisation (for reporting/monitoring)
     */
    public int getCurrentSequence(UUID organisationId) {
        return sequenceRepository.findByOrganisationId(organisationId)
                .map(OrganisationVoucherSequenceEntity::getCurrentSequence)
                .orElse(0);
    }
}