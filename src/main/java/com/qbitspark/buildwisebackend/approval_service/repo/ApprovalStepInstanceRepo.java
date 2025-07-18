package com.qbitspark.buildwisebackend.approval_service.repo;

import com.qbitspark.buildwisebackend.approval_service.entities.ApprovalInstance;
import com.qbitspark.buildwisebackend.approval_service.entities.ApprovalStepInstance;
import com.qbitspark.buildwisebackend.approval_service.enums.StepStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ApprovalStepInstanceRepo extends JpaRepository<ApprovalStepInstance, UUID> {

    List<ApprovalStepInstance> findByApprovalInstanceOrderByStepOrderAsc(ApprovalInstance approvalInstance);

    Optional<ApprovalStepInstance> findByApprovalInstanceAndStepOrder(ApprovalInstance approvalInstance, int stepOrder);

    List<ApprovalStepInstance> findByRoleIdAndStatus(UUID roleId, StepStatus status);

    List<ApprovalStepInstance> findByApprovalInstanceAndStatus(ApprovalInstance approvalInstance, StepStatus status);

   List<ApprovalStepInstance> findByStatus(StepStatus status);

}