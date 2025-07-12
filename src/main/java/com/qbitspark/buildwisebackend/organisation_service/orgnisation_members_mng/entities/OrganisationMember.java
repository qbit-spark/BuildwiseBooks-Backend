package com.qbitspark.buildwisebackend.organisation_service.orgnisation_members_mng.entities;

import com.qbitspark.buildwisebackend.authentication_service.entity.AccountEntity;
import com.qbitspark.buildwisebackend.organisation_service.organisation_mng.entity.OrganisationEntity;
import com.qbitspark.buildwisebackend.organisation_service.orgnisation_members_mng.enums.MemberStatus;
import com.qbitspark.buildwisebackend.organisation_service.roles_mng.entity.MemberRoleEntity;
import com.qbitspark.buildwisebackend.projectmng_service.entity.ProjectTeamMemberEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "organisation_members",
        indexes = {
                @Index(name = "idx_member_org_account", columnList = "organisation_id, account_id", unique = true),
                @Index(name = "idx_member_account_email_org", columnList = "account_id, organisation_id"),
                @Index(name = "idx_member_organisation", columnList = "organisation_id"),
                @Index(name = "idx_member_account", columnList = "account_id"),
                @Index(name = "idx_member_status", columnList = "status"),
                @Index(name = "idx_member_joined_at", columnList = "joinedAt"),
                @Index(name = "idx_member_invited_by", columnList = "invitedBy")
        })
public class OrganisationMember {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID memberId;

    @ManyToOne
    @JoinColumn(name = "organisation_id")
    private OrganisationEntity organisation;

    @ManyToOne
    @JoinColumn(name = "account_id")
    private AccountEntity account;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "role_id", nullable = false)
    private MemberRoleEntity memberRole;

    @Enumerated(EnumType.STRING)
    private MemberStatus status;

    private LocalDateTime joinedAt;

    private UUID invitedBy;

    @OneToMany(mappedBy = "organisationMember", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<ProjectTeamMemberEntity> projectMemberships = new HashSet<>();
}