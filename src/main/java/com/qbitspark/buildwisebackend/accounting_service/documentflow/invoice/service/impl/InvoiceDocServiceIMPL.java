package com.qbitspark.buildwisebackend.accounting_service.documentflow.invoice.service.impl;

import com.qbitspark.buildwisebackend.accounting_service.documentflow.invoice.entity.InvoiceDocEntity;
import com.qbitspark.buildwisebackend.accounting_service.documentflow.invoice.paylaod.CreateInvoiceDocRequest;
import com.qbitspark.buildwisebackend.accounting_service.documentflow.invoice.paylaod.InvoiceDocResponse;
import com.qbitspark.buildwisebackend.accounting_service.documentflow.invoice.repo.InvoiceDocRepo;
import com.qbitspark.buildwisebackend.accounting_service.documentflow.invoice.service.InvoiceDocService;
import com.qbitspark.buildwisebackend.authentication_service.Repository.AccountRepo;
import com.qbitspark.buildwisebackend.authentication_service.entity.AccountEntity;
import com.qbitspark.buildwisebackend.clientsmng_service.entity.ClientEntity;
import com.qbitspark.buildwisebackend.globeadvice.exceptions.AccessDeniedException;
import com.qbitspark.buildwisebackend.globeadvice.exceptions.ItemNotFoundException;
import com.qbitspark.buildwisebackend.organisation_service.organisation_mng.entity.OrganisationEntity;
import com.qbitspark.buildwisebackend.organisation_service.organisation_mng.repo.OrganisationRepo;
import com.qbitspark.buildwisebackend.organisation_service.orgnisation_members_mng.entities.OrganisationMember;
import com.qbitspark.buildwisebackend.organisation_service.orgnisation_members_mng.enums.MemberStatus;
import com.qbitspark.buildwisebackend.organisation_service.orgnisation_members_mng.repo.OrganisationMemberRepo;
import com.qbitspark.buildwisebackend.projectmng_service.entity.ProjectEntity;
import com.qbitspark.buildwisebackend.projectmng_service.entity.ProjectTeamMemberEntity;
import com.qbitspark.buildwisebackend.projectmng_service.enums.TeamMemberRole;
import com.qbitspark.buildwisebackend.projectmng_service.repo.ProjectRepo;
import com.qbitspark.buildwisebackend.projectmng_service.repo.ProjectTeamMemberRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class InvoiceDocServiceIMPL implements InvoiceDocService {

    private final InvoiceDocRepo invoiceDocRepo;
    private final ProjectRepo projectRepo;
    private final OrganisationRepo organisationRepo;
    private final AccountRepo accountRepo;
    private final OrganisationMemberRepo organisationMemberRepo;
    private final ProjectTeamMemberRepo projectTeamMemberRepo;

    @Override
    public InvoiceDocResponse createInvoiceWithAttachments(UUID organisationId, CreateInvoiceDocRequest request, List<MultipartFile> attachments) throws ItemNotFoundException, AccessDeniedException {

        AccountEntity currentUser = getAuthenticatedAccount();

        ProjectEntity project = projectRepo.findById(request.getProjectId())
                .orElseThrow(() -> new ItemNotFoundException("Project not found"));

        OrganisationEntity organisation = organisationRepo.findById(organisationId)
                .orElseThrow(() -> new ItemNotFoundException("Organisation not found"));

        validateProject(project, organisation);

        validateProjectMemberPermissions(currentUser, project,
                List.of(TeamMemberRole.ACCOUNTANT, TeamMemberRole.OWNER, TeamMemberRole.PROJECT_MANAGER));

        ClientEntity client = project.getClient();
        if (client == null) {
            throw new ItemNotFoundException("Project has no client assigned");
        }

        InvoiceDocEntity invoice = InvoiceDocEntity.builder()
                .project(project)
                .client(client)
                .organisation(organisation)
                .dateOfIssue(request.getDateOfIssue())
                .dueDate(request.getDueDate())
                .reference(request.getReference())
                .createdBy(currentUser.getId())
                .updatedBy(currentUser.getId())
                .build();

        // TODO: Handle attachments later

        return null;
    }

    private ProjectTeamMemberEntity validateProjectMemberPermissions(AccountEntity account, ProjectEntity project, List<TeamMemberRole> allowedRoles) throws ItemNotFoundException, AccessDeniedException {

        OrganisationMember organisationMember = organisationMemberRepo.findByAccountAndOrganisation(account, project.getOrganisation())
                .orElseThrow(() -> new ItemNotFoundException("Member is not found in organisation"));

        if (organisationMember.getStatus() != MemberStatus.ACTIVE) {
            throw new ItemNotFoundException("Member is not active");
        }

        ProjectTeamMemberEntity projectTeamMember = projectTeamMemberRepo.findByOrganisationMemberAndProject(organisationMember, project)
                .orElseThrow(() -> new ItemNotFoundException("Project team member not found"));

        if (!allowedRoles.contains(projectTeamMember.getRole())) {
            throw new AccessDeniedException("Member has insufficient permissions for this operation");
        }

        return projectTeamMember;
    }

    private void validateProject(ProjectEntity project, OrganisationEntity organisation) throws ItemNotFoundException {
        if (!project.getOrganisation().getOrganisationId().equals(organisation.getOrganisationId())) {
            throw new ItemNotFoundException("Project does not belong to this organisation");
        }
    }

    private AccountEntity getAuthenticatedAccount() throws ItemNotFoundException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            String userName = userDetails.getUsername();
            return accountRepo.findByUserName(userName)
                    .orElseThrow(() -> new ItemNotFoundException("User not found"));
        }
        throw new ItemNotFoundException("User not authenticated");
    }
}