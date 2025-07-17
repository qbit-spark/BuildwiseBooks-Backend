package com.qbitspark.buildwisebackend.projectmng_service.repo;

import com.qbitspark.buildwisebackend.organisation_service.organisation_mng.entity.OrganisationEntity;
import com.qbitspark.buildwisebackend.projectmng_service.entity.ProjectTeamRoleEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProjectTeamRoleRepo extends JpaRepository<ProjectTeamRoleEntity, UUID> {

    Optional<ProjectTeamRoleEntity> findByOrganisationAndRoleName(OrganisationEntity organisation, String roleName);

    boolean existsByOrganisationAndRoleNameIgnoreCase(OrganisationEntity organisation, String roleName);

    List<ProjectTeamRoleEntity> findByOrganisationAndIsActiveTrue(OrganisationEntity organisation);


    List<ProjectTeamRoleEntity> findByOrganisationAndIsActiveTrueAndIsDefaultRoleTrue(OrganisationEntity organisation);

    long countByOrganisationAndIsActiveTrue(OrganisationEntity organisation);
}