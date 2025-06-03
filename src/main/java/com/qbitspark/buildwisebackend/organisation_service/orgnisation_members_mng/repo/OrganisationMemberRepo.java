package com.qbitspark.buildwisebackend.organisation_service.orgnisation_members_mng.repo;

import com.qbitspark.buildwisebackend.authentication_service.entity.AccountEntity;
import com.qbitspark.buildwisebackend.organisation_service.organisation_mng.entity.OrganisationEntity;
import com.qbitspark.buildwisebackend.organisation_service.orgnisation_members_mng.entities.OrganisationMember;
import com.qbitspark.buildwisebackend.organisation_service.orgnisation_members_mng.enums.MemberStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface OrganisationMemberRepo extends JpaRepository<OrganisationMember, UUID> {

    boolean existsByAccountEmailAndOrganisation(String email, OrganisationEntity organisation);

    Optional<OrganisationMember> findByAccountAndOrganisation(AccountEntity account, OrganisationEntity organisation);

    List<OrganisationMember> findAllByOrganisation(OrganisationEntity organisation);

    List<OrganisationMember> findAllByOrganisationAndStatus(OrganisationEntity organisation, MemberStatus status);

    long countByOrganisationAndStatus(OrganisationEntity organisation, MemberStatus status);

    List<OrganisationMember> findAllByAccount(AccountEntity account);

    Optional<OrganisationMember> findByMemberIdAndOrganisation(UUID memberId, OrganisationEntity organisation);

    Optional<OrganisationMember> findByMemberIdAndOrganisationOrganisationId(UUID memberId, UUID organisationId);

    List<OrganisationMember> findByMemberIdInAndOrganisationOrganisationIdAndStatus(
            Set<UUID> memberIds, UUID organisationId, MemberStatus status);


}