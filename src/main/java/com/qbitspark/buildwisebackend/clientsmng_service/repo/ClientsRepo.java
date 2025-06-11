package com.qbitspark.buildwisebackend.clientsmng_service.repo;

import com.qbitspark.buildwisebackend.clientsmng_service.entity.ClientEntity;
import com.qbitspark.buildwisebackend.organisation_service.organisation_mng.entity.OrganisationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ClientsRepo extends JpaRepository<ClientEntity, UUID> {
    boolean existsByNameIgnoreCaseAndOrganisationAndIsActiveTrue(String name, OrganisationEntity organisation);
    boolean existsByAddressIgnoreCaseAndOrganisationAndIsActiveTrue(String address, OrganisationEntity organisation);
    boolean existsByTinIgnoreCaseAndOrganisationAndIsActiveTrue(String tin, OrganisationEntity organisation);
    boolean existsByEmailIgnoreCaseAndOrganisationAndIsActiveTrue(String email, OrganisationEntity organisation);

    Optional<ClientEntity> findClientEntitiesByClientIdAndOrganisation(UUID clientId, OrganisationEntity organisation);
}