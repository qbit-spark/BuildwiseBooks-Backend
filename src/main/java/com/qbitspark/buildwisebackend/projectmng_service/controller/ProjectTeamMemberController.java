package com.qbitspark.buildwisebackend.projectmng_service.controller;

import com.qbitspark.buildwisebackend.globeadvice.exceptions.ItemNotFoundException;
import com.qbitspark.buildwisebackend.globeresponsebody.GlobeSuccessResponseBuilder;
import com.qbitspark.buildwisebackend.projectmng_service.payloads.*;
import com.qbitspark.buildwisebackend.projectmng_service.service.ProjectTeamMemberService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@RestController
//@RequestMapping("/api/v1/projects/{projectId}/team")
//Todo: changed
@RequestMapping("/api/v1/{organisationId}/projects/{projectId}/team")
@RequiredArgsConstructor
@Slf4j
public class ProjectTeamMemberController {

    private final ProjectTeamMemberService projectTeamMemberService;

    @PostMapping
    public ResponseEntity<GlobeSuccessResponseBuilder> addTeamMembers(
            @PathVariable UUID organisationId,
            @PathVariable UUID projectId,
            @Valid @RequestBody Set<BulkAddTeamMemberRequest> requests) throws Exception {

        List<ProjectTeamMemberResponse> responses = projectTeamMemberService.addTeamMembers(organisationId, projectId, requests);

        return ResponseEntity.ok(
                GlobeSuccessResponseBuilder.success(
                        String.format("Successfully added %d team members", responses.size()),
                        responses
                )
        );
    }

    @DeleteMapping
    public ResponseEntity<GlobeSuccessResponseBuilder> removeTeamMembers(
            @PathVariable UUID organisationId,
            @PathVariable UUID projectId,
            @RequestBody Set<UUID> memberIds) throws ItemNotFoundException {

        ProjectTeamRemovalResponse responses = projectTeamMemberService.removeTeamMembers(organisationId, projectId, memberIds);

        return ResponseEntity.ok(
                GlobeSuccessResponseBuilder.success(
                        "Successfully removed team member(s)",
                        responses
                )
        );
    }

    @GetMapping
    public ResponseEntity<GlobeSuccessResponseBuilder> getProjectTeamMembers(
            @PathVariable UUID organisationId,
            @PathVariable UUID projectId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String sort) throws ItemNotFoundException {


        Pageable pageable = sort != null ?
                PageRequest.of(page, size, Sort.by(sort)) :
                PageRequest.of(page, size);

        Page<ProjectTeamMemberResponse> responsePage = projectTeamMemberService.getProjectTeamMembers(organisationId, projectId, pageable);

        return ResponseEntity.ok(
                GlobeSuccessResponseBuilder.success(
                        String.format("Retrieved %d of %d team members",
                                responsePage.getNumberOfElements(),
                                responsePage.getTotalElements()),
                        responsePage
                )
        );
    }

    @PutMapping("/member/{memberId}/role")
    public ResponseEntity<GlobeSuccessResponseBuilder> updateTeamMemberRole(
            @PathVariable UUID organisationId,
            @PathVariable UUID projectId,
            @PathVariable UUID memberId,
            @Valid @RequestBody UpdateTeamMemberRoleRequest request) throws ItemNotFoundException {

        ProjectTeamMemberResponse response = projectTeamMemberService.updateTeamMemberRole(organisationId, projectId, memberId, request);

        return ResponseEntity.ok(
                GlobeSuccessResponseBuilder.success(
                        "Team member role updated successfully",
                        response
                )
        );
    }

    @GetMapping("/check")
    public ResponseEntity<GlobeSuccessResponseBuilder> checkTeamMembership(
            @PathVariable UUID organisationId,
            @PathVariable UUID projectId, @RequestParam UUID memberId) throws ItemNotFoundException {

        boolean isTeamMember = projectTeamMemberService.isTeamMember(organisationId, projectId, memberId);

        TeamMembershipCheck result = new TeamMembershipCheck(memberId, projectId, isTeamMember);

        return ResponseEntity.ok(
                GlobeSuccessResponseBuilder.success(
                        String.format("Member %s %s part of project team",
                                memberId, isTeamMember ? "is" : "is not"),
                        result
                )
        );
    }

    @GetMapping("/available-members")
    public ResponseEntity<GlobeSuccessResponseBuilder>getAvailableTeamMembers(
            @PathVariable("organisationId") UUID organisationId,
            @PathVariable("projectId") UUID projectId) throws ItemNotFoundException {


            List<AvailableTeamMemberResponse> availableMembers =
                    projectTeamMemberService.getAvailableTeamMembers(organisationId, projectId);


            return ResponseEntity.ok(
                    GlobeSuccessResponseBuilder.success(
                            "Available project member retried successfully", availableMembers
                    )
            );

    }

    // Simple response class for membership check
    public static class TeamMembershipCheck {
        public UUID memberId;
        public UUID projectId;
        public boolean isTeamMember;

        public TeamMembershipCheck(UUID memberId, UUID projectId, boolean isTeamMember) {
            this.memberId = memberId;
            this.projectId = projectId;
            this.isTeamMember = isTeamMember;
        }
    }
}