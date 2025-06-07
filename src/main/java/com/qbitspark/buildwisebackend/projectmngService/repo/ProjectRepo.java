package com.qbitspark.buildwisebackend.projectmngService.repo;

import com.qbitspark.buildwisebackend.organisationService.organisation_mng.entity.OrganisationEntity;
import com.qbitspark.buildwisebackend.projectmngService.entity.ProjectEntity;
import com.qbitspark.buildwisebackend.projectmngService.enums.ProjectStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ProjectRepo extends JpaRepository<ProjectEntity, UUID> {

    boolean existsByNameAndOrganisation(String name, OrganisationEntity organisation);

    List<ProjectEntity> findByOrganisationOrganisationId(UUID organisationId);

    Page<ProjectEntity> findByOrganisationOrganisationIdAndStatus(UUID organisationId, ProjectStatus status, Pageable pageable);

    long countByOrganisationOrganisationId(UUID organisationId);

    long countByOrganisationOrganisationIdAndStatus(UUID organisationId, ProjectStatus status);

    Page<ProjectEntity> findByStatusNot(ProjectStatus projectStatus, Pageable pageable);

    @Query("SELECT p FROM ProjectEntity p JOIN p.teamMembers tm WHERE tm.member.memberId = :memberId AND p.status != :status")
    Page<ProjectEntity> findByTeamMembersMemberIdAndStatusNot(@Param("memberId") UUID memberId, @Param("status") ProjectStatus status, Pageable pageable);

    Page<ProjectEntity> findByOrganisationOrganisationIdAndStatusNot(UUID organisationId, ProjectStatus projectStatus, Pageable pageable);

    // Find projects by client ID and ProjectStatus enum
    @Query("SELECT p FROM ProjectEntity p WHERE p.client.clientId = :clientId AND p.status = :status")
    List<ProjectEntity> findByClientIdAndProjectStatus(@Param("clientId") UUID clientId, @Param("status") ProjectStatus status);

    // Find active projects for a client (assuming you have an active status)
    @Query("SELECT p FROM ProjectEntity p WHERE p.client.clientId = :clientId AND p.status != 'CANCELLED' AND p.status != 'COMPLETED'")
    List<ProjectEntity> findActiveProjectsByClientId(@Param("clientId") UUID clientId);

    List<ProjectEntity> findByClientClientId(UUID clientId);

    Page<ProjectEntity> findByClientClientId(UUID clientId, Pageable pageable);

    Long countByClientClientId(UUID clientId);

    // For status queries, use explicit JPQL to avoid issues
    @Query("SELECT p FROM ProjectEntity p WHERE p.client.clientId = :clientId AND p.status = :status")
    List<ProjectEntity> findByClientIdAndStatus(@Param("clientId") UUID clientId, @Param("status") String status);

    // Exists query
    boolean existsByNameAndOrganisationOrganisationId(String name, UUID organisationId);
}