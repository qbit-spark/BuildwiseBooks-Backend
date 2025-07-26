package com.qbitspark.buildwisebackend.projectmng_service.entity;

import com.qbitspark.buildwisebackend.organisation_service.orgnisation_members_mng.entities.OrganisationMember;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "project_team_members",
        uniqueConstraints = @UniqueConstraint(columnNames = {"project_id", "member_id"}),
        indexes = {
                @Index(name = "idx_project_team_member_project", columnList = "project_id"),
                @Index(name = "idx_project_team_member_member", columnList = "member_id"),
                @Index(name = "idx_project_team_member_role", columnList = "role_id")
        })
public class ProjectTeamMemberEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private ProjectEntity project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private OrganisationMember organisationMember;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "role_id", nullable = false)
    private ProjectTeamRoleEntity projectTeamRole;

    @CreationTimestamp
    @Column(name = "joined_at", updatable = false)
    private LocalDateTime joinedAt;

    @Column(name = "added_by")
    private UUID addedBy;
}