package com.qbitspark.buildwisebackend.approval_service.repo;

import com.qbitspark.buildwisebackend.approval_service.entities.ApprovalFlow;
import com.qbitspark.buildwisebackend.approval_service.enums.ServiceType;
import com.qbitspark.buildwisebackend.organisation_service.organisation_mng.entity.OrganisationEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ApprovalFlowRepo extends JpaRepository<ApprovalFlow, UUID> {

    Optional<ApprovalFlow> findByServiceNameAndOrganisationAndIsActiveTrue(ServiceType serviceName, OrganisationEntity organisation);

    List<ApprovalFlow> findByOrganisationAndIsActiveTrue(OrganisationEntity organisation);

    boolean existsByServiceNameAndOrganisationAndIsActiveTrue(ServiceType serviceName, OrganisationEntity organisation);

    Optional<ApprovalFlow> findByOrganisationAndFlowId(OrganisationEntity organisation, UUID flowId);
}