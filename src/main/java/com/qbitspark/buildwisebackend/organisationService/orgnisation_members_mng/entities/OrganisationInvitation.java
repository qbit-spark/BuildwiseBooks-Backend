package com.qbitspark.buildwisebackend.organisationService.orgnisation_members_mng.entities;

import com.qbitspark.buildwisebackend.globeauthentication.entity.AccountEntity;
import com.qbitspark.buildwisebackend.organisationService.organisation_mng.entity.OrganisationEntity;
import com.qbitspark.buildwisebackend.organisationService.orgnisation_members_mng.enums.InvitationStatus;
import com.qbitspark.buildwisebackend.organisationService.orgnisation_members_mng.enums.MemberRole;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "organisation_invitations")
public class OrganisationInvitation {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID invitationId;

    @ManyToOne
    private OrganisationEntity organisation;

    @ManyToOne
    private AccountEntity inviter;

    private String email;

    @Enumerated(EnumType.STRING)
    private MemberRole role;

    @Column(name = "token", columnDefinition = "TEXT", unique = true, nullable = false)
    private String token; // Unique invitation token

    @Enumerated(EnumType.STRING)
    private InvitationStatus status; // PENDING, ACCEPTED, DECLINED, EXPIRED

    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;
    private LocalDateTime respondedAt;

}