package com.qbitspark.buildwisebackend.organisationservice.orgnisation_members_mng.controller;

import com.qbitspark.buildwisebackend.globeadvice.exceptions.InvitationAlreadyProcessedException;
import com.qbitspark.buildwisebackend.globeadvice.exceptions.InvitationExpiredException;
import com.qbitspark.buildwisebackend.globeadvice.exceptions.ItemNotFoundException;
import com.qbitspark.buildwisebackend.globeresponsebody.GlobeSuccessResponseBuilder;
import com.qbitspark.buildwisebackend.organisationservice.orgnisation_members_mng.payloads.InvitationInfoResponse;
import com.qbitspark.buildwisebackend.organisationservice.orgnisation_members_mng.service.OrganisationMemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/invitation")
@RequiredArgsConstructor
class OrganisationInvitationController {

    private final OrganisationMemberService organisationMemberService;

    @GetMapping("/info")
    public ResponseEntity<GlobeSuccessResponseBuilder> getInvitationInfo(
            @RequestParam String token
    ) throws ItemNotFoundException {

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
    ) throws ItemNotFoundException, InvitationExpiredException, InvitationAlreadyProcessedException {

        boolean accepted = organisationMemberService.acceptInvitation(token);

        return ResponseEntity.ok(
                GlobeSuccessResponseBuilder.success(
                        "Invitation accepted successfully! Welcome to the organisation."
                )
        );
    }

    @PostMapping("/decline")
    public ResponseEntity<GlobeSuccessResponseBuilder> declineInvitation(
            @RequestParam String token
    ) throws ItemNotFoundException, InvitationExpiredException, InvitationAlreadyProcessedException {

        boolean declined = organisationMemberService.declineInvitation(token);

        return ResponseEntity.ok(
                GlobeSuccessResponseBuilder.success(
                        "Invitation declined successfully."
                )
        );
    }
}