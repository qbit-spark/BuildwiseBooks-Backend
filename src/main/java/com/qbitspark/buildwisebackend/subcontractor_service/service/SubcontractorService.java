package com.qbitspark.buildwisebackend.subcontractor_service.service;

import com.qbitspark.buildwisebackend.globeadvice.exceptions.ItemNotFoundException;
import com.qbitspark.buildwisebackend.subcontractor_service.payloads.*;

import java.util.List;
import java.util.UUID;

public interface SubcontractorService {

    SubcontractorResponse createSubcontractor(UUID organisationId, SubcontractorCreateRequest request) throws ItemNotFoundException;

    SubcontractorResponse getSubcontractorById(UUID subcontractorId) throws ItemNotFoundException;

    List<SubcontractorListResponse> getAllSubcontractors();

    List<SubcontractorListResponse> getSubcontractorsByOrganisation(UUID organisationId);

    SubcontractorResponse updateSubcontractor(UUID subcontractorId, SubcontractorUpdateRequest request) throws ItemNotFoundException;

    void deleteSubcontractor(UUID subcontractorId) throws ItemNotFoundException;

    List<ProjectResponseForSubcontractor> getSubcontractorProjects(UUID subcontractorId) throws ItemNotFoundException;

    SubcontractorResponse assignProjectToSubcontractor(UUID subcontractorId, UUID projectId) throws ItemNotFoundException;

    List<SubcontractorResponse> assignSubcontractorsToProject(UUID projectId, List<UUID> subcontractorIds) throws ItemNotFoundException;

    SubcontractorResponse removeProjectFromSubcontractor(UUID subcontractorId, UUID projectId) throws ItemNotFoundException;

    List<SubcontractorListResponse> getSubcontractorsBySpecializations(List<String> specializations);

    boolean isRegistrationNumberExists(String registrationNumber);

    boolean isEmailExists(String email);

    boolean isTinExists(String tin);
}