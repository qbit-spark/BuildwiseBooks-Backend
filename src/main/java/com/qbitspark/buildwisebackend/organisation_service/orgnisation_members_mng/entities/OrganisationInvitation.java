package com.qbitspark.buildwisebackend.organisation_service.orgnisation_members_mng.entities;

import com.qbitspark.buildwisebackend.authentication_service.entity.AccountEntity;
import com.qbitspark.buildwisebackend.organisation_service.organisation_mng.entity.OrganisationEntity;
import com.qbitspark.buildwisebackend.organisation_service.orgnisation_members_mng.enums.InvitationStatus;
import com.qbitspark.buildwisebackend.organisation_service.roles_mng.entity.MemberRoleEntity;
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
@Table(name = "organisation_invitations",
        indexes = {
                @Index(name = "idx_invitation_token", columnList = "token"),
                @Index(name = "idx_invitation_email", columnList = "email"),
                @Index(name = "idx_invitation_status", columnList = "status"),
                @Index(name = "idx_invitation_expires_at", columnList = "expiresAt"),
                @Index(name = "idx_invitation_email_org_status", columnList = "email, organisation_id, status"),
                @Index(name = "idx_invitation_org_status", columnList = "organisation_id, status"),
                @Index(name = "idx_invitation_inviter", columnList = "inviter_id")
        })
public class OrganisationInvitation {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID invitationId;

    @ManyToOne
    @JoinColumn(name = "organisation_id")
    private OrganisationEntity organisation;

    @ManyToOne
    @JoinColumn(name = "inviter_id")
    private AccountEntity inviter;

    private String email;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "role_id", nullable = false)
    private MemberRoleEntity memberRole;

    @Column(name = "token", columnDefinition = "TEXT", unique = true, nullable = false)
    private String token;

    @Enumerated(EnumType.STRING)
    private InvitationStatus status;

    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;
    private LocalDateTime respondedAt;
}