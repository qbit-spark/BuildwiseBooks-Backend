package com.qbitspark.buildwisebackend.approval_service.utils;

import com.qbitspark.buildwisebackend.accounting_service.documentflow.invoice.entity.InvoiceDocEntity;
import com.qbitspark.buildwisebackend.accounting_service.documentflow.invoice.repo.InvoiceDocRepo;
import com.qbitspark.buildwisebackend.accounting_service.documentflow.voucher.entity.VoucherEntity;
import com.qbitspark.buildwisebackend.accounting_service.documentflow.voucher.repo.VoucherRepo;
import com.qbitspark.buildwisebackend.approval_service.entities.ApprovalInstance;
import com.qbitspark.buildwisebackend.approval_service.entities.ApprovalStepInstance;
import com.qbitspark.buildwisebackend.approval_service.entities.embedings.RejectionRecord;
import com.qbitspark.buildwisebackend.approval_service.enums.RejectionRecordStatus;
import com.qbitspark.buildwisebackend.approval_service.enums.ServiceType;
import com.qbitspark.buildwisebackend.approval_service.enums.ScopeType;
import com.qbitspark.buildwisebackend.approval_service.payloads.PendingApprovalResponse;
import com.qbitspark.buildwisebackend.approval_service.repo.ApprovalStepInstanceRepo;
import com.qbitspark.buildwisebackend.approval_service.service.ApprovalPermissionService;
import com.qbitspark.buildwisebackend.authentication_service.Repository.AccountRepo;
import com.qbitspark.buildwisebackend.authentication_service.entity.AccountEntity;
import com.qbitspark.buildwisebackend.organisation_service.roles_mng.entity.OrgMemberRoleEntity;
import com.qbitspark.buildwisebackend.organisation_service.roles_mng.repo.OrgMemberRoleRepo;
import com.qbitspark.buildwisebackend.projectmng_service.entity.ProjectEntity;
import com.qbitspark.buildwisebackend.projectmng_service.entity.ProjectTeamRoleEntity;
import com.qbitspark.buildwisebackend.projectmng_service.repo.ProjectRepo;
import com.qbitspark.buildwisebackend.projectmng_service.repo.ProjectTeamRoleRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PendingApprovalResponseUtils {

    // Repository dependencies
    private final AccountRepo accountRepo;
    private final ProjectRepo projectRepo;
    private final InvoiceDocRepo invoiceDocRepo;
    private final VoucherRepo voucherRepo;
    private final OrgMemberRoleRepo orgMemberRoleRepo;
    private final ProjectTeamRoleRepo projectTeamRoleRepo;
    private final ApprovalPermissionService permissionService;
    private final ApprovalStepInstanceRepo approvalStepInstanceRepo;

    // =========================================================================================
    // MAIN MAPPING METHOD
    // =========================================================================================

    /**
     * 🎯 Main method to convert ApprovalStepInstance to PendingApprovalResponse
     */
    public PendingApprovalResponse mapToPendingApprovalResponse(ApprovalStepInstance step, AccountEntity currentUser) {
        ApprovalInstance instance = step.getApprovalInstance();

        log.debug("Mapping pending approval for step {} in instance {}", step.getStepOrder(), instance.getInstanceId());

        try {
            // Build basic response
            PendingApprovalResponse.PendingApprovalResponseBuilder builder = PendingApprovalResponse.builder()
                    // Instance info
                    .instanceId(instance.getInstanceId())
                    .itemId(instance.getItemId())
                    .serviceName(instance.getServiceName())
                    .status(instance.getStatus())

                    // Step info
                    .currentStep(instance.getCurrentStepOrder())
                    .totalSteps(instance.getTotalSteps())
                    .myStepOrder(step.getStepOrder())
                    .myScopeType(step.getScopeType())
                    .myRoleName(getRoleName(step.getRoleId(), step.getScopeType()))

                    // Item context
                    .itemReference(getItemReference(instance.getServiceName(), instance.getItemId()))
                    .itemDescription(getItemDescription(instance.getServiceName(), instance.getItemId()))
                    .projectName(getProjectName(instance.getContextProjectId()))
                    .clientName(getClientName(instance.getContextProjectId()))

                    // Timing info
                    .submittedAt(instance.getStartedAt())
                    .submittedBy(getSubmitterName(instance.getSubmittedBy()))
                    .daysWaiting(calculateDaysWaiting(instance.getStartedAt()))

                    // Priority
                    .priority(calculatePriority(instance))
                    .isOverdue(isOverdue(instance))

                    // Actions
                    .canApprove(permissionService.canUserApprove(currentUser, step))
                    .canReject(permissionService.canUserApprove(currentUser, step));


            // Add rejection context
            PendingApprovalResponse.RejectionContext rejectionContext = analyzeRejectionContext(instance, step);
            builder.hasRejectionHistory(rejectionContext.isComingBackFromRejection())
                    .rejectionContext(rejectionContext);

            return builder.build();

        } catch (Exception e) {
            log.error("Error mapping pending approval response for step {} in instance {}",
                    step.getStepOrder(), instance.getInstanceId(), e);

            // Return minimal response on error
            return createMinimalResponse(step, currentUser);
        }
    }

    // =========================================================================================
    // REJECTION CONTEXT ANALYSIS
    // =========================================================================================

    /**
     * 🔍 Analyze rejection context to understand why this step is pending
     */
    private PendingApprovalResponse.RejectionContext analyzeRejectionContext(ApprovalInstance instance, ApprovalStepInstance currentStep) {
        try {
            // Get all steps for this instance
            List<ApprovalStepInstance> allSteps = approvalStepInstanceRepo
                    .findByApprovalInstanceOrderByStepOrderAsc(instance);

            // Check for rejections from future steps (came back from higher step)
            Optional<ApprovalStepInstance> futureRejectedStep = allSteps.stream()
                    .filter(step -> step.getStepOrder() > currentStep.getStepOrder())
                    .filter(this::hasActiveRejections)
                    .findFirst();

            if (futureRejectedStep.isPresent()) {
                return buildFutureRejectionContext(futureRejectedStep.get(), currentStep);
            }

            // Check if current step has its own rejection history
            if (hasActiveRejections(currentStep)) {
                return buildCurrentStepRejectionContext(currentStep);
            }

            // No rejection context
            return PendingApprovalResponse.RejectionContext.builder()
                    .isComingBackFromRejection(false)
                    .build();

        } catch (Exception e) {
            log.error("Error analyzing rejection context for step {} in instance {}",
                    currentStep.getStepOrder(), instance.getInstanceId(), e);

            return PendingApprovalResponse.RejectionContext.builder()
                    .isComingBackFromRejection(false)
                    .build();
        }
    }

    /**
     * Build rejection context when rejected by a future step
     */
    private PendingApprovalResponse.RejectionContext buildFutureRejectionContext(ApprovalStepInstance rejectedStep, ApprovalStepInstance currentStep) {
        RejectionRecord latestRejection = getLatestActiveRejection(rejectedStep);

        if (latestRejection == null) {
            return PendingApprovalResponse.RejectionContext.builder()
                    .isComingBackFromRejection(false)
                    .build();
        }

        String rejectedStepRoleName = getRoleName(rejectedStep.getRoleId(), rejectedStep.getScopeType());
        int totalRejections = countTotalRejections(rejectedStep.getApprovalInstance());

        return PendingApprovalResponse.RejectionContext.builder()
                .isComingBackFromRejection(true)
                .rejectedByRole(rejectedStepRoleName)
                .rejectedByUser(latestRejection.getRejectedBy())
                .rejectedAt(latestRejection.getRejectedAt())
                .rejectionReason(latestRejection.getRejectionReason())
                .rejectedFromStep(rejectedStep.getStepOrder())
                .rejectedFromStepName(rejectedStepRoleName)
                .timesRejected(totalRejections)
                .contextMessage(String.format("Returned by %s: %s", rejectedStepRoleName, latestRejection.getRejectionReason()))
                .actionRequired(String.format("Address %s's concerns before re-approving", rejectedStepRoleName))
                .build();
    }

    /**
     * Build rejection context for current step's own rejections
     */
    private PendingApprovalResponse.RejectionContext buildCurrentStepRejectionContext(ApprovalStepInstance currentStep) {
        RejectionRecord latestRejection = getLatestActiveRejection(currentStep);

        if (latestRejection == null) {
            return PendingApprovalResponse.RejectionContext.builder()
                    .isComingBackFromRejection(false)
                    .build();
        }

        String roleName = getRoleName(currentStep.getRoleId(), currentStep.getScopeType());
        int timesRejected = currentStep.getRejectionHistory().size();

        return PendingApprovalResponse.RejectionContext.builder()
                .isComingBackFromRejection(true)
                .rejectedByRole(roleName)
                .rejectedByUser(latestRejection.getRejectedBy())
                .rejectedAt(latestRejection.getRejectedAt())
                .rejectionReason(latestRejection.getRejectionReason())
                .rejectedFromStep(currentStep.getStepOrder())
                .rejectedFromStepName(roleName)
                .timesRejected(timesRejected)
                .contextMessage(String.format("Previously rejected: %s", latestRejection.getRejectionReason()))
                .actionRequired("Address the rejection concerns and re-approve")
                .build();
    }

    // =========================================================================================
    // ITEM CONTEXT METHODS
    // =========================================================================================

    /**
     * Get item reference (invoice number, voucher number, etc.)
     */
    private String getItemReference(ServiceType serviceType, UUID itemId) {
        try {
            return switch (serviceType) {
                case INVOICE -> invoiceDocRepo.findById(itemId)
                        .map(InvoiceDocEntity::getInvoiceNumber)
                        .orElse("INV-" + itemId.toString().substring(0, 8));

                case VOUCHER -> voucherRepo.findById(itemId)
                        .map(VoucherEntity::getVoucherNumber)
                        .orElse("VOU-" + itemId.toString().substring(0, 8));

                case BUDGET -> "BUDGET-" + itemId.toString().substring(0, 8);
                case PROJECT -> "PROJECT-" + itemId.toString().substring(0, 8);
                case RECEIPT -> "RECEIPT-" + itemId.toString().substring(0, 8);
                case ORDERS -> "ORDER-" + itemId.toString().substring(0, 8);
                default -> serviceType.name() + "-" + itemId.toString().substring(0, 8);
            };
        } catch (Exception e) {
            log.error("Error getting item reference for serviceType: {}, itemId: {}", serviceType, itemId, e);
            return serviceType.name() + "-" + itemId.toString().substring(0, 8);
        }
    }

    /**
     * Get item description based on service type
     */
    private String getItemDescription(ServiceType serviceType, UUID itemId) {
        try {
            return switch (serviceType) {
                case INVOICE -> invoiceDocRepo.findById(itemId)
                        .map(inv -> String.format("Invoice for %s - Total: %s",
                                inv.getProject().getName(), inv.getTotalAmount()))
                        .orElse("Invoice approval required");

                case VOUCHER -> voucherRepo.findById(itemId)
                        .map(vou -> String.format("Voucher for %s - Amount: %s",
                                vou.getProject().getName(), vou.getTotalAmount()))
                        .orElse("Voucher approval required");

                case BUDGET -> "Budget approval required";
                case PROJECT -> "Project approval required";
                case RECEIPT -> "Receipt approval required";
                case ORDERS -> "Order approval required";
                default -> serviceType.name() + " approval required";
            };
        } catch (Exception e) {
            log.error("Error getting item description for serviceType: {}, itemId: {}", serviceType, itemId, e);
            return serviceType.name() + " approval required";
        }
    }

    // =========================================================================================
    // PROJECT & CLIENT CONTEXT
    // =========================================================================================

    /**
     * Get project name from project ID
     */
    private String getProjectName(UUID projectId) {
        if (projectId == null) return "Unknown Project";

        try {
            return projectRepo.findById(projectId)
                    .map(ProjectEntity::getName)
                    .orElse("Unknown Project");
        } catch (Exception e) {
            log.error("Error getting project name for projectId: {}", projectId, e);
            return "Unknown Project";
        }
    }

    /**
     * Get client name through project
     */
    private String getClientName(UUID projectId) {
        if (projectId == null) return "Unknown Client";

        try {
            return projectRepo.findById(projectId)
                    .map(project -> project.getClient().getName())
                    .orElse("Unknown Client");
        } catch (Exception e) {
            log.error("Error getting client name for projectId: {}", projectId, e);
            return "Unknown Client";
        }
    }

    // =========================================================================================
    // ROLE & USER CONTEXT
    // =========================================================================================

    /**
     * Get role name based on scope type
     */
    private String getRoleName(UUID roleId, ScopeType scopeType) {
        if (roleId == null || scopeType == null) return "Unknown Role";

        try {
            return switch (scopeType) {
                case ORGANIZATION -> orgMemberRoleRepo.findById(roleId)
                        .map(OrgMemberRoleEntity::getRoleName)
                        .orElse("Unknown Role");

                case PROJECT -> projectTeamRoleRepo.findById(roleId)
                        .map(ProjectTeamRoleEntity::getRoleName)
                        .orElse("Unknown Role");

                default -> "Unknown Role";
            };
        } catch (Exception e) {
            log.error("Error getting role name for roleId: {}, scopeType: {}", roleId, scopeType, e);
            return "Unknown Role";
        }
    }

    /**
     * Get submitter name from user ID
     */
    private String getSubmitterName(UUID submittedBy) {
        if (submittedBy == null) return "Unknown User";

        try {
            return accountRepo.findById(submittedBy)
                    .map(AccountEntity::getUserName)
                    .orElse("Unknown User");
        } catch (Exception e) {
            log.error("Error getting submitter name for userId: {}", submittedBy, e);
            return "Unknown User";
        }
    }

    // =========================================================================================
    // TIMING & PRIORITY CALCULATIONS
    // =========================================================================================

    /**
     * Calculate days waiting since submission
     */
    private int calculateDaysWaiting(LocalDateTime submittedAt) {
        if (submittedAt == null) return 0;

        try {
            return (int) java.time.temporal.ChronoUnit.DAYS.between(
                    submittedAt.toLocalDate(), LocalDate.now());
        } catch (Exception e) {
            log.error("Error calculating days waiting for submittedAt: {}", submittedAt, e);
            return 0;
        }
    }

    /**
     * Calculate priority based on business rules
     */
    private String calculatePriority(ApprovalInstance instance) {
        try {
            int daysWaiting = calculateDaysWaiting(instance.getStartedAt());
            boolean hasRejections = countTotalRejections(instance) > 0;

            // Higher priority for rejected items
            if (hasRejections && daysWaiting > 5) return "CRITICAL";
            if (hasRejections && daysWaiting > 2) return "HIGH";
            if (daysWaiting > 7) return "HIGH";
            if (daysWaiting > 3) return "MEDIUM";
            return "LOW";

        } catch (Exception e) {
            log.error("Error calculating priority for instance: {}", instance.getInstanceId(), e);
            return "LOW";
        }
    }

    /**
     * Check if approval is overdue
     */
    private boolean isOverdue(ApprovalInstance instance) {
        try {
            int daysWaiting = calculateDaysWaiting(instance.getStartedAt());
            boolean hasRejections = countTotalRejections(instance) > 0;

            // Different thresholds for rejected vs new items
            return hasRejections ? daysWaiting > 3 : daysWaiting > 5;
        } catch (Exception e) {
            log.error("Error checking overdue status for instance: {}", instance.getInstanceId(), e);
            return false;
        }
    }

    // =========================================================================================
    // HELPER METHODS
    // =========================================================================================

    /**
     * Check if step has active rejections
     */
    private boolean hasActiveRejections(ApprovalStepInstance step) {
        return step.getRejectionHistory() != null &&
                step.getRejectionHistory().stream()
                        .anyMatch(rejection -> RejectionRecordStatus.ACTIVE.equals(rejection.getStatus()));
    }

    /**
     * Get latest active rejection from step
     */
    private RejectionRecord getLatestActiveRejection(ApprovalStepInstance step) {
        if (step.getRejectionHistory() == null) return null;

        return step.getRejectionHistory().stream()
                .filter(rejection -> RejectionRecordStatus.ACTIVE.equals(rejection.getStatus()))
                .findFirst()
                .orElse(null);
    }

    /**
     * Count total rejections across all steps in an instance
     */
    private int countTotalRejections(ApprovalInstance instance) {
        try {
            List<ApprovalStepInstance> allSteps = approvalStepInstanceRepo
                    .findByApprovalInstanceOrderByStepOrderAsc(instance);

            return allSteps.stream()
                    .mapToInt(step -> step.getRejectionHistory() != null ? step.getRejectionHistory().size() : 0)
                    .sum();
        } catch (Exception e) {
            log.error("Error counting total rejections for instance: {}", instance.getInstanceId(), e);
            return 0;
        }
    }


    /**
     * Create minimal response on error
     */
    private PendingApprovalResponse createMinimalResponse(ApprovalStepInstance step, AccountEntity currentUser) {
        ApprovalInstance instance = step.getApprovalInstance();

        return PendingApprovalResponse.builder()
                .instanceId(instance.getInstanceId())
                .itemId(instance.getItemId())
                .serviceName(instance.getServiceName())
                .status(instance.getStatus())
                .myStepOrder(step.getStepOrder())
                .itemReference("ERROR-" + instance.getItemId().toString().substring(0, 8))
                .itemDescription("Error loading item details")
                .hasRejectionHistory(false)
                .rejectionContext(PendingApprovalResponse.RejectionContext.builder()
                        .isComingBackFromRejection(false)
                        .build())
                .canApprove(false)
                .canReject(false)
                .build();
    }
}