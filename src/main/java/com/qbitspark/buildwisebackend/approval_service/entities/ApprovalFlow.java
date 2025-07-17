package com.qbitspark.buildwisebackend.approval_service.entities;

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
@Table(name = "approval_flows")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ApprovalFlow {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID flowId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ServiceType serviceName;

    @Column(length = 500)
    private String description;

    @ManyToOne
    @JoinColumn(name = "organisation_id", nullable = false)
    private OrganisationEntity organisation;

    @Column(nullable = false)
    private boolean isActive = true;

    @OneToMany(mappedBy = "approvalFlow", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<ApprovalStep> steps;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;
}