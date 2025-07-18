package com.qbitspark.buildwisebackend.organisation_service.orgnisation_members_mng.repo;

import com.qbitspark.buildwisebackend.authentication_service.entity.AccountEntity;
import com.qbitspark.buildwisebackend.organisation_service.organisation_mng.entity.OrganisationEntity;
import com.qbitspark.buildwisebackend.organisation_service.orgnisation_members_mng.entities.OrganisationMember;
import com.qbitspark.buildwisebackend.organisation_service.orgnisation_members_mng.enums.MemberStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrganisationMemberRepo extends JpaRepository<OrganisationMember, UUID> {

    boolean existsByAccountEmailAndOrganisation(String email, OrganisationEntity organisation);

    Optional<OrganisationMember> findByAccountAndOrganisation(AccountEntity account, OrganisationEntity organisation);

    Optional<OrganisationMember> findByOrganisationAndMemberRole_RoleName(OrganisationEntity organisation, String roleName);

    List<OrganisationMember> findAllByOrganisation(OrganisationEntity organisation);

    List<OrganisationMember> findAllByOrganisationAndStatus(OrganisationEntity organisation, MemberStatus status);

    long countByOrganisationAndStatus(OrganisationEntity organisation, MemberStatus status);

    List<OrganisationMember> findAllByAccount(AccountEntity account);

    Optional<OrganisationMember> findByMemberIdAndOrganisation(UUID memberId, OrganisationEntity organisation);

    List<OrganisationMember> findByOrganisationAndStatus(OrganisationEntity organisation, MemberStatus status);

  OrganisationMember findByAccountIdAndOrganisation_OrganisationId(UUID accountId, UUID organisationId);

    OrganisationMember findByAccount_IdAndOrganisation_OrganisationId(UUID accountId, UUID organisationId);

    @Query("SELECT COUNT(om) > 0 FROM OrganisationMember om " +
            "WHERE om.account.id = :userId " +
            "AND om.organisation.organisationId = :orgId " +
            "AND om.memberRole.roleId = :roleId " +
            "AND om.memberRole.isActive = true " +
            "AND om.status = 'ACTIVE'")
    boolean hasActiveOrgRole(@Param("userId") UUID userId,
                             @Param("orgId") UUID orgId,
                             @Param("roleId") UUID roleId);

    OrganisationMember findByAccount_Id(UUID accountId);

}