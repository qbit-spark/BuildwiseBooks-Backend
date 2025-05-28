package com.qbitspark.buildwisebackend.organisationService.orgnisation_members_mng.repo;

import com.qbitspark.buildwisebackend.globeauthentication.entity.AccountEntity;
import com.qbitspark.buildwisebackend.organisationService.organisation_mng.entity.OrganisationEntity;
import com.qbitspark.buildwisebackend.organisationService.orgnisation_members_mng.entities.OrganisationMember;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface OrganisationMemberRepo extends JpaRepository<OrganisationMember, UUID> {

    boolean existsByAccountEmailAndOrganisation(String email, OrganisationEntity organisation);

    Optional<OrganisationMember> findByAccountAndOrganisation(AccountEntity account, OrganisationEntity organisation);
}