package com.qbitspark.buildwisebackend.vendormng_service.repo;

import com.qbitspark.buildwisebackend.vendormng_service.entity.VendorEntity;
import com.qbitspark.buildwisebackend.organisation_service.organisation_mng.entity.OrganisationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface VendorsRepo extends JpaRepository<VendorEntity, UUID> {

    boolean existsByNameIgnoreCaseAndOrganisationAndIsActiveTrue(String name, OrganisationEntity organisation);
    boolean existsByAddressIgnoreCaseAndOrganisationAndIsActiveTrue(String address, OrganisationEntity organisation);
    boolean existsByTinIgnoreCaseAndOrganisationAndIsActiveTrue(String tin, OrganisationEntity organisation);
    boolean existsByEmailIgnoreCaseAndOrganisationAndIsActiveTrue(String email, OrganisationEntity organisation);

    Optional<VendorEntity> findVendorEntitiesByVendorIdAndOrganisation(UUID vendorId, OrganisationEntity organisation);
}