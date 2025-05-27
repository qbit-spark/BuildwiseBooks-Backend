package com.qbitspark.buildwisebackend.organisationService.repo;

import com.qbitspark.buildwisebackend.globeauthentication.entity.AccountEntity;
import com.qbitspark.buildwisebackend.organisationService.entity.OrganisationEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrganisationRepo extends JpaRepository<OrganisationEntity, UUID> {
    boolean existsByOrganisationNameAndOwner(String name, AccountEntity owner);
    Optional<OrganisationEntity> findOrganisationEntityByOwner(AccountEntity owner);
    Optional<OrganisationEntity> findByOrganisationIdAndOwner(UUID organisationId, AccountEntity owner);
    List<OrganisationEntity> findAllByOwner(AccountEntity owner);
}
