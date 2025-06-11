package com.qbitspark.buildwisebackend.subcontractor_service.controller;

import com.qbitspark.buildwisebackend.globeadvice.exceptions.ItemNotFoundException;
import com.qbitspark.buildwisebackend.globeresponsebody.GlobeSuccessResponseBuilder;
import com.qbitspark.buildwisebackend.subcontractor_service.payloads.*;
import com.qbitspark.buildwisebackend.subcontractor_service.service.SubcontractorService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/v1/subcontractor")
@RequiredArgsConstructor
public class SubcontractorController {

    private final SubcontractorService subcontractorService;

    @PostMapping("/organisation/{organisationId}")
    public ResponseEntity<GlobeSuccessResponseBuilder> createSubcontractor(
            @PathVariable UUID organisationId,
            @RequestBody @Validated SubcontractorCreateRequest request) throws ItemNotFoundException {
        SubcontractorResponse subcontractorResponse = subcontractorService.createSubcontractor(organisationId, request);
        return ResponseEntity.ok(GlobeSuccessResponseBuilder.success("Subcontractor created successfully", subcontractorResponse));
    }

    @GetMapping("/{subcontractorId}")
    public ResponseEntity<GlobeSuccessResponseBuilder> getSubcontractor(
            @PathVariable UUID subcontractorId) throws ItemNotFoundException {
        SubcontractorResponse subcontractorResponse = subcontractorService.getSubcontractorById(subcontractorId);
        return ResponseEntity.ok(GlobeSuccessResponseBuilder.success("Subcontractor retrieved successfully", subcontractorResponse));
    }

    @GetMapping
    public ResponseEntity<GlobeSuccessResponseBuilder> getAllSubcontractors() {
        List<SubcontractorListResponse> subcontractors = subcontractorService.getAllSubcontractors();
        return ResponseEntity.ok(GlobeSuccessResponseBuilder.success("Subcontractors retrieved successfully", subcontractors));
    }

    @GetMapping("/organisation/{organisationId}")
    public ResponseEntity<GlobeSuccessResponseBuilder> getSubcontractorsByOrganisation(
            @PathVariable UUID organisationId) {
        List<SubcontractorListResponse> subcontractors = subcontractorService.getSubcontractorsByOrganisation(organisationId);
        return ResponseEntity.ok(GlobeSuccessResponseBuilder.success("Subcontractors retrieved successfully", subcontractors));
    }

    @PutMapping("/{subcontractorId}")
    public ResponseEntity<GlobeSuccessResponseBuilder> updateSubcontractor(
            @PathVariable UUID subcontractorId,
            @RequestBody @Validated SubcontractorUpdateRequest request) throws ItemNotFoundException {
        SubcontractorResponse subcontractorResponse = subcontractorService.updateSubcontractor(subcontractorId, request);
        return ResponseEntity.ok(GlobeSuccessResponseBuilder.success("Subcontractor updated successfully", subcontractorResponse));
    }

    @DeleteMapping("/{subcontractorId}")
    public ResponseEntity<GlobeSuccessResponseBuilder> deleteSubcontractor(
            @PathVariable UUID subcontractorId) throws ItemNotFoundException {
        subcontractorService.deleteSubcontractor(subcontractorId);
        return ResponseEntity.ok(GlobeSuccessResponseBuilder.success("Subcontractor deleted successfully", null));
    }

    @GetMapping("/{subcontractorId}/projects")
    public ResponseEntity<GlobeSuccessResponseBuilder> getSubcontractorProjects(
            @PathVariable UUID subcontractorId) throws ItemNotFoundException {
        List<ProjectResponseForSubcontractor> projects = subcontractorService.getSubcontractorProjects(subcontractorId);
        return ResponseEntity.ok(GlobeSuccessResponseBuilder.success("Subcontractor projects retrieved successfully", projects));
    }

    @PostMapping("/projects/{projectId}/assign")
    public ResponseEntity<GlobeSuccessResponseBuilder> assignSubcontractorsToProject(
            @PathVariable UUID projectId,
            @RequestBody @Validated SubcontractorAssignRequest request) throws ItemNotFoundException {
        List<SubcontractorResponse> responses = subcontractorService.assignSubcontractorsToProject(projectId, request.getSubcontractorIds());
        return ResponseEntity.ok(GlobeSuccessResponseBuilder.success("Subcontractors assigned to project successfully", responses));
    }

    @PostMapping("/{subcontractorId}/projects/{projectId}")
    public ResponseEntity<GlobeSuccessResponseBuilder> assignProjectToSubcontractor(
            @PathVariable UUID subcontractorId,
            @PathVariable UUID projectId) throws ItemNotFoundException {
        SubcontractorResponse subcontractorResponse = subcontractorService.assignProjectToSubcontractor(subcontractorId, projectId);
        return ResponseEntity.ok(GlobeSuccessResponseBuilder.success("Project assigned to subcontractor successfully", subcontractorResponse));
    }

    @DeleteMapping("/{subcontractorId}/projects/{projectId}")
    public ResponseEntity<GlobeSuccessResponseBuilder> removeProjectFromSubcontractor(
            @PathVariable UUID subcontractorId,
            @PathVariable UUID projectId) throws ItemNotFoundException {
        SubcontractorResponse subcontractorResponse = subcontractorService.removeProjectFromSubcontractor(subcontractorId, projectId);
        return ResponseEntity.ok(GlobeSuccessResponseBuilder.success("Project removed from subcontractor successfully", subcontractorResponse));
    }

    @GetMapping("/specializations")
    public ResponseEntity<GlobeSuccessResponseBuilder> getSubcontractorsBySpecializations(
            @RequestParam List<String> specializations) {
        List<SubcontractorListResponse> subcontractors = subcontractorService.getSubcontractorsBySpecializations(specializations);
        return ResponseEntity.ok(GlobeSuccessResponseBuilder.success("Subcontractors by specializations retrieved successfully", subcontractors));
    }

    @GetMapping("/check/registration-number")
    public ResponseEntity<GlobeSuccessResponseBuilder> checkRegistrationNumber(
            @RequestParam String registrationNumber) {
        boolean exists = subcontractorService.isRegistrationNumberExists(registrationNumber);
        String message = exists ? "Registration number already exists" : "Registration number is available";
        return ResponseEntity.ok(GlobeSuccessResponseBuilder.success(message, exists));
    }

    @GetMapping("/check/email")
    public ResponseEntity<GlobeSuccessResponseBuilder> checkEmail(
            @RequestParam String email) {
        boolean exists = subcontractorService.isEmailExists(email);
        String message = exists ? "Email already exists" : "Email is available";
        return ResponseEntity.ok(GlobeSuccessResponseBuilder.success(message, exists));
    }

    @GetMapping("/check/tin")
    public ResponseEntity<GlobeSuccessResponseBuilder> checkTin(
            @RequestParam String tin) {
        boolean exists = subcontractorService.isTinExists(tin);
        String message = exists ? "TIN already exists" : "TIN is available";
        return ResponseEntity.ok(GlobeSuccessResponseBuilder.success(message, exists));
    }
}