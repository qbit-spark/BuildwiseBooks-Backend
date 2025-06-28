package com.qbitspark.buildwisebackend.vendormng_service.repo;

import com.qbitspark.buildwisebackend.vendormng_service.entity.VendorEntity;
import com.qbitspark.buildwisebackend.organisation_service.organisation_mng.entity.OrganisationEntity;
import com.qbitspark.buildwisebackend.vendormng_service.enums.VendorStatus;
import com.qbitspark.buildwisebackend.vendormng_service.enums.VendorType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface VendorsRepo extends JpaRepository<VendorEntity, UUID> {

    boolean existsByNameIgnoreCaseAndOrganisationAndStatus(String name, OrganisationEntity organisation, VendorStatus status);
    boolean existsByEmailIgnoreCaseAndOrganisationAndStatus(String email, OrganisationEntity organisation, VendorStatus status);
    boolean existsByTinIgnoreCaseAndOrganisationAndStatus(String tin, OrganisationEntity organisation, VendorStatus status);

    List<VendorEntity> findAllByOrganisationAndStatus(OrganisationEntity organisation, VendorStatus status);
    List<VendorEntity> findAllByOrganisation(OrganisationEntity organisation);
    Optional<VendorEntity> findByVendorIdAndOrganisation(UUID vendorId, OrganisationEntity organisation);

    Page<VendorEntity> findAllByOrganisationAndStatus(OrganisationEntity organisation, VendorStatus status, Pageable pageable);
    Page<VendorEntity> findAllByOrganisation(OrganisationEntity organisation, Pageable pageable);

    List<VendorEntity> findAllByOrganisationAndStatusOrderByName(OrganisationEntity organisation, VendorStatus status);
    List<VendorEntity> findAllByOrganisationAndStatusAndVendorTypeOrderByName(OrganisationEntity organisation, VendorStatus status, VendorType vendorType);

    boolean existsByNameIgnoreCaseAndOrganisationAndStatusAndVendorIdNot(
            String name, OrganisationEntity organisation, VendorStatus status, UUID vendorId);

    boolean existsByEmailIgnoreCaseAndOrganisationAndStatusAndVendorIdNot(
            String email, OrganisationEntity organisation, VendorStatus status, UUID vendorId);

    boolean existsByTinIgnoreCaseAndOrganisationAndStatusAndVendorIdNot(
            String tin, OrganisationEntity organisation, VendorStatus status, UUID vendorId);
}