package com.qbitspark.buildwisebackend.accounting_service.tax_mng.repo;

import com.qbitspark.buildwisebackend.accounting_service.tax_mng.entity.TaxEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TaxRepo extends JpaRepository<TaxEntity, UUID> {

    List<TaxEntity> findByOrganisation_OrganisationId(UUID organisationId);

    List<TaxEntity> findByOrganisation_OrganisationIdAndIsActiveTrue(UUID organisationId);

    Optional<TaxEntity> findByTaxNameAndOrganisation_OrganisationId(String taxName, UUID organisationId);

    Optional<TaxEntity> findByTaxIdAndOrganisation_OrganisationId(UUID taxId, UUID organisationId);
}