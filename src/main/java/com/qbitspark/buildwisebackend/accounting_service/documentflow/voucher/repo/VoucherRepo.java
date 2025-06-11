package com.qbitspark.buildwisebackend.accounting_service.documentflow.voucher.repo;

import com.qbitspark.buildwisebackend.accounting_service.documentflow.voucher.entity.VoucherEntity;
import com.qbitspark.buildwisebackend.organisation_service.organisation_mng.entity.OrganisationEntity;
import com.qbitspark.buildwisebackend.projectmng_service.entity.ProjectEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface VoucherRepo extends JpaRepository<VoucherEntity, UUID> {

    Optional<VoucherEntity> findByVoucherNumber(String voucherNumber);

    Optional<VoucherEntity> findByIdAndOrganisation(UUID voucherId, OrganisationEntity organisation);

    Optional<VoucherEntity> findByVoucherNumberAndOrganisation(String voucherNumber, OrganisationEntity organisation);

    List<VoucherEntity> findAllByProject(ProjectEntity project);

    List<VoucherEntity> findAllByOrganisation(OrganisationEntity organisation);
}