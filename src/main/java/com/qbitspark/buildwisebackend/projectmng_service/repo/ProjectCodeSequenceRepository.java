package com.qbitspark.buildwisebackend.projectmng_service.repo;

import com.qbitspark.buildwisebackend.projectmng_service.entity.ProjectCodeSequenceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface ProjectCodeSequenceRepository extends JpaRepository<ProjectCodeSequenceEntity, UUID> {

    Optional<ProjectCodeSequenceEntity> findByOrganisationId(UUID organisationId);

    @Modifying
    @Query("UPDATE ProjectCodeSequenceEntity p SET p.currentSequence = p.currentSequence + 1 WHERE p.organisationId = :orgId")
    int incrementSequence(@Param("orgId") UUID organisationId);

}