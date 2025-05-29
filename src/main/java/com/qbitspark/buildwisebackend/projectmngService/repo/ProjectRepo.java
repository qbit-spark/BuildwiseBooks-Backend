package com.qbitspark.buildwisebackend.projectmngService.repo;
import com.qbitspark.buildwisebackend.organisationService.organisation_mng.entity.OrganisationEntity;
import com.qbitspark.buildwisebackend.projectmngService.entity.ProjectEntity;
import com.qbitspark.buildwisebackend.projectmngService.enums.ProjectStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Repository
public interface ProjectRepo extends JpaRepository<ProjectEntity, UUID> {

    boolean existsByNameAndOrganisation(String name, OrganisationEntity organisation);

    List<ProjectEntity> findByOrganisation(OrganisationEntity organisation);

    Page<ProjectEntity> findByOrganisation(OrganisationEntity organisation, Pageable pageable);

    List<ProjectEntity> findByOrganisationOrganisationId(UUID organisationId);

    Page<ProjectEntity> findByOrganisationOrganisationId(UUID organisationId, Pageable pageable);

    List<ProjectEntity> findByStatus(ProjectStatus status);

    List<ProjectEntity> findByOrganisationAndStatus(OrganisationEntity organisation, ProjectStatus status);

    List<ProjectEntity> findByOrganisationOrganisationIdAndStatus(UUID organisationId, ProjectStatus status);

    Page<ProjectEntity> findByOrganisationOrganisationIdAndStatus(UUID organisationId, ProjectStatus status, Pageable pageable);

    long countByOrganisationOrganisationId(UUID organisationId);

    long countByOrganisationOrganisationIdAndStatus(UUID organisationId, ProjectStatus status);

    List<ProjectEntity> findByOrganisationOrganisationIdAndStatusIn(UUID organisationId, Set<ProjectStatus> statuses);

    Page<ProjectEntity> findByOrganisationOrganisationIdAndStatusIn(UUID organisationId, Set<ProjectStatus> statuses, Pageable pageable);

    List<ProjectEntity> findByOrganisationOrganisationIdOrderByCreatedAtDesc(UUID organisationId, Pageable pageable);

    Page<ProjectEntity> findByTeamMembersMemberId(UUID memberId, Pageable pageable);
}