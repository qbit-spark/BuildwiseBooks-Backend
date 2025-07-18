package com.qbitspark.buildwisebackend.organisation_service.roles_mng.repo;

import com.qbitspark.buildwisebackend.organisation_service.organisation_mng.entity.OrganisationEntity;
import com.qbitspark.buildwisebackend.organisation_service.roles_mng.entity.OrgMemberRoleEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrgMemberRoleRepo extends JpaRepository<OrgMemberRoleEntity, UUID> {
    Optional<OrgMemberRoleEntity> findByOrganisationAndRoleName(OrganisationEntity organisation, String roleName);
    Optional<OrgMemberRoleEntity> findByOrganisationAndRoleId(OrganisationEntity organisation, UUID roleId);

    boolean existsByOrganisationAndRoleNameIgnoreCase(OrganisationEntity organisation, String roleName);

    List<OrgMemberRoleEntity> findByOrganisationAndIsActiveTrue(OrganisationEntity organisation);


}
