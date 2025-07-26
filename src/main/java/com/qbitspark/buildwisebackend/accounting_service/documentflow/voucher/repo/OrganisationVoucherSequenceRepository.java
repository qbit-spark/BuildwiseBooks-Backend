package com.qbitspark.buildwisebackend.accounting_service.documentflow.voucher.repo;

import com.qbitspark.buildwisebackend.accounting_service.documentflow.voucher.entity.OrganisationVoucherSequenceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface OrganisationVoucherSequenceRepository extends JpaRepository<OrganisationVoucherSequenceEntity, UUID> {

    Optional<OrganisationVoucherSequenceEntity> findByOrganisationId(UUID organisationId);

    @Modifying
    @Query("UPDATE OrganisationVoucherSequenceEntity v SET v.currentSequence = v.currentSequence + 1 " +
            "WHERE v.organisationId = :orgId")
    int incrementSequence(@Param("orgId") UUID organisationId);
}