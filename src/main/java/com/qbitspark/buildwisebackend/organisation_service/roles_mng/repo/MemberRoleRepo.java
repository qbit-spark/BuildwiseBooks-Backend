package com.qbitspark.buildwisebackend.organisation_service.roles_mng.repo;

import com.qbitspark.buildwisebackend.organisation_service.organisation_mng.entity.OrganisationEntity;
import com.qbitspark.buildwisebackend.organisation_service.roles_mng.entity.MemberRoleEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MemberRoleRepo extends JpaRepository<MemberRoleEntity, UUID> {
    Optional<MemberRoleEntity> findByOrganisationAndRoleName(OrganisationEntity organisation, String roleName);

    boolean existsByOrganisationAndRoleNameIgnoreCase(OrganisationEntity organisation, String roleName);

    List<MemberRoleEntity> findByOrganisationAndIsActiveTrue(OrganisationEntity organisation);
}
