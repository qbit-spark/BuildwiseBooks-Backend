package com.qbitspark.buildwisebackend.organisationService.orgnisation_members_mng.entities;

import com.qbitspark.buildwisebackend.globeauthentication.entity.AccountEntity;
import com.qbitspark.buildwisebackend.organisationService.organisation_mng.entity.OrganisationEntity;
import com.qbitspark.buildwisebackend.organisationService.orgnisation_members_mng.enums.MemberRole;
import com.qbitspark.buildwisebackend.organisationService.orgnisation_members_mng.enums.MemberStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "organisation_members")
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
    private MemberRole role; // OWNER, ADMIN, MEMBER

    @Enumerated(EnumType.STRING)
    private MemberStatus status; // ACTIVE, SUSPENDED, LEFT

    private LocalDateTime joinedAt;

    private UUID invitedBy;

}