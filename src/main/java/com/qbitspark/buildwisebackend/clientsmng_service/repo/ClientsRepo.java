package com.qbitspark.buildwisebackend.clientsmng_service.repo;

import com.qbitspark.buildwisebackend.clientsmng_service.entity.ClientEntity;
import com.qbitspark.buildwisebackend.organisation_service.organisation_mng.entity.OrganisationEntity;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ClientsRepo extends JpaRepository<ClientEntity, UUID> {
    boolean existsByNameIgnoreCaseAndOrganisationAndIsActiveTrue(String name, OrganisationEntity organisation);
    boolean existsByAddressIgnoreCaseAndOrganisationAndIsActiveTrue(String address, OrganisationEntity organisation);
    boolean existsByTinIgnoreCaseAndOrganisationAndIsActiveTrue(String tin, OrganisationEntity organisation);
    boolean existsByEmailIgnoreCaseAndOrganisationAndIsActiveTrue(String email, OrganisationEntity organisation);

    Optional<ClientEntity> findClientEntitiesByClientIdAndOrganisation(UUID clientId, OrganisationEntity organisation);

}