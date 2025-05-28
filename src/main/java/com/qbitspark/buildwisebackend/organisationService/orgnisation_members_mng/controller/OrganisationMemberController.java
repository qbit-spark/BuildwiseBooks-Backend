package com.qbitspark.buildwisebackend.organisationService.orgnisation_members_mng.controller;

import com.qbitspark.buildwisebackend.globeadvice.exceptions.AccessDeniedException;
import com.qbitspark.buildwisebackend.globeadvice.exceptions.ItemNotFoundException;
import com.qbitspark.buildwisebackend.globeresponsebody.GlobeSuccessResponseBuilder;
import com.qbitspark.buildwisebackend.organisationService.orgnisation_members_mng.payloads.InviteMemberRequest;
import com.qbitspark.buildwisebackend.organisationService.orgnisation_members_mng.service.OrganisationMemberService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/organisation/{orgId}/members")
@RequiredArgsConstructor
public class OrganisationMemberController {

    private final OrganisationMemberService organisationMemberService;

    @PostMapping("/invite")
    public ResponseEntity<GlobeSuccessResponseBuilder> inviteMember(
            @PathVariable UUID orgId,
            @RequestBody @Valid InviteMemberRequest request
    ) throws ItemNotFoundException, AccessDeniedException {

        boolean invited = organisationMemberService.inviteMember(
                orgId,
                request.getEmail(),
                request.getRole()
        );

        if (invited) {
            return ResponseEntity.ok(
                    GlobeSuccessResponseBuilder.success(
                            "Invitation sent successfully to " + request.getEmail()
                    )
            );
        } else {
            return ResponseEntity.badRequest().body(
                    GlobeSuccessResponseBuilder.success(
                            "Unable to send invitation. User may already be a member or have a pending invitation."
                    )
            );
        }
    }
}