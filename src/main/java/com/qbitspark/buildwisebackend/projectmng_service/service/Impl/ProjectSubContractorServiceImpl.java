package com.qbitspark.buildwisebackend.projectmng_service.service.Impl;

import com.qbitspark.buildwisebackend.authentication_service.entity.AccountEntity;
import com.qbitspark.buildwisebackend.authentication_service.Repository.AccountRepo;
import com.qbitspark.buildwisebackend.globeadvice.exceptions.ItemNotFoundException;
import com.qbitspark.buildwisebackend.organisation_service.orgnisation_members_mng.entities.OrganisationMember;
import com.qbitspark.buildwisebackend.organisation_service.orgnisation_members_mng.enums.MemberRole;
import com.qbitspark.buildwisebackend.organisation_service.orgnisation_members_mng.enums.MemberStatus;
import com.qbitspark.buildwisebackend.organisation_service.orgnisation_members_mng.repo.OrganisationMemberRepo;
import com.qbitspark.buildwisebackend.projectmng_service.entity.ProjectEntity;
import com.qbitspark.buildwisebackend.projectmng_service.entity.ProjectSubcontractorEntity;
import com.qbitspark.buildwisebackend.projectmng_service.payloads.BulkAddSubContractorRequest;
import com.qbitspark.buildwisebackend.projectmng_service.payloads.ProjectSubContractorRemovalResponse;
import com.qbitspark.buildwisebackend.projectmng_service.payloads.ProjectSubContractorResponse;
import com.qbitspark.buildwisebackend.projectmng_service.repo.ProjectRepo;
import com.qbitspark.buildwisebackend.projectmng_service.repo.ProjectSubContractorRepo;
import com.qbitspark.buildwisebackend.projectmng_service.service.ProjectSubContractorService;
import com.qbitspark.buildwisebackend.projectmng_service.utils.AsyncEmailService;
import com.qbitspark.buildwisebackend.subcontractor_service.entity.SubcontractorEntity;
import com.qbitspark.buildwisebackend.subcontractor_service.repo.SubcontractorRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProjectSubContractorServiceImpl implements ProjectSubContractorService {

    private final ProjectRepo projectRepository;
    private final SubcontractorRepo subcontractorRepository;
    private final OrganisationMemberRepo organisationMemberRepository;
    private final AccountRepo accountRepository;
    private final AsyncEmailService asyncEmailService;
    private final ProjectSubContractorRepo projectSubcontractorRepository; // Add repository

    @Override
    @Transactional
    public List<ProjectSubContractorResponse> addSubContractors(UUID projectId, Set<BulkAddSubContractorRequest> requests) throws ItemNotFoundException {
        validateRequesterPermissions(projectId);
        return addSubContractorsInternal(projectId, requests, true);
    }

    @Override
    @Transactional
    public ProjectSubContractorRemovalResponse removeSubContractors(UUID projectId, Set<UUID> subcontractorIds) throws ItemNotFoundException {
        validateRequesterPermissions(projectId);

        Object projectObj = projectRepository.findByIdWithSubcontractors(projectId)
                .orElseThrow(() -> new ItemNotFoundException("Project not found"));
        ProjectEntity project = (ProjectEntity) projectObj;

        List<ProjectSubcontractorEntity> subcontractorsToRemove = new ArrayList<>();
        List<UUID> skippedSubcontractors = new ArrayList<>();

        for (UUID subcontractorId : subcontractorIds) {
            Optional<ProjectSubcontractorEntity> projectSubcontractorOpt = project.getProjectSubcontractors().stream()
                    .filter(ps -> ps.getSubcontractor().getSubcontractorId().equals(subcontractorId))
                    .findFirst();

            if (projectSubcontractorOpt.isPresent()) {
                subcontractorsToRemove.add(projectSubcontractorOpt.get());
            } else {
                skippedSubcontractors.add(subcontractorId);
            }
        }

        List<ProjectSubContractorResponse> removedSubcontractors = new ArrayList<>();
        for (ProjectSubcontractorEntity projectSubcontractor : subcontractorsToRemove) {
            try {
                SubcontractorEntity subcontractor = projectSubcontractor.getSubcontractor();
                removedSubcontractors.add(mapToResponse(subcontractor, project));
                project.getProjectSubcontractors().remove(projectSubcontractor);
                subcontractor.getProjectSubcontractors().remove(projectSubcontractor);
                projectSubcontractorRepository.delete(projectSubcontractor);
            } catch (Exception e) {
                log.error("Failed to remove subcontractor: {}", e.getMessage());
            }
        }

        projectRepository.save(project);
        subcontractorRepository.saveAll(subcontractorsToRemove.stream()
                .map(ProjectSubcontractorEntity::getSubcontractor)
                .collect(Collectors.toList()));

        ProjectSubContractorRemovalResponse response = new ProjectSubContractorRemovalResponse();
        response.setRemovedCount(removedSubcontractors.size());
        response.setNotFoundSubcontractorIds(new HashSet<>(skippedSubcontractors));
        response.setNotAssignedSubcontractorIds(new HashSet<>());
        return response;
    }

    @Override
    public List<ProjectSubContractorResponse> getProjectSubContractors(UUID projectId) throws ItemNotFoundException {
        validateRequesterAccess(projectId);

        Object projectObj = projectRepository.findByIdWithSubcontractors(projectId)
                .orElseThrow(() -> new ItemNotFoundException("Project not found"));
        ProjectEntity project = (ProjectEntity) projectObj;

        return project.getProjectSubcontractors().stream()
                .map(ps -> mapToResponse(ps.getSubcontractor(), project))
                .collect(Collectors.toList());
    }

    @Override
    public boolean isSubContractorAssigned(UUID projectId, UUID subcontractorId) throws ItemNotFoundException {
        ProjectEntity project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ItemNotFoundException("Project not found"));
        SubcontractorEntity subcontractor = subcontractorRepository.findById(subcontractorId)
                .orElseThrow(() -> new ItemNotFoundException("Subcontractor with ID " + subcontractorId + " not found"));

        return project.isSubcontractorAssigned(subcontractor);
    }

    private SubcontractorEntity validateSubcontractor(UUID subcontractorId, ProjectEntity project) throws ItemNotFoundException {
        SubcontractorEntity subcontractor = subcontractorRepository.findById(subcontractorId)
                .orElseThrow(() -> new ItemNotFoundException("Subcontractor not found"));

        if (!subcontractor.getOrganisation().getOrganisationId().equals(project.getOrganisation().getOrganisationId())) {
            throw new ItemNotFoundException("Subcontractor does not belong to this project's organization");
        }

        return subcontractor;
    }

    private void validateRequesterPermissions(UUID projectId) throws ItemNotFoundException {
        AccountEntity currentUser = getAuthenticatedAccount();
        ProjectEntity project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ItemNotFoundException("Project not found"));

        OrganisationMember requester = organisationMemberRepository
                .findByAccountAndOrganisation(currentUser, project.getOrganisation())
                .orElseThrow(() -> new ItemNotFoundException("You are not a member of this project's organization"));

        if (requester.getStatus() != MemberStatus.ACTIVE) {
            throw new ItemNotFoundException("Your membership is not active");
        }

        if (requester.getRole() != MemberRole.OWNER && requester.getRole() != MemberRole.ADMIN) {
            throw new ItemNotFoundException("You don't have permission to manage this project's subcontractors");
        }
    }

    private void validateRequesterAccess(UUID projectId) throws ItemNotFoundException {
        AccountEntity currentUser = getAuthenticatedAccount();
        ProjectEntity project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ItemNotFoundException("Project not found"));

        OrganisationMember requester = organisationMemberRepository
                .findByAccountAndOrganisation(currentUser, project.getOrganisation())
                .orElseThrow(() -> new ItemNotFoundException("You are not a member of this project's organization"));

        if (requester.getStatus() != MemberStatus.ACTIVE) {
            throw new ItemNotFoundException("Your membership is not active");
        }
    }

    private ProjectSubContractorResponse mapToResponse(SubcontractorEntity subcontractor, ProjectEntity project) {
        return new ProjectSubContractorResponse(
                UUID.randomUUID(),
                project.getProjectId(),
                subcontractor.getSubcontractorId(),
                subcontractor.getCompanyName(),
                LocalDateTime.now()
        );
    }

    private AccountEntity getAuthenticatedAccount() throws ItemNotFoundException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            String userName = userDetails.getUsername();
            return accountRepository.findByUserName(userName)
                    .orElseThrow(() -> new ItemNotFoundException("User not found"));
        }
        throw new ItemNotFoundException("User not authenticated");
    }

    private List<ProjectSubContractorResponse> addSubContractorsInternal(UUID projectId, Set<BulkAddSubContractorRequest> requests, Boolean sendEmail) throws ItemNotFoundException {
        Object projectObj = projectRepository.findByIdWithSubcontractors(projectId)
                .orElseThrow(() -> new ItemNotFoundException("Project not found"));
        ProjectEntity project = (ProjectEntity) projectObj;

        Set<UUID> currentSubcontractorIds = project.getProjectSubcontractors().stream()
                .map(ps -> ps.getSubcontractor().getSubcontractorId())
                .collect(Collectors.toSet());

        List<ProjectSubcontractorEntity> newAssignments = new ArrayList<>();

        for (BulkAddSubContractorRequest request : requests) {
            UUID subcontractorId = request.getSubcontractorId();

            if (currentSubcontractorIds.contains(subcontractorId)) {
                log.info("Subcontractor {} already assigned to project {}", subcontractorId, projectId);
                continue;
            }

            try {
                SubcontractorEntity subcontractor = validateSubcontractor(subcontractorId, project);
                ProjectSubcontractorEntity projectSubcontractor = new ProjectSubcontractorEntity();
                projectSubcontractor.setProject(project);
                projectSubcontractor.setSubcontractor(subcontractor);
                project.getProjectSubcontractors().add(projectSubcontractor);
                subcontractor.getProjectSubcontractors().add(projectSubcontractor);
                newAssignments.add(projectSubcontractor);
                currentSubcontractorIds.add(subcontractorId);
            } catch (ItemNotFoundException e) {
                log.warn("Skipping invalid subcontractor {}: {}", subcontractorId, e.getMessage());
            }
        }

        if (!newAssignments.isEmpty()) {
            projectSubcontractorRepository.saveAll(newAssignments);
            projectRepository.save(project);
            subcontractorRepository.saveAll(newAssignments.stream()
                    .map(ProjectSubcontractorEntity::getSubcontractor)
                    .collect(Collectors.toList()));

            if (sendEmail) {
                for (ProjectSubcontractorEntity projectSubcontractor : newAssignments) {
                    SubcontractorEntity subcontractor = projectSubcontractor.getSubcontractor();
                    asyncEmailService.sendSubcontractorAssignmentEmailAsync(
                            subcontractor.getEmail(),
                            subcontractor.getCompanyName(),
                            project.getName(),
                            project.getOrganisation().getOrganisationId(),
                            project.getProjectId()
                    );
                }
            }
        }

        return newAssignments.stream()
                .map(ps -> mapToResponse(ps.getSubcontractor(), project))
                .collect(Collectors.toList());
    }
}