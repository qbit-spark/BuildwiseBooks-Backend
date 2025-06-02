package com.qbitspark.buildwisebackend.projectmngService.entity;
import com.qbitspark.buildwisebackend.organisationService.organisation_mng.entity.OrganisationEntity;
import com.qbitspark.buildwisebackend.organisationService.orgnisation_members_mng.entities.OrganisationMember;
import com.qbitspark.buildwisebackend.projectmngService.enums.ProjectStatus;
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

    // Many-to-One relationship with Organisation
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organisation_id", nullable = false)
    @NotNull(message = "Organisation is required")
    private OrganisationEntity organisation;

    // Many-to-Many relationship with Organisation Members (Team)
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "project_team_members",
            joinColumns = @JoinColumn(name = "project_id", referencedColumnName = "project_id"),
            inverseJoinColumns = @JoinColumn(name = "member_id", referencedColumnName = "memberId")
    )
    private Set<OrganisationMember> teamMembers = new HashSet<>();

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ProjectStatus status = ProjectStatus.ACTIVE;

    @CreationTimestamp
    @Column(name = "createdAt", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updatedAt")
    private LocalDateTime updatedAt;

    // Created by (tracking who created the project)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_id")
    private OrganisationMember createdBy;

    // Helper method to add a team member to the project
    public void addTeamMember(OrganisationMember member) {
        if (member != null) {
            this.teamMembers.add(member);
            member.getProjects().add(this); // Maintain bidirectional relationship
        }
    }

    // Helper method to remove a team member from the project
    public void removeTeamMember(OrganisationMember member) {
        if (member != null) {
            this.teamMembers.remove(member);
            member.getProjects().remove(this); // Maintain bidirectional relationship
        }
    }

    // Helper method to check if a member is part of the project
    public boolean isTeamMember(OrganisationMember member) {
        return this.teamMembers.contains(member);
    }

    // Helper method to get the count of team members in the project
    public int getTeamMembersCount() {
        return this.teamMembers != null ? this.teamMembers.size() : 0;
    }
}
