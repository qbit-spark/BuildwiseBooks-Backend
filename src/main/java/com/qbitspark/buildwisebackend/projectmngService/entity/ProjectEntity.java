package com.qbitspark.buildwisebackend.projectmngService.entity;
import com.qbitspark.buildwisebackend.clientsmngService.entity.ClientEntity;
import com.qbitspark.buildwisebackend.organisationService.organisation_mng.entity.OrganisationEntity;
import com.qbitspark.buildwisebackend.organisationService.orgnisation_members_mng.entities.OrganisationMember;
import com.qbitspark.buildwisebackend.projectmngService.enums.ProjectStatus;
import com.qbitspark.buildwisebackend.projectmngService.enums.TeamMemberRole;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
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
    @NotNull(message = "Organisation is required")
    private OrganisationEntity organisation;

    @OneToMany(mappedBy = "project", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<ProjectTeamMember> teamMembers = new HashSet<>();

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ProjectStatus status = ProjectStatus.ACTIVE;

    @CreationTimestamp
    @Column(name = "createdAt", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updatedAt")
    private LocalDateTime updatedAt;

    @Column(name = "contract_number", nullable = false, length = 100)
    @NotNull(message = "Contract number is required")
    private String contractNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_id")
    private OrganisationMember createdBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", referencedColumnName = "client_id")
    private ClientEntity client;

    public void addTeamMember(OrganisationMember member, TeamMemberRole role, String contractNumber) {
        if (member != null && role != null && contractNumber != null) {
            ProjectTeamMember teamMember = new ProjectTeamMember();
            teamMember.setProject(this);
            teamMember.setMember(member);
            teamMember.setRole(role);
            teamMember.setContractNumber(contractNumber);
            this.teamMembers.add(teamMember);
        }
    }
    // Client relationship management methods
    public void setClient(ClientEntity client) {
        // Remove from old client's projects if exists
        if (this.client != null && this.client != client) {
            this.client.getProjects().remove(this);
        }

        this.client = client;

        // Add to new client's projects if not null
        if (client != null && !client.getProjects().contains(this)) {
            client.getProjects().add(this);
        }
    }

    // Helper method to safely remove this project from client
    public void removeFromClient() {
        if (this.client != null) {
            this.client.getProjects().remove(this);
            this.client = null;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ProjectEntity)) return false;
        ProjectEntity that = (ProjectEntity) o;
        return projectId != null && projectId.equals(that.projectId);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    public void removeTeamMember(OrganisationMember member) {
        if (member != null) {
            teamMembers.removeIf(teamMember -> teamMember.getMember().equals(member));
        }
    }

    public boolean isTeamMember(OrganisationMember member) {
        return teamMembers.stream().anyMatch(teamMember -> teamMember.getMember().equals(member));
    }

    public int getTeamMembersCount() {
        return this.teamMembers != null ? this.teamMembers.size() : 0;
    }
}