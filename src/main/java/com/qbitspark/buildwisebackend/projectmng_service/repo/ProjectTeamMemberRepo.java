package com.qbitspark.buildwisebackend.projectmng_service.repo;

import com.qbitspark.buildwisebackend.organisation_service.orgnisation_members_mng.entities.OrganisationMember;
import com.qbitspark.buildwisebackend.projectmng_service.entity.ProjectEntity;
import com.qbitspark.buildwisebackend.projectmng_service.entity.ProjectTeamMemberEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProjectTeamMemberRepo extends JpaRepository<ProjectTeamMemberEntity, UUID> {
    List<ProjectTeamMemberEntity> findByProjectProjectId(UUID projectId);
    List<ProjectTeamMemberEntity> findByProject(ProjectEntity project);

    Page<ProjectTeamMemberEntity> findByProject(ProjectEntity project, Pageable pageable);

    Optional<ProjectTeamMemberEntity> findByOrganisationMemberAndProject(OrganisationMember organisationMember, ProjectEntity project);

    ProjectTeamMemberEntity findByProject_ProjectIdAndOrganisationMember_MemberId(UUID projectId, UUID memberId);
}