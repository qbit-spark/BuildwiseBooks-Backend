package com.qbitspark.buildwisebackend.organisationservice.orgnisation_members_mng.controller;

import com.qbitspark.buildwisebackend.globeadvice.exceptions.ItemNotFoundException;
import com.qbitspark.buildwisebackend.globeresponsebody.GlobeSuccessResponseBuilder;
import com.qbitspark.buildwisebackend.organisationservice.orgnisation_members_mng.payloads.UserOrganisationsOverviewResponse;
import com.qbitspark.buildwisebackend.organisationservice.orgnisation_members_mng.service.OrganisationMemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/my/organisations")
@RequiredArgsConstructor
public class UserOrganisationController {

    private final OrganisationMemberService organisationMemberService;

    @GetMapping
    public ResponseEntity<GlobeSuccessResponseBuilder> getMyOrganisations() throws ItemNotFoundException {

        UserOrganisationsOverviewResponse myOrganisations = organisationMemberService.getMyOrganisations();

        return ResponseEntity.ok(
                GlobeSuccessResponseBuilder.success(
                        "Your organisations retrieved successfully",
                        myOrganisations
                )
        );
    }
}