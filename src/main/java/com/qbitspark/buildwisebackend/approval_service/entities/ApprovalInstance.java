package com.qbitspark.buildwisebackend.approval_service.entities;

import com.qbitspark.buildwisebackend.approval_service.enums.ApprovalStatus;
import com.qbitspark.buildwisebackend.approval_service.enums.ServiceType;
import com.qbitspark.buildwisebackend.organisation_service.organisation_mng.entity.OrganisationEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "approval_instances")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ApprovalInstance {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID instanceId;

    @ManyToOne
    @JoinColumn(name = "flow_id", nullable = false)
    private ApprovalFlow approvalFlow;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ServiceType serviceName;

    @Column(nullable = false)
    private UUID itemId;

    @Column
    private UUID contextProjectId;

    @ManyToOne
    @JoinColumn(name = "organisation_id", nullable = false)
    private OrganisationEntity organisation;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ApprovalStatus status;

    @Column(nullable = false)
    private int currentStepOrder;

    @Column(nullable = false)
    private int totalSteps;

    @Column(nullable = false)
    private UUID submittedBy;

    @Column(nullable = false)
    private LocalDateTime startedAt;

    @Column
    private LocalDateTime completedAt;

    @OneToMany(mappedBy = "approvalInstance", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<ApprovalStepInstance> stepInstances;
}