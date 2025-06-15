package com.qbitspark.buildwisebackend.accounting_service.documentflow.payment.repo;

import com.qbitspark.buildwisebackend.accounting_service.documentflow.payment.entity.PaymentDocEntity;
import com.qbitspark.buildwisebackend.organisation_service.organisation_mng.entity.OrganisationEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PaymentDocRepo extends JpaRepository<PaymentDocEntity, UUID> {

    Optional<PaymentDocEntity> findByPaymentNumber(String paymentNumber);

    Optional<PaymentDocEntity> findByIdAndOrganisation(UUID paymentId, OrganisationEntity organisation);

    List<PaymentDocEntity> findAllByOrganisation(OrganisationEntity organisation);

    List<PaymentDocEntity> findAllByOrganisationOrderByCreatedAtDesc(OrganisationEntity organisation);
}
