package com.qbitspark.buildwisebackend.approval_service.entities;

import com.qbitspark.buildwisebackend.approval_service.entities.embedings.ApprovalRecord;
import com.qbitspark.buildwisebackend.approval_service.entities.embedings.RejectionRecord;
import com.qbitspark.buildwisebackend.approval_service.enums.*;
import com.qbitspark.buildwisebackend.approval_service.utils.ApprovalRecordListConverter;
import com.qbitspark.buildwisebackend.approval_service.utils.RejectionRecordListConverter;
import com.qbitspark.buildwisebackend.organisation_service.organisation_mng.entity.OrganisationEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "approval_step_instances")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ApprovalStepInstance {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID stepInstanceId;

    @ManyToOne
    @JoinColumn(name = "organisation_id", nullable = false)
    private OrganisationEntity organisation;

    @ManyToOne
    @JoinColumn(name = "instance_id", nullable = false)
    private ApprovalInstance approvalInstance;

    @Column(nullable = false)
    private int stepOrder;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ScopeType scopeType;

    @Column(nullable = false)
    private UUID roleId;

    @Column(nullable = false)
    private boolean isRequired;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StepStatus status;

    // Current state fields (can be cleared/reset during workflow)
    @Column
    private UUID approvedBy;

    @Column
    private LocalDateTime approvedAt;

    @Column(columnDefinition = "TEXT")
    private String comments;

    @Enumerated(EnumType.STRING)
    private ApprovalAction action;

    @Column(name = "approval_history", columnDefinition = "jsonb")
    @Convert(converter = ApprovalRecordListConverter.class)
    private List<ApprovalRecord> approvalHistory = new ArrayList<>();

    @Column(name = "rejection_history", columnDefinition = "jsonb")
    @Convert(converter = RejectionRecordListConverter.class)
    private List<RejectionRecord> rejectionHistory = new ArrayList<>();


    public void addApprovalToHistory(UUID approvedBy, String approvedByEmail, String comments, Integer revisionNumber) {
        // Mark all previous approvals as superseded
        approvalHistory.forEach(ApprovalRecord::markAsSuperseded);

        ApprovalRecord newApproval = ApprovalRecord.builder()
                .approvalId(UUID.randomUUID())
                .stepId(this.stepInstanceId)
                .approvedAt(LocalDateTime.now())
                .approvedBy(approvedByEmail)
                .comments(comments)
                .revisionNumber(revisionNumber)
                .status(ApprovalRecordStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .build();

        approvalHistory.add(newApproval);
    }

    public void addRejectionToHistory(String rejectedByEmail, String rejectionReason, Integer revisionNumber) {
        RejectionRecord newRejection = RejectionRecord.builder()
                .rejectionId(UUID.randomUUID())
                .stepId(this.stepInstanceId)
                .rejectedAt(LocalDateTime.now())
                .rejectedBy(rejectedByEmail)
                .rejectionReason(rejectionReason)
                .revisionNumber(revisionNumber)
                .status(RejectionRecordStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .build();

        rejectionHistory.add(newRejection);
    }

    /**
     * Mark all active rejections as resolved
     */
    public void resolveActiveRejections(String resolvedByEmail, String resolutionComments) {
        rejectionHistory.stream()
                .filter(RejectionRecord::isActive)
                .forEach(rejection -> rejection.markAsResolved(resolvedByEmail, resolutionComments));
    }

    /**
     * Get the current revision number (for next approval/rejection)
     */
    public Integer getNextRevisionNumber() {
        int maxApprovalRevision = approvalHistory.stream()
                .mapToInt(ApprovalRecord::getRevisionNumber)
                .max()
                .orElse(0);

        int maxRejectionRevision = rejectionHistory.stream()
                .mapToInt(RejectionRecord::getRevisionNumber)
                .max()
                .orElse(0);

        return Math.max(maxApprovalRevision, maxRejectionRevision) + 1;
    }

    /**
     * Get total number of approvals
     */
    public int getTotalApprovals() {
        return approvalHistory.size();
    }

    /**
     * Get total number of rejections
     */
    public int getTotalRejections() {
        return rejectionHistory.size();
    }

    /**
     * Check if there are any active rejections
     */
    public boolean hasActiveRejections() {
        return rejectionHistory.stream().anyMatch(RejectionRecord::isActive);
    }

    /**
     * Get the latest approval (if any)
     */
    public ApprovalRecord getLatestApproval() {
        return approvalHistory.stream()
                .filter(ApprovalRecord::isActive)
                .findFirst()
                .orElse(null);
    }

    /**
     * Get the latest rejection (if any)
     */
    public RejectionRecord getLatestRejection() {
        return rejectionHistory.stream()
                .filter(RejectionRecord::isActive)
                .findFirst()
                .orElse(null);
    }
}