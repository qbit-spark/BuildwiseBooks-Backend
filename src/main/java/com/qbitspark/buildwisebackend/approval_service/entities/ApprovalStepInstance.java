package com.qbitspark.buildwisebackend.approval_service.entities;

import com.qbitspark.buildwisebackend.approval_service.enums.ApprovalAction;
import com.qbitspark.buildwisebackend.approval_service.enums.ScopeType;
import com.qbitspark.buildwisebackend.approval_service.enums.StepStatus;
import com.qbitspark.buildwisebackend.organisation_service.organisation_mng.entity.OrganisationEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
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

    @Column
    private UUID approvedBy;

    @Column
    private LocalDateTime approvedAt;

    @Column(columnDefinition = "TEXT")
    private String comments;

    @Enumerated(EnumType.STRING)
    private ApprovalAction action;
}