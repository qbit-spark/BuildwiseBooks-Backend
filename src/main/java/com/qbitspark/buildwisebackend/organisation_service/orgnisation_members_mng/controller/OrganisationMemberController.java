package com.qbitspark.buildwisebackend.organisation_service.orgnisation_members_mng.controller;

import com.qbitspark.buildwisebackend.globeadvice.exceptions.AccessDeniedException;
import com.qbitspark.buildwisebackend.globeadvice.exceptions.ItemNotFoundException;
import com.qbitspark.buildwisebackend.globeadvice.exceptions.RandomExceptions;
import com.qbitspark.buildwisebackend.globeresponsebody.GlobeSuccessResponseBuilder;
import com.qbitspark.buildwisebackend.organisation_service.orgnisation_members_mng.payloads.InviteMemberRequest;
import com.qbitspark.buildwisebackend.organisation_service.orgnisation_members_mng.payloads.OrganisationMemberResponse;
import com.qbitspark.buildwisebackend.organisation_service.orgnisation_members_mng.payloads.OrganisationMembersOverviewResponse;
import com.qbitspark.buildwisebackend.organisation_service.orgnisation_members_mng.payloads.PendingInvitationResponse;
import com.qbitspark.buildwisebackend.organisation_service.orgnisation_members_mng.service.OrganisationMemberService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/members/{organisationId}")
@RequiredArgsConstructor
public class OrganisationMemberController {

    private final OrganisationMemberService organisationMemberService;

    @PostMapping("/invite")
    public ResponseEntity<GlobeSuccessResponseBuilder> inviteMember(
            @PathVariable UUID organisationId,
            @RequestBody @Valid InviteMemberRequest request
    ) throws ItemNotFoundException, AccessDeniedException, RandomExceptions {

        boolean invited = organisationMemberService.inviteMember(
                organisationId,
                request.getEmail(),
                request.getRoleId()
        );

        if (invited) {
            return ResponseEntity.ok(
                    GlobeSuccessResponseBuilder.success(
                            "Invitation sent successfully to " + request.getEmail()
                    )
            );
        } else {
            throw new RandomExceptions("Unable to send invitation. User may already be a member or have a pending invitation.");
        }
    }

    @GetMapping
    public ResponseEntity<GlobeSuccessResponseBuilder> getAllMembersAndInvitations(
            @PathVariable UUID organisationId
    ) throws ItemNotFoundException, AccessDeniedException {

        OrganisationMembersOverviewResponse overview = organisationMemberService.getAllMembersAndInvitations(organisationId);

        return ResponseEntity.ok(
                GlobeSuccessResponseBuilder.success(
                        "Organisation members and invitations retrieved successfully",
                        overview
                )
        );
    }

    @GetMapping("/active")
    public ResponseEntity<GlobeSuccessResponseBuilder> getActiveMembers(
            @PathVariable UUID organisationId
    ) throws ItemNotFoundException, AccessDeniedException {

        List<OrganisationMemberResponse> activeMembers = organisationMemberService.getActiveMembers(organisationId);

        return ResponseEntity.ok(
                GlobeSuccessResponseBuilder.success(
                        "Active members retrieved successfully",
                        activeMembers
                )
        );
    }

    @GetMapping("/invitations/pending")
    public ResponseEntity<GlobeSuccessResponseBuilder> getPendingInvitations(
            @PathVariable UUID organisationId
    ) throws ItemNotFoundException, AccessDeniedException {

        List<PendingInvitationResponse> pendingInvitations = organisationMemberService.getPendingInvitations(organisationId);

        return ResponseEntity.ok(
                GlobeSuccessResponseBuilder.success(
                        "Pending invitations retrieved successfully",
                        pendingInvitations
                )
        );
    }

    @DeleteMapping("/{memberId}")
    public ResponseEntity<GlobeSuccessResponseBuilder> removeMember(
            @PathVariable UUID organisationId,
            @PathVariable UUID memberId
    ) throws ItemNotFoundException, AccessDeniedException, RandomExceptions {

        boolean removed = organisationMemberService.removeMember(organisationId, memberId);

        if (removed) {
            return ResponseEntity.ok(
                    GlobeSuccessResponseBuilder.success("Member removed successfully")
            );
        } else {
            throw new RandomExceptions("Unable to remove member. Member may not exist or you may not have permission to remove them.");
        }
    }
}