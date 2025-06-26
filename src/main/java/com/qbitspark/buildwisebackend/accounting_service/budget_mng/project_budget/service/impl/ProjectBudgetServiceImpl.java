package com.qbitspark.buildwisebackend.accounting_service.budget_mng.project_budget.service.impl;

import com.qbitspark.buildwisebackend.accounting_service.budget_mng.org_budget.entity.OrgBudgetEntity;
import com.qbitspark.buildwisebackend.accounting_service.budget_mng.org_budget.enums.BudgetStatus;
import com.qbitspark.buildwisebackend.accounting_service.budget_mng.org_budget.repo.OrgBudgetRepo;
import com.qbitspark.buildwisebackend.accounting_service.budget_mng.project_budget.entity.ProjectBudgetEntity;
import com.qbitspark.buildwisebackend.accounting_service.budget_mng.project_budget.enums.ProjectBudgetStatus;
import com.qbitspark.buildwisebackend.accounting_service.budget_mng.project_budget.payload.DistributeBudgetRequest;
import com.qbitspark.buildwisebackend.accounting_service.budget_mng.project_budget.repo.ProjectBudgetRepo;
import com.qbitspark.buildwisebackend.accounting_service.budget_mng.project_budget.service.ProjectBudgetService;
import com.qbitspark.buildwisebackend.accounting_service.coa.entity.ChartOfAccounts;
import com.qbitspark.buildwisebackend.accounting_service.coa.repo.ChartOfAccountsRepo;
import com.qbitspark.buildwisebackend.authentication_service.Repository.AccountRepo;
import com.qbitspark.buildwisebackend.authentication_service.entity.AccountEntity;
import com.qbitspark.buildwisebackend.globeadvice.exceptions.AccessDeniedException;
import com.qbitspark.buildwisebackend.globeadvice.exceptions.ItemNotFoundException;
import com.qbitspark.buildwisebackend.organisation_service.organisation_mng.entity.OrganisationEntity;
import com.qbitspark.buildwisebackend.organisation_service.orgnisation_members_mng.entities.OrganisationMember;
import com.qbitspark.buildwisebackend.organisation_service.orgnisation_members_mng.enums.MemberRole;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProjectBudgetServiceImpl implements ProjectBudgetService {

    private final OrgBudgetRepo orgBudgetRepo;
    private final ProjectRepo projectRepo;
    private final ChartOfAccountsRepo chartOfAccountRepo;
    private final ProjectBudgetRepo projectBudgetRepo;
    private final AccountRepo accountRepo;
    private final OrganisationMemberRepo organisationMemberRepo;
    private final ProjectTeamMemberRepo projectTeamMemberRepo;

    @Override
    public List<ProjectBudgetEntity> distributeBudgetToProject(DistributeBudgetRequest request, UUID orgBudgetId, UUID projectId) throws ItemNotFoundException, AccessDeniedException {

        AccountEntity authenticatedAccount = getAuthenticatedAccount();

        OrgBudgetEntity orgBudget = orgBudgetRepo.findById(orgBudgetId)
                .orElseThrow(() -> new ItemNotFoundException("Organization budget not found"));


        ProjectEntity project = projectRepo.findById(projectId)
                .orElseThrow(() -> new ItemNotFoundException("Project not found"));


        validateProject(project, orgBudget.getOrganisation());

        validateProjectMemberPermissions(authenticatedAccount, project,
                List.of(TeamMemberRole.ACCOUNTANT, TeamMemberRole.OWNER, TeamMemberRole.PROJECT_MANAGER));


        if (orgBudget.getStatus() != BudgetStatus.ACTIVE) {
            throw new ItemNotFoundException("Only active organization budgets can distribute money");
        }


        BigDecimal totalAmount = request.getAccountDistributions().stream()
                .map(DistributeBudgetRequest.AccountDistribution::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Step 7: Check if org budget has enough money
        if (orgBudget.getAvailableAmount().compareTo(totalAmount) < 0) {
            throw new ItemNotFoundException("Insufficient budget available. Available: " +
                    orgBudget.getAvailableAmount() + ", Requested: " + totalAmount);
        }

        List<ProjectBudgetEntity> createdBudgets = new ArrayList<>();

        for (DistributeBudgetRequest.AccountDistribution distribution : request.getAccountDistributions()) {

            ChartOfAccounts account = chartOfAccountRepo.findById(distribution.getAccountId())
                    .orElseThrow(() -> new ItemNotFoundException("Chart of account not found: " + distribution.getAccountId()));

            ProjectBudgetEntity projectBudget = new ProjectBudgetEntity();
            projectBudget.setOrgBudget(orgBudget);
            projectBudget.setProject(project);
            projectBudget.setChartOfAccount(account);
            projectBudget.setBudgetAmount(distribution.getAmount());
            projectBudget.setSpentAmount(BigDecimal.ZERO);
            projectBudget.setCommittedAmount(BigDecimal.ZERO);
            projectBudget.setStatus(ProjectBudgetStatus.DRAFT);
            projectBudget.setBudgetNotes(distribution.getDescription());
            projectBudget.setCreatedBy(authenticatedAccount.getAccountId());
            projectBudget.setCreatedDate(LocalDateTime.now());

            ProjectBudgetEntity saved = projectBudgetRepo.save(projectBudget);
            createdBudgets.add(saved);
        }


        orgBudget.setAllocatedAmount(orgBudget.getAllocatedAmount().add(totalAmount));
        orgBudgetRepo.save(orgBudget);

        return createdBudgets;
    }

    private void validateMemberPermissions(AccountEntity account, OrganisationEntity organisation, List<MemberRole> allowedRoles) throws ItemNotFoundException {

        OrganisationMember member = organisationMemberRepo.findByAccountAndOrganisation(account, organisation)
                .orElseThrow(() -> new ItemNotFoundException("Member is not found in organisation"));

        if (member.getStatus() != MemberStatus.ACTIVE) {
            throw new ItemNotFoundException("Member is not active");
        }

        if (!allowedRoles.contains(member.getRole())) {
            throw new ItemNotFoundException("Member has insufficient permissions");
        }

    }

    private AccountEntity getAuthenticatedAccount() throws ItemNotFoundException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return extractAccount(authentication);
    }

    private AccountEntity extractAccount(Authentication authentication) throws ItemNotFoundException {
        if (authentication != null && authentication.isAuthenticated()) {
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            String userName = userDetails.getUsername();
            return accountRepo.findByUserName(userName)
                    .orElseThrow(() -> new ItemNotFoundException("User given username does not exist"));
        }
        throw new ItemNotFoundException("User is not authenticated");
    }

    private void validateProjectMemberPermissions(AccountEntity account, ProjectEntity project, List<TeamMemberRole> allowedRoles) throws ItemNotFoundException, AccessDeniedException {

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

    }

    private void validateProject(ProjectEntity project, OrganisationEntity organisation) throws ItemNotFoundException {
        if (!project.getOrganisation().getOrganisationId().equals(organisation.getOrganisationId())) {
            throw new ItemNotFoundException("Project does not belong to this organisation");
        }
    }

}
