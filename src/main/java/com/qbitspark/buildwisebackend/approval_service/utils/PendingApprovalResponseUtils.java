package com.qbitspark.buildwisebackend.approval_service.utils;

import com.qbitspark.buildwisebackend.accounting_service.documentflow.invoice.entity.InvoiceDocEntity;
import com.qbitspark.buildwisebackend.accounting_service.documentflow.invoice.repo.InvoiceDocRepo;
import com.qbitspark.buildwisebackend.accounting_service.documentflow.voucher.entity.VoucherEntity;
import com.qbitspark.buildwisebackend.accounting_service.documentflow.voucher.repo.VoucherRepo;
import com.qbitspark.buildwisebackend.approval_service.entities.ApprovalInstance;
import com.qbitspark.buildwisebackend.approval_service.entities.ApprovalStepInstance;
import com.qbitspark.buildwisebackend.approval_service.enums.ServiceType;
import com.qbitspark.buildwisebackend.approval_service.enums.ScopeType;
import com.qbitspark.buildwisebackend.approval_service.payloads.PendingApprovalResponse;
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


    public PendingApprovalResponse mapToPendingApprovalResponse(ApprovalStepInstance step, AccountEntity currentUser) {
        ApprovalInstance instance = step.getApprovalInstance();

        return PendingApprovalResponse.builder()
                // Approval Instance Info
                .instanceId(instance.getInstanceId())
                .itemId(instance.getItemId())
                .serviceName(instance.getServiceName())
                .status(instance.getStatus())

                // Step Info
                .currentStep(instance.getCurrentStepOrder())
                .totalSteps(instance.getTotalSteps())
                .myStepOrder(step.getStepOrder())
                .myScopeType(step.getScopeType())
                .myRoleName(getRoleName(step.getRoleId(), step.getScopeType()))

                // Item Context - fetch based on service type
                .itemReference(getItemReference(instance.getServiceName(), instance.getItemId()))
                .itemDescription(getItemDescription(instance.getServiceName(), instance.getItemId()))
                .projectName(getProjectName(instance.getContextProjectId()))
                .clientName(getClientName(instance.getContextProjectId()))

                // Timing Info
                .submittedAt(instance.getStartedAt())
                .submittedBy(getSubmitterName(instance.getSubmittedBy()))
                .daysWaiting(calculateDaysWaiting(instance.getStartedAt()))

                // Priority/Urgency
                .priority(calculatePriority(instance))
                .isOverdue(isOverdue(instance))

                // Quick Actions
                .canApprove(permissionService.canUserApprove(currentUser, step))
                .canReject(permissionService.canUserApprove(currentUser, step))

                .build();
    }

    /**
     * Get item reference (invoice number, voucher number, etc.)
     */
    private String getItemReference(ServiceType serviceType, UUID itemId) {
        try {
            switch (serviceType) {
                case INVOICE -> {
                    Optional<InvoiceDocEntity> invoice = invoiceDocRepo.findById(itemId);
                    return invoice.map(InvoiceDocEntity::getInvoiceNumber)
                            .orElse("INV-" + itemId.toString().substring(0, 8));
                }
                case VOUCHER -> {
                    Optional<VoucherEntity> voucher = voucherRepo.findById(itemId);
                    return voucher.map(VoucherEntity::getVoucherNumber)
                            .orElse("VOU-" + itemId.toString().substring(0, 8));
                }
                case BUDGET -> {
                    return "BUDGET-" + itemId.toString().substring(0, 8);
                }
                case PROJECT -> {
                    return "PROJECT-" + itemId.toString().substring(0, 8);
                }
                default -> {
                    return serviceType.name() + "-" + itemId.toString().substring(0, 8);
                }
            }
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
            switch (serviceType) {
                case INVOICE -> {
                    Optional<InvoiceDocEntity> invoice = invoiceDocRepo.findById(itemId);
                    if (invoice.isPresent()) {
                        InvoiceDocEntity inv = invoice.get();
                        return String.format("Invoice for %s - Total: %s",
                                inv.getProject().getName(),
                                inv.getTotalAmount());
                    }
                    return "Invoice approval required";
                }
                case VOUCHER -> {
                    Optional<VoucherEntity> voucher = voucherRepo.findById(itemId);
                    if (voucher.isPresent()) {
                        VoucherEntity vou = voucher.get();
                        return String.format("Voucher for %s - Amount: %s",
                                vou.getProject().getName(),
                                vou.getTotalAmount());
                    }
                    return "Voucher approval required";
                }
                case BUDGET -> {
                    return "Budget approval required";
                }
                case PROJECT -> {
                    return "Project approval required";
                }
                default -> {
                    return serviceType.name() + " approval required";
                }
            }
        } catch (Exception e) {
            log.error("Error getting item description for serviceType: {}, itemId: {}", serviceType, itemId, e);
            return serviceType.name() + " approval required";
        }
    }

    /**
     * Get project name from project ID
     */
    private String getProjectName(UUID projectId) {
        if (projectId == null) {
            return "Unknown Project";
        }

        try {
            Optional<ProjectEntity> project = projectRepo.findById(projectId);
            return project.map(ProjectEntity::getName).orElse("Unknown Project");
        } catch (Exception e) {
            log.error("Error getting project name for projectId: {}", projectId, e);
            return "Unknown Project";
        }
    }

    /**
     * Get client name through project
     */
    private String getClientName(UUID projectId) {
        if (projectId == null) {
            return "Unknown Client";
        }

        try {
            Optional<ProjectEntity> project = projectRepo.findById(projectId);
            return project.map(p -> p.getClient().getName()).orElse("Unknown Client");
        } catch (Exception e) {
            log.error("Error getting client name for projectId: {}", projectId, e);
            return "Unknown Client";
        }
    }

    /**
     * Get submitter name from user ID
     */
    private String getSubmitterName(UUID submittedBy) {
        if (submittedBy == null) {
            return "Unknown User";
        }

        try {
            return accountRepo.findById(submittedBy)
                    .map(AccountEntity::getUserName)
                    .orElse("Unknown User");
        } catch (Exception e) {
            log.error("Error getting submitter name for userId: {}", submittedBy, e);
            return "Unknown User";
        }
    }

    /**
     * Calculate days waiting since submission
     */
    private int calculateDaysWaiting(LocalDateTime submittedAt) {
        if (submittedAt == null) {
            return 0;
        }

        try {
            return (int) java.time.temporal.ChronoUnit.DAYS.between(
                    submittedAt.toLocalDate(),
                    LocalDate.now()
            );
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

            // Business rules for priority
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
            // Business logic: consider overdue if waiting more than 5 days
            return calculateDaysWaiting(instance.getStartedAt()) > 5;
        } catch (Exception e) {
            log.error("Error checking overdue status for instance: {}", instance.getInstanceId(), e);
            return false;
        }
    }

    /**
     * Get role name based on scope type
     */
    private String getRoleName(UUID roleId, ScopeType scopeType) {
        if (roleId == null || scopeType == null) {
            return "Unknown Role";
        }

        try {
            switch (scopeType) {
                case ORGANIZATION -> {
                    Optional<OrgMemberRoleEntity> orgRole = orgMemberRoleRepo.findById(roleId);
                    return orgRole.map(OrgMemberRoleEntity::getRoleName).orElse("Unknown Role");
                }
                case PROJECT -> {
                    Optional<ProjectTeamRoleEntity> projectRole = projectTeamRoleRepo.findById(roleId);
                    return projectRole.map(ProjectTeamRoleEntity::getRoleName).orElse("Unknown Role");
                }
                default -> {
                    return "Unknown Role";
                }
            }
        } catch (Exception e) {
            log.error("Error getting role name for roleId: {}, scopeType: {}", roleId, scopeType, e);
            return "Unknown Role";
        }
    }
}