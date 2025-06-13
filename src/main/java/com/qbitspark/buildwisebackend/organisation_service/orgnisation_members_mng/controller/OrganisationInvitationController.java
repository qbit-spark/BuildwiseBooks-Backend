package com.qbitspark.buildwisebackend.organisation_service.orgnisation_members_mng.controller;

import com.qbitspark.buildwisebackend.globeadvice.exceptions.*;
import com.qbitspark.buildwisebackend.globeresponsebody.GlobeSuccessResponseBuilder;
import com.qbitspark.buildwisebackend.organisation_service.orgnisation_members_mng.payloads.AcceptInvitationResponse;
import com.qbitspark.buildwisebackend.organisation_service.orgnisation_members_mng.payloads.InvitationInfoResponse;
import com.qbitspark.buildwisebackend.organisation_service.orgnisation_members_mng.service.OrganisationMemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/invitation")
@RequiredArgsConstructor
class OrganisationInvitationController {

    private final OrganisationMemberService organisationMemberService;

    @GetMapping("/info")
    public ResponseEntity<GlobeSuccessResponseBuilder> getInvitationInfo(
            @RequestParam String token
    ) throws ItemNotFoundException, AccessDeniedException, InvitationAlreadyProcessedException, InvitationExpiredException, RandomExceptions {

        InvitationInfoResponse invitationInfo = organisationMemberService.getInvitationInfo(token);

        return ResponseEntity.ok(
                GlobeSuccessResponseBuilder.success(
                        "Invitation information retrieved successfully",
                        invitationInfo
                )
        );
    }

    @PostMapping("/accept")
    public ResponseEntity<GlobeSuccessResponseBuilder> acceptInvitation(
            @RequestParam String token
    ) throws ItemNotFoundException, InvitationExpiredException, InvitationAlreadyProcessedException, AccessDeniedException, RandomExceptions {

        AcceptInvitationResponse acceptResponse = organisationMemberService.acceptInvitation(token);

        return ResponseEntity.ok(
                GlobeSuccessResponseBuilder.success(
                        "Invitation accepted successfully! Welcome to the organisation.",acceptResponse
                )
        );
    }

    @PostMapping("/decline")
    public ResponseEntity<GlobeSuccessResponseBuilder> declineInvitation(
            @RequestParam String token
    ) throws ItemNotFoundException, InvitationExpiredException, InvitationAlreadyProcessedException, AccessDeniedException, RandomExceptions {

        boolean declined = organisationMemberService.declineInvitation(token);

        return ResponseEntity.ok(
                GlobeSuccessResponseBuilder.success(
                        "Invitation declined successfully."
                )
        );
    }

    @DeleteMapping("/revoke")
    public ResponseEntity<GlobeSuccessResponseBuilder> revokeInvitation(
            @RequestParam UUID invitationId,
            @RequestParam UUID organisationId
    ) throws ItemNotFoundException, InvitationExpiredException, InvitationAlreadyProcessedException, AccessDeniedException, RandomExceptions {

        organisationMemberService.revokeInvitation(organisationId,invitationId);

        return ResponseEntity.ok(
                GlobeSuccessResponseBuilder.success(
                        "Invitation revoked successfully."
                )
        );
    }
}