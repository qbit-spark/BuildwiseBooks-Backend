package com.qbitspark.buildwisebackend.projectmng_service.controller;

import com.qbitspark.buildwisebackend.globeadvice.exceptions.ItemNotFoundException;
import com.qbitspark.buildwisebackend.globeresponsebody.GlobeSuccessResponseBuilder;
import com.qbitspark.buildwisebackend.projectmng_service.payloads.BulkAddTeamMemberRequest;
import com.qbitspark.buildwisebackend.projectmng_service.payloads.ProjectTeamMemberResponse;
import com.qbitspark.buildwisebackend.projectmng_service.payloads.ProjectTeamRemovalResponse;
import com.qbitspark.buildwisebackend.projectmng_service.payloads.UpdateTeamMemberRoleRequest;
import com.qbitspark.buildwisebackend.projectmng_service.service.ProjectTeamMemberService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/projects/{projectId}/team")
@RequiredArgsConstructor
@Slf4j
public class ProjectTeamMemberController {

    private final ProjectTeamMemberService projectTeamMemberService;

    @PostMapping()
    public ResponseEntity<GlobeSuccessResponseBuilder> addTeamMembers(
            @PathVariable UUID projectId,
            @Valid @RequestBody Set<BulkAddTeamMemberRequest> requests) throws Exception {

        List<ProjectTeamMemberResponse> responses = projectTeamMemberService.addTeamMembers(projectId, requests);

        return ResponseEntity.ok(
                GlobeSuccessResponseBuilder.success(
                        String.format("Successfully added %d team members", responses.size()),
                        responses
                )
        );
    }

    @DeleteMapping()
    public ResponseEntity<GlobeSuccessResponseBuilder> removeTeamMembers(
            @PathVariable UUID projectId,
            @RequestBody Set<UUID> memberIds) throws ItemNotFoundException {

        ProjectTeamRemovalResponse responses = projectTeamMemberService.removeTeamMembers(projectId, memberIds);

        return ResponseEntity.ok(
                GlobeSuccessResponseBuilder.success(
                        "Successfully removed %d team members",
                        responses
                )
        );
    }

    @GetMapping
    public ResponseEntity<GlobeSuccessResponseBuilder> getProjectTeamMembers(
            @PathVariable UUID projectId) throws ItemNotFoundException {

        List<ProjectTeamMemberResponse> responses = projectTeamMemberService.getProjectTeamMembers(projectId);

        return ResponseEntity.ok(
                GlobeSuccessResponseBuilder.success(
                        String.format("Retrieved %d team members", responses.size()),
                        responses
                )
        );
    }

    @PutMapping("/member/{memberId}/role")
    public ResponseEntity<GlobeSuccessResponseBuilder> updateTeamMemberRole(
            @PathVariable UUID projectId,
            @PathVariable UUID memberId,
            @Valid @RequestBody UpdateTeamMemberRoleRequest request) throws ItemNotFoundException {

        ProjectTeamMemberResponse response = projectTeamMemberService.updateTeamMemberRole(projectId, memberId, request);

        return ResponseEntity.ok(
                GlobeSuccessResponseBuilder.success(
                        "Team member role updated successfully",
                        response
                )
        );
    }

    @GetMapping("/check")
    public ResponseEntity<GlobeSuccessResponseBuilder> checkTeamMembership(
            @PathVariable UUID projectId, @RequestParam UUID memberId) throws ItemNotFoundException {

        boolean isTeamMember = projectTeamMemberService.isTeamMember(projectId, memberId);

        TeamMembershipCheck result = new TeamMembershipCheck(memberId, projectId, isTeamMember);

        return ResponseEntity.ok(
                GlobeSuccessResponseBuilder.success(
                        String.format("Member %s %s part of project team",
                                memberId, isTeamMember ? "is" : "is not"),
                        result
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