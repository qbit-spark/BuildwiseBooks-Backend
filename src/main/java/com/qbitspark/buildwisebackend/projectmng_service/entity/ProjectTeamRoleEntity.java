package com.qbitspark.buildwisebackend.projectmng_service.entity;

import com.qbitspark.buildwisebackend.organisation_service.organisation_mng.entity.OrganisationEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "project_team_roles",
        uniqueConstraints = @UniqueConstraint(columnNames = {"organisation_id", "role_name"}),
        indexes = {
                @Index(name = "idx_project_team_role_org", columnList = "organisation_id"),
                @Index(name = "idx_project_team_role_name", columnList = "role_name"),
                @Index(name = "idx_project_team_role_active", columnList = "is_active")
        })
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class ProjectTeamRoleEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID roleId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organisation_id", nullable = false)
    private OrganisationEntity organisation;

    @Column(name = "role_name", nullable = false, length = 50)
    private String roleName;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "is_default_role", nullable = false)
    private Boolean isDefaultRole = false;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "created_by")
    private UUID createdBy;

    @CreationTimestamp
    @Column(name = "created_date", updatable = false)
    private LocalDateTime createdDate;

    @UpdateTimestamp
    @Column(name = "updated_date")
    private LocalDateTime updatedDate;
}