package com.qbitspark.buildwisebackend.projectmngService.entity;

import com.qbitspark.buildwisebackend.organisationService.orgnisation_members_mng.entities.OrganisationMember;
import com.qbitspark.buildwisebackend.projectmngService.enums.TeamMemberRole;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "project_team_members",
        indexes = {
                @Index(name = "idx_project_team_member_project", columnList = "project_id"),
                @Index(name = "idx_project_team_member_member", columnList = "member_id")
        })
public class ProjectTeamMember {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private ProjectEntity project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private OrganisationMember member;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    private TeamMemberRole role;

    @Column(name = "contract_number", nullable = false, length = 100)
    private String contractNumber;
}