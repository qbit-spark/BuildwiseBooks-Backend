package com.qbitspark.buildwisebackend.organisationservice.organisation_mng.repo;

import com.qbitspark.buildwisebackend.globeauthentication.entity.AccountEntity;
import com.qbitspark.buildwisebackend.organisationservice.organisation_mng.entity.OrganisationEntity;
import com.qbitspark.buildwisebackend.organisationservice.orgnisation_members_mng.entities.OrganisationMember;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrganisationRepo extends JpaRepository<OrganisationEntity, UUID> {
    boolean existsByOrganisationNameAndOwner(String name, AccountEntity owner);
    Optional<OrganisationEntity> findOrganisationEntityByOwner(AccountEntity owner);
    Optional<OrganisationEntity> findByOrganisationIdAndOwner(UUID organisationId, AccountEntity owner);
    List<OrganisationEntity> findAllByOwner(AccountEntity owner);
    // Add this method to your OrganisationMemberRepo interface

}
