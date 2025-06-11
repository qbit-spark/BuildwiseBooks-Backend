package com.qbitspark.buildwisebackend.projectmng_service.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.qbitspark.buildwisebackend.clientsmng_service.entity.ClientEntity;
import com.qbitspark.buildwisebackend.organisation_service.organisation_mng.entity.OrganisationEntity;
import com.qbitspark.buildwisebackend.organisation_service.orgnisation_members_mng.entities.OrganisationMember;
import com.qbitspark.buildwisebackend.projectmng_service.enums.ProjectStatus;
import com.qbitspark.buildwisebackend.subcontractor_service.entity.SubcontractorEntity;
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

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description", nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(name = "budget", nullable = false, precision = 15, scale = 2)
    private BigDecimal budget;

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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_id")
    private OrganisationMember createdBy;

    @OneToMany(mappedBy = "project", fetch = FetchType.LAZY, cascade = {CascadeType.PERSIST, CascadeType.MERGE}, orphanRemoval = true)
    private Set<ProjectSubcontractorEntity> projectSubcontractors = new HashSet<>();

    private UUID deletedByMemberId;

    public boolean isTeamMember(OrganisationMember member) {
        return teamMembers.stream().anyMatch(teamMember -> teamMember.getMember().equals(member));
    }

    public int getTeamMembersCount() {
        return this.teamMembers != null ? this.teamMembers.size() : 0;
    }

    public int getSubcontractorsCount() {
        return this.projectSubcontractors != null ? this.projectSubcontractors.size() : 0;
    }

    public void addSubcontractor(SubcontractorEntity subcontractor) {
        ProjectSubcontractorEntity projectSubcontractor = new ProjectSubcontractorEntity();
        projectSubcontractor.setProject(this);
        projectSubcontractor.setSubcontractor(subcontractor);
        this.projectSubcontractors.add(projectSubcontractor);
        subcontractor.getProjectSubcontractors().add(projectSubcontractor);
    }

    public void removeSubcontractor(SubcontractorEntity subcontractor) {
        projectSubcontractors.removeIf(ps -> ps.getSubcontractor().equals(subcontractor));
        subcontractor.getProjectSubcontractors().removeIf(ps -> ps.getProject().equals(this));
    }

    public boolean isSubcontractorAssigned(SubcontractorEntity subcontractor) {
        return projectSubcontractors.stream().anyMatch(ps -> ps.getSubcontractor().equals(subcontractor));
    }

}