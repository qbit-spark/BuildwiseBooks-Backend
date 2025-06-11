package com.qbitspark.buildwisebackend.subcontractor_service.service.Impl;

import com.qbitspark.buildwisebackend.globeadvice.exceptions.ItemNotFoundException;
import com.qbitspark.buildwisebackend.globeadvice.exceptions.DuplicateResourceException;
import com.qbitspark.buildwisebackend.organisation_service.organisation_mng.entity.OrganisationEntity;
import com.qbitspark.buildwisebackend.organisation_service.organisation_mng.repo.OrganisationRepo;
import com.qbitspark.buildwisebackend.projectmng_service.entity.ProjectEntity;
import com.qbitspark.buildwisebackend.projectmng_service.entity.ProjectSubcontractorEntity;
import com.qbitspark.buildwisebackend.projectmng_service.repo.ProjectRepo;
import com.qbitspark.buildwisebackend.projectmng_service.repo.ProjectSubContractorRepo;
import com.qbitspark.buildwisebackend.subcontractor_service.entity.SubcontractorEntity;
import com.qbitspark.buildwisebackend.subcontractor_service.payloads.*;
import com.qbitspark.buildwisebackend.subcontractor_service.repo.SubcontractorRepo;
import com.qbitspark.buildwisebackend.subcontractor_service.service.SubcontractorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Hibernate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class SubcontractorServiceImpl implements SubcontractorService {

    private final SubcontractorRepo subcontractorRepository;
    private final OrganisationRepo organisationRepository;
    private final ProjectRepo projectRepository;
    private final ProjectSubContractorRepo projectSubcontractorRepository; // Add repository for ProjectSubcontractorEntity

    @Override
    public SubcontractorResponse createSubcontractor(UUID organisationId, SubcontractorCreateRequest request) throws ItemNotFoundException {
        log.info("Creating subcontractor with company name: {}", request.getCompanyName());

        if (subcontractorRepository.existsByRegistrationNumber(request.getRegistrationNumber())) {
            throw new DuplicateResourceException("Registration number already exists: " + request.getRegistrationNumber());
        }
        if (subcontractorRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("Email already exists: " + request.getEmail());
        }
        if (subcontractorRepository.existsByTin(request.getTin())) {
            throw new DuplicateResourceException("TIN already exists: " + request.getTin());
        }

        OrganisationEntity organisation = organisationRepository.findById(organisationId)
                .orElseThrow(() -> new ItemNotFoundException("Organisation not found with ID: " + organisationId));

        if (subcontractorRepository.existsByCompanyNameAndOrganisationOrganisationId(request.getCompanyName(), organisationId)) {
            throw new DuplicateResourceException("Company name already exists for this organisation: " + request.getCompanyName());
        }

        SubcontractorEntity subcontractor = new SubcontractorEntity();
        subcontractor.setCompanyName(request.getCompanyName());
        subcontractor.setEmail(request.getEmail());
        subcontractor.setPhoneNumber(request.getPhoneNumber());
        subcontractor.setTin(request.getTin());
        subcontractor.setAddress(request.getAddress());
        subcontractor.setRegistrationNumber(request.getRegistrationNumber());
        subcontractor.setSpecializations(request.getSpecializations());
        subcontractor.setOrganisation(organisation);

        if (request.getProjectIds() != null && !request.getProjectIds().isEmpty()) {
            log.info("Assigning projects with IDs: {}", request.getProjectIds());
            Set<ProjectSubcontractorEntity> projectSubcontractors = new HashSet<>();
            for (UUID projectId : request.getProjectIds()) {
                ProjectEntity project = projectRepository.findById(projectId)
                        .orElseThrow(() -> new ItemNotFoundException("Project not found with ID: " + projectId));
                ProjectSubcontractorEntity projectSubcontractor = new ProjectSubcontractorEntity();
                projectSubcontractor.setProject(project);
                projectSubcontractor.setSubcontractor(subcontractor);
                projectSubcontractors.add(projectSubcontractor);
                project.getProjectSubcontractors().add(projectSubcontractor);
            }
            subcontractor.setProjectSubcontractors(projectSubcontractors);
            projectRepository.saveAll(projectSubcontractors.stream().map(ProjectSubcontractorEntity::getProject).collect(Collectors.toList()));
        }

        SubcontractorEntity savedSubcontractor = subcontractorRepository.save(subcontractor);
        log.info("Subcontractor created with ID: {} and projects: {}",
                savedSubcontractor.getSubcontractorId(),
                savedSubcontractor.getProjectSubcontractors());

        Hibernate.initialize(savedSubcontractor.getProjectSubcontractors());
        return mapToSubcontractorResponse(savedSubcontractor);
    }

    @Override
    @Transactional(readOnly = true)
    public SubcontractorResponse getSubcontractorById(UUID subcontractorId) throws ItemNotFoundException {
        log.info("Fetching subcontractor with ID: {}", subcontractorId);

        SubcontractorEntity subcontractor = subcontractorRepository.findById(subcontractorId)
                .orElseThrow(() -> new ItemNotFoundException("Subcontractor not found with ID: " + subcontractorId));

        Hibernate.initialize(subcontractor.getProjectSubcontractors());

        return mapToSubcontractorResponse(subcontractor);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SubcontractorListResponse> getAllSubcontractors() {
        log.info("Fetching all subcontractors");

        List<SubcontractorEntity> subcontractors = subcontractorRepository.findAll();
        subcontractors.forEach(subcontractor -> Hibernate.initialize(subcontractor.getProjectSubcontractors()));
        return subcontractors.stream()
                .map(this::mapToSubcontractorListResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<SubcontractorListResponse> getSubcontractorsByOrganisation(UUID organisationId) {
        log.info("Fetching subcontractors for organisation with ID: {}", organisationId);

        List<SubcontractorEntity> subcontractors = subcontractorRepository.findByOrganisationOrganisationId(organisationId);
        subcontractors.forEach(subcontractor -> Hibernate.initialize(subcontractor.getProjectSubcontractors()));
        return subcontractors.stream()
                .map(this::mapToSubcontractorListResponse)
                .collect(Collectors.toList());
    }

    @Override
    public SubcontractorResponse updateSubcontractor(UUID subcontractorId, SubcontractorUpdateRequest request) throws ItemNotFoundException {
        log.info("Updating subcontractor with ID: {}", subcontractorId);

        SubcontractorEntity subcontractor = subcontractorRepository.findById(subcontractorId)
                .orElseThrow(() -> new ItemNotFoundException("Subcontractor not found with ID: " + subcontractorId));

        if (request.getRegistrationNumber() != null &&
                subcontractorRepository.existsByRegistrationNumberAndSubcontractorIdNot(request.getRegistrationNumber(), subcontractorId)) {
            throw new DuplicateResourceException("Registration number already exists: " + request.getRegistrationNumber());
        }
        if (request.getEmail() != null &&
                subcontractorRepository.existsByEmailAndSubcontractorIdNot(request.getEmail(), subcontractorId)) {
            throw new DuplicateResourceException("Email already exists: " + request.getEmail());
        }
        if (request.getTin() != null &&
                subcontractorRepository.existsByTinAndSubcontractorIdNot(request.getTin(), subcontractorId)) {
            throw new DuplicateResourceException("TIN already exists: " + request.getTin());
        }

        if (request.getCompanyName() != null) {
            subcontractor.setCompanyName(request.getCompanyName());
        }
        if (request.getEmail() != null) {
            subcontractor.setEmail(request.getEmail());
        }
        if (request.getPhoneNumber() != null) {
            subcontractor.setPhoneNumber(request.getPhoneNumber());
        }
        if (request.getTin() != null) {
            subcontractor.setTin(request.getTin());
        }
        if (request.getAddress() != null) {
            subcontractor.setAddress(request.getAddress());
        }
        if (request.getRegistrationNumber() != null) {
            subcontractor.setRegistrationNumber(request.getRegistrationNumber());
        }
        if (request.getSpecializations() != null) {
            subcontractor.setSpecializations(request.getSpecializations());
        }

        if (request.getProjectIds() != null) {
            Set<ProjectSubcontractorEntity> projectSubcontractors = new HashSet<>();
            for (UUID projectId : request.getProjectIds()) {
                ProjectEntity project = projectRepository.findById(projectId)
                        .orElseThrow(() -> new ItemNotFoundException("Project not found with ID: " + projectId));
                ProjectSubcontractorEntity projectSubcontractor = new ProjectSubcontractorEntity();
                projectSubcontractor.setProject(project);
                projectSubcontractor.setSubcontractor(subcontractor);
                projectSubcontractors.add(projectSubcontractor);
                project.getProjectSubcontractors().add(projectSubcontractor);
            }
            subcontractor.setProjectSubcontractors(projectSubcontractors);
            projectRepository.saveAll(projectSubcontractors.stream().map(ProjectSubcontractorEntity::getProject).collect(Collectors.toList()));
        }

        SubcontractorEntity updatedSubcontractor = subcontractorRepository.save(subcontractor);
        log.info("Subcontractor updated with ID: {} and projects: {}",
                updatedSubcontractor.getSubcontractorId(),
                updatedSubcontractor.getProjectSubcontractors());

        Hibernate.initialize(updatedSubcontractor.getProjectSubcontractors());
        return mapToSubcontractorResponse(updatedSubcontractor);
    }

    @Override
    public void deleteSubcontractor(UUID subcontractorId) throws ItemNotFoundException {
        log.info("Deleting subcontractor with ID: {}", subcontractorId);

        SubcontractorEntity subcontractor = subcontractorRepository.findById(subcontractorId)
                .orElseThrow(() -> new ItemNotFoundException("Subcontractor not found with ID: " + subcontractorId));

        Set<ProjectSubcontractorEntity> projectSubcontractors = subcontractor.getProjectSubcontractors();
        if (projectSubcontractors != null) {
            projectSubcontractors.forEach(ps -> ps.getProject().getProjectSubcontractors().remove(ps));
            projectRepository.saveAll(projectSubcontractors.stream().map(ProjectSubcontractorEntity::getProject).collect(Collectors.toList()));
        }

        subcontractorRepository.delete(subcontractor);
        log.info("Subcontractor deleted successfully");
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProjectResponseForSubcontractor> getSubcontractorProjects(UUID subcontractorId) throws ItemNotFoundException {
        log.info("Fetching projects for subcontractor ID: {}", subcontractorId);

        SubcontractorEntity subcontractor = subcontractorRepository.findById(subcontractorId)
                .orElseThrow(() -> new ItemNotFoundException("Subcontractor not found with ID: " + subcontractorId));

        Hibernate.initialize(subcontractor.getProjectSubcontractors());

        return subcontractor.getProjectSubcontractors().stream()
                .map(ps -> mapToProjectResponseForSubcontractor(ps.getProject()))
                .collect(Collectors.toList());
    }

    @Override
    public SubcontractorResponse assignProjectToSubcontractor(UUID subcontractorId, UUID projectId) throws ItemNotFoundException {
        log.info("Assigning project {} to subcontractor {}", projectId, subcontractorId);

        SubcontractorEntity subcontractor = subcontractorRepository.findById(subcontractorId)
                .orElseThrow(() -> new ItemNotFoundException("Subcontractor not found with ID: " + subcontractorId));

        ProjectEntity project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ItemNotFoundException("Project not found with ID: " + projectId));

        if (subcontractor.getProjectSubcontractors() == null) {
            subcontractor.setProjectSubcontractors(new HashSet<>());
        }
        if (project.getProjectSubcontractors() == null) {
            project.setProjectSubcontractors(new HashSet<>());
        }

        boolean alreadyAssigned = subcontractor.getProjectSubcontractors().stream()
                .anyMatch(ps -> ps.getProject().getProjectId().equals(projectId));

        if (!alreadyAssigned) {
            ProjectSubcontractorEntity projectSubcontractor = new ProjectSubcontractorEntity();
            projectSubcontractor.setProject(project);
            projectSubcontractor.setSubcontractor(subcontractor);
            project.getProjectSubcontractors().add(projectSubcontractor);
            subcontractor.getProjectSubcontractors().add(projectSubcontractor);
            projectSubcontractorRepository.save(projectSubcontractor);
        } else {
            log.info("Project {} already assigned to subcontractor {}", projectId, subcontractorId);
        }

        Hibernate.initialize(subcontractor.getProjectSubcontractors());
        return mapToSubcontractorResponse(subcontractor);
    }

    @Override
    public List<SubcontractorResponse> assignSubcontractorsToProject(UUID projectId, List<UUID> subcontractorIds) throws ItemNotFoundException {
        log.info("Assigning subcontractors {} to project {}", subcontractorIds, projectId);

        ProjectEntity project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ItemNotFoundException("Project not found with ID: " + projectId));

        if (project.getProjectSubcontractors() == null) {
            project.setProjectSubcontractors(new HashSet<>());
        }

        List<SubcontractorResponse> responses = new ArrayList<>();
        List<ProjectSubcontractorEntity> projectSubcontractorsToSave = new ArrayList<>();

        for (UUID subcontractorId : subcontractorIds) {
            SubcontractorEntity subcontractor = subcontractorRepository.findById(subcontractorId)
                    .orElseThrow(() -> new ItemNotFoundException("Subcontractor not found with ID: " + subcontractorId));

            if (subcontractor.getProjectSubcontractors() == null) {
                subcontractor.setProjectSubcontractors(new HashSet<>());
            }

            boolean alreadyAssigned = project.getProjectSubcontractors().stream()
                    .anyMatch(ps -> ps.getSubcontractor().getSubcontractorId().equals(subcontractorId));

            if (!alreadyAssigned) {
                ProjectSubcontractorEntity projectSubcontractor = new ProjectSubcontractorEntity();
                projectSubcontractor.setProject(project);
                projectSubcontractor.setSubcontractor(subcontractor);
                project.getProjectSubcontractors().add(projectSubcontractor);
                subcontractor.getProjectSubcontractors().add(projectSubcontractor);
                projectSubcontractorsToSave.add(projectSubcontractor);
                log.info("Subcontractor {} assigned to project {}", subcontractorId, projectId);
            } else {
                log.info("Subcontractor {} already assigned to project {}", subcontractorId, projectId);
            }
        }

        if (!projectSubcontractorsToSave.isEmpty()) {
            projectSubcontractorRepository.saveAll(projectSubcontractorsToSave);
            projectRepository.save(project);
            subcontractorRepository.saveAll(projectSubcontractorsToSave.stream()
                    .map(ProjectSubcontractorEntity::getSubcontractor)
                    .collect(Collectors.toList()));
        }

        for (UUID subcontractorId : subcontractorIds) {
            SubcontractorEntity subcontractor = subcontractorRepository.findById(subcontractorId)
                    .orElseThrow(() -> new ItemNotFoundException("Subcontractor not found with ID: " + subcontractorId));

            Hibernate.initialize(subcontractor.getProjectSubcontractors());
            responses.add(mapToSubcontractorResponse(subcontractor));
        }

        return responses;
    }

    @Override
    public SubcontractorResponse removeProjectFromSubcontractor(UUID subcontractorId, UUID projectId) throws ItemNotFoundException {
        log.info("Removing project {} from subcontractor {}", projectId, subcontractorId);

        SubcontractorEntity subcontractor = subcontractorRepository.findById(subcontractorId)
                .orElseThrow(() -> new ItemNotFoundException("Subcontractor not found with ID: " + subcontractorId));

        ProjectEntity project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ItemNotFoundException("Project not found with ID: " + projectId));

        ProjectSubcontractorEntity projectSubcontractor = project.getProjectSubcontractors().stream()
                .filter(ps -> ps.getSubcontractor().getSubcontractorId().equals(subcontractorId))
                .findFirst()
                .orElse(null);

        if (projectSubcontractor != null) {
            project.getProjectSubcontractors().remove(projectSubcontractor);
            subcontractor.getProjectSubcontractors().remove(projectSubcontractor);
            projectSubcontractorRepository.delete(projectSubcontractor);
            projectRepository.save(project);
            subcontractorRepository.save(subcontractor);
            log.info("Project {} removed from subcontractor {}", projectId, subcontractorId);
        } else {
            log.info("Project {} was not assigned to subcontractor {}", projectId, subcontractorId);
        }

        Hibernate.initialize(subcontractor.getProjectSubcontractors());
        return mapToSubcontractorResponse(subcontractor);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SubcontractorListResponse> getSubcontractorsBySpecializations(List<String> specializations) {
        log.info("Fetching subcontractors by specializations: {}", specializations);

        List<SubcontractorEntity> subcontractors = subcontractorRepository.findBySpecializationsIn(specializations);
        subcontractors.forEach(subcontractor -> Hibernate.initialize(subcontractor.getProjectSubcontractors()));
        return subcontractors.stream()
                .map(this::mapToSubcontractorListResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isRegistrationNumberExists(String registrationNumber) {
        return subcontractorRepository.existsByRegistrationNumber(registrationNumber);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isEmailExists(String email) {
        return subcontractorRepository.existsByEmail(email);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isTinExists(String tin) {
        return subcontractorRepository.existsByTin(tin);
    }

    private SubcontractorResponse mapToSubcontractorResponse(SubcontractorEntity entity) {
        List<ProjectResponseForSubcontractor> projectList = null;
        if (entity.getProjectSubcontractors() != null) {
            projectList = entity.getProjectSubcontractors().stream()
                    .map(ps -> mapToProjectResponseForSubcontractor(ps.getProject()))
                    .collect(Collectors.toList());
        }

        return SubcontractorResponse.builder()
                .subcontractorId(entity.getSubcontractorId())
                .companyName(entity.getCompanyName())
                .email(entity.getEmail())
                .phoneNumber(entity.getPhoneNumber())
                .tin(entity.getTin())
                .address(entity.getAddress())
                .registrationNumber(entity.getRegistrationNumber())
                .specializations(entity.getSpecializations())
                .organisationName(entity.getOrganisationName())
                .organisationId(entity.getOrganisationId())
                .projects(projectList)
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .projectsCount(entity.getProjectsCount())
                .specializationsCount(entity.getSpecializationsCount())
                .build();
    }

    private SubcontractorListResponse mapToSubcontractorListResponse(SubcontractorEntity entity) {
        return SubcontractorListResponse.builder()
                .subcontractorId(entity.getSubcontractorId())
                .companyName(entity.getCompanyName())
                .email(entity.getEmail())
                .phoneNumber(entity.getPhoneNumber())
                .specializations(entity.getSpecializations())
                .organisationName(entity.getOrganisationName())
                .organisationId(entity.getOrganisationId())
                .projectsCount(entity.getProjectsCount())
                .specializationsCount(entity.getSpecializationsCount())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    private ProjectResponseForSubcontractor mapToProjectResponseForSubcontractor(ProjectEntity project) {
        ProjectResponseForSubcontractor response = new ProjectResponseForSubcontractor();
        response.setProjectId(project.getProjectId());
        response.setName(project.getName());
        response.setDescription(project.getDescription());
        response.setBudget(project.getBudget());
        response.setOrganisationId(project.getOrganisation() != null ? project.getOrganisation().getOrganisationId() : null);
        response.setOrganisationName(project.getOrganisation() != null ? project.getOrganisation().getOrganisationName() : null);
        response.setStatus(project.getStatus() != null ? project.getStatus().toString() : null);
        response.setContractNumber(project.getContractNumber());
        response.setCreatedAt(project.getCreatedAt());
        response.setUpdatedAt(project.getUpdatedAt());
        return response;
    }
}