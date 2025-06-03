package com.qbitspark.buildwisebackend.organisation_service.organisation_mng.repo;

import com.qbitspark.buildwisebackend.authentication_service.entity.AccountEntity;
import com.qbitspark.buildwisebackend.organisation_service.organisation_mng.entity.OrganisationEntity;
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
