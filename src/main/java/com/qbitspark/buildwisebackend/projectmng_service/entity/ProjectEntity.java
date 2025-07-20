package com.qbitspark.buildwisebackend.projectmng_service.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.qbitspark.buildwisebackend.clientsmng_service.entity.ClientEntity;
import com.qbitspark.buildwisebackend.organisation_service.organisation_mng.entity.OrganisationEntity;
import com.qbitspark.buildwisebackend.organisation_service.orgnisation_members_mng.entities.OrganisationMember;
import com.qbitspark.buildwisebackend.projectmng_service.enums.ProjectStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "projects",
        uniqueConstraints = @UniqueConstraint(columnNames = {"name", "organisation_id"}),
        indexes = {
                @Index(name = "idx_project_organisation", columnList = "organisation_id"),
                @Index(name = "idx_project_status", columnList = "status"),
                @Index(name = "idx_project_created_by", columnList = "created_by_id"),
                @Index(name = "idx_project_name", columnList = "name"),
                @Index(name = "idx_project_created_at", columnList = "createdAt")
        })
public class ProjectEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "project_id", nullable = false, updatable = false)
    private UUID projectId;

    //This should be changed later to  nullable = false
    @Column(name = "project_code", columnDefinition = "TEXT")
    private String projectCode;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description", nullable = false, columnDefinition = "TEXT")
    private String description;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organisation_id", nullable = false)
    private OrganisationEntity organisation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", nullable = false)
    private ClientEntity client;

    @JsonIgnore
    @OneToMany(mappedBy = "project", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<ProjectTeamMemberEntity> teamMembers = new HashSet<>();

    @Enumerated(EnumType.STRING)
    private ProjectStatus status = ProjectStatus.ACTIVE;

    @CreationTimestamp
    @Column(name = "createdAt", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updatedAt")
    private LocalDateTime updatedAt;

    @Column(name = "contract_number", nullable = false, length = 100)
    private String contractNumber;

    @Column(name = "contract_sum")
    private BigDecimal contractSum;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_id")
    private OrganisationMember createdBy;

    private UUID deletedByMemberId;



    public boolean isTeamMember(OrganisationMember member) {
        return teamMembers.stream().anyMatch(teamMember -> teamMember.getOrganisationMember().equals(member));
    }

}