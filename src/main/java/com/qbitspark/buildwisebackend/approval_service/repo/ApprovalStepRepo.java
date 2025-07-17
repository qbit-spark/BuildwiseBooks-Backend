package com.qbitspark.buildwisebackend.approval_service.repo;

import com.qbitspark.buildwisebackend.approval_service.entities.ApprovalFlow;
import com.qbitspark.buildwisebackend.approval_service.entities.ApprovalStep;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ApprovalStepRepo extends JpaRepository<ApprovalStep, UUID> {
    List<ApprovalStep> findByApprovalFlowOrderByStepOrderAsc(ApprovalFlow approvalFlow);

    void deleteByApprovalFlow(ApprovalFlow approvalFlow);
}