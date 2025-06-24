package com.qbitspark.buildwisebackend.projectmng_service.repo;
import com.qbitspark.buildwisebackend.clientsmng_service.entity.ClientEntity;
import com.qbitspark.buildwisebackend.organisation_service.organisation_mng.entity.OrganisationEntity;
import com.qbitspark.buildwisebackend.organisation_service.orgnisation_members_mng.entities.OrganisationMember;
import com.qbitspark.buildwisebackend.projectmng_service.entity.ProjectEntity;
import com.qbitspark.buildwisebackend.projectmng_service.enums.ProjectStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Repository
public interface ProjectRepo extends JpaRepository<ProjectEntity, UUID> {
    boolean existsByNameAndOrganisation(String name, OrganisationEntity organisation);
    Page<ProjectEntity> findAllByOrganisation(OrganisationEntity organisation, Pageable pageable);
    List<ProjectEntity> findAllByClientAndOrganisation(ClientEntity client, OrganisationEntity organisation);
    /**
     * Find projects by team member and exclude deleted ones
     */
    Page<ProjectEntity> findByTeamMembersOrganisationMemberAndStatusNot(OrganisationMember member, ProjectStatus status, Pageable pageable);
    List<ProjectEntity> findByTeamMembersOrganisationMemberAndStatusNot(OrganisationMember member, ProjectStatus status);

}