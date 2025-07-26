package com.qbitspark.buildwisebackend.approval_service.repo;

import com.qbitspark.buildwisebackend.approval_service.entities.ApprovalInstance;
import com.qbitspark.buildwisebackend.approval_service.enums.ApprovalStatus;
import com.qbitspark.buildwisebackend.approval_service.enums.ServiceType;
import com.qbitspark.buildwisebackend.organisation_service.organisation_mng.entity.OrganisationEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ApprovalInstanceRepo extends JpaRepository<ApprovalInstance, UUID> {

    Optional<ApprovalInstance> findByServiceNameAndItemId(ServiceType serviceName, UUID itemId);

    List<ApprovalInstance> findByOrganisationAndStatus(OrganisationEntity organisation, ApprovalStatus status);

    List<ApprovalInstance> findBySubmittedBy(UUID submittedBy);

    boolean existsByServiceNameAndItemIdAndStatusIn(ServiceType serviceName, UUID itemId, List<ApprovalStatus> statuses);
}