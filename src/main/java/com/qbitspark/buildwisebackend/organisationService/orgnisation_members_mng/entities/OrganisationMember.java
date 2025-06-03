package com.qbitspark.buildwisebackend.organisationservice.orgnisation_members_mng.entities;

import com.qbitspark.buildwisebackend.globeauthentication.entity.AccountEntity;
import com.qbitspark.buildwisebackend.organisationservice.organisation_mng.entity.OrganisationEntity;
import com.qbitspark.buildwisebackend.organisationservice.orgnisation_members_mng.enums.MemberRole;
import com.qbitspark.buildwisebackend.organisationservice.orgnisation_members_mng.enums.MemberStatus;
import com.qbitspark.buildwisebackend.projectmngService.entity.ProjectEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
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
                @Index(name = "idx_member_role", columnList = "role"),
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

    @Enumerated(EnumType.STRING)
    private MemberRole role;

    @Enumerated(EnumType.STRING)
    private MemberStatus status;

    private LocalDateTime joinedAt;

    private UUID invitedBy;
    @ManyToMany(fetch = FetchType.LAZY, mappedBy = "teamMembers")
    private List<ProjectEntity> projects;

}