package com.qbitspark.buildwisebackend.accounting_service.deducts_mng.repo;


import com.qbitspark.buildwisebackend.accounting_service.deducts_mng.entity.DeductsEntity;
import com.qbitspark.buildwisebackend.organisation_service.organisation_mng.entity.OrganisationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DeductRepo extends JpaRepository<DeductsEntity, UUID> {
    List<DeductsEntity> findByDeductIdInAndOrganisation_OrganisationId(List<UUID> deductIds, UUID organisationId);

    List<DeductsEntity> findByOrganisation_OrganisationId(UUID organisationId);

    List<DeductsEntity> findByOrganisation_OrganisationIdAndIsActiveTrue(UUID organisationId);

    List<DeductsEntity> findByDeductIdInAndOrganisation(List<UUID> deductIds, OrganisationEntity organisation);

    Optional<DeductsEntity> findByDeductNameAndOrganisation_OrganisationId(String deductName, UUID organisationId);

    Optional<DeductsEntity> findByDeductIdAndOrganisation_OrganisationId(UUID deductId, UUID organisationId);

    List<DeductsEntity> findByDeductIdInAndOrganisationAndIsActiveTrue(List<UUID> deductIds, OrganisationEntity organisation);
}
