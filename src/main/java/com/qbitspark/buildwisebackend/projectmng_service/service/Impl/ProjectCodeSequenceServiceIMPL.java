package com.qbitspark.buildwisebackend.projectmng_service.service.Impl;

import com.qbitspark.buildwisebackend.projectmng_service.entity.ProjectCodeSequenceEntity;
import com.qbitspark.buildwisebackend.projectmng_service.repo.ProjectCodeSequenceRepository;
import com.qbitspark.buildwisebackend.projectmng_service.service.ProjectCodeSequenceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@RequiredArgsConstructor
@Service
public class ProjectCodeSequenceServiceIMPL implements ProjectCodeSequenceService {

    private final ProjectCodeSequenceRepository sequenceRepo;

    @Override
    public String generateProjectCode(UUID organisationId) {
        // Find or create a sequence for organisation
        ProjectCodeSequenceEntity sequence = sequenceRepo.findByOrganisationId(organisationId)
                .orElseGet(() -> {
                    ProjectCodeSequenceEntity newSeq = new ProjectCodeSequenceEntity();
                    newSeq.setOrganisationId(organisationId);
                    newSeq.setCurrentSequence(0);
                    return sequenceRepo.save(newSeq);
                });

        // Increment sequence atomically
        sequenceRepo.incrementSequence(organisationId);

        // Get updated sequence
        int nextSequence = sequence.getCurrentSequence() + 1;

        // Format as PROJ0001, PROJ0002, etc.
        return String.format("PROJ%04d", nextSequence);
    }

}
