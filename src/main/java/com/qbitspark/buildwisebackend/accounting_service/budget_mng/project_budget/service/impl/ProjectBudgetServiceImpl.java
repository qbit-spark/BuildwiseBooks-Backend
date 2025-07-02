package com.qbitspark.buildwisebackend.accounting_service.budget_mng.project_budget.service.impl;

import com.qbitspark.buildwisebackend.accounting_service.budget_mng.org_budget.entity.OrgBudgetEntity;
import com.qbitspark.buildwisebackend.accounting_service.budget_mng.org_budget.enums.OrgBudgetStatus;
import com.qbitspark.buildwisebackend.accounting_service.budget_mng.org_budget.repo.OrgBudgetRepo;
import com.qbitspark.buildwisebackend.accounting_service.budget_mng.project_budget.entity.ProjectBudgetEntity;
import com.qbitspark.buildwisebackend.accounting_service.budget_mng.project_budget.entity.ProjectBudgetLineItemEntity;
import com.qbitspark.buildwisebackend.accounting_service.budget_mng.project_budget.payload.DistributeBudgetRequest;
import com.qbitspark.buildwisebackend.accounting_service.budget_mng.project_budget.enums.ProjectBudgetStatus;
import com.qbitspark.buildwisebackend.accounting_service.budget_mng.project_budget.payload.ProjectBudgetSummaryResponse;
import com.qbitspark.buildwisebackend.accounting_service.budget_mng.project_budget.repo.ProjectBudgetLineItemRepo;
import com.qbitspark.buildwisebackend.accounting_service.budget_mng.project_budget.repo.ProjectBudgetRepo;
import com.qbitspark.buildwisebackend.accounting_service.budget_mng.project_budget.service.ProjectBudgetService;
import com.qbitspark.buildwisebackend.accounting_service.coa.entity.ChartOfAccounts;
import com.qbitspark.buildwisebackend.accounting_service.coa.enums.AccountType;
import com.qbitspark.buildwisebackend.accounting_service.coa.repo.ChartOfAccountsRepo;
import com.qbitspark.buildwisebackend.authentication_service.Repository.AccountRepo;
import com.qbitspark.buildwisebackend.authentication_service.entity.AccountEntity;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class ProjectBudgetServiceImpl implements ProjectBudgetService {

    private final ProjectBudgetRepo projectBudgetRepo;
    private final ProjectBudgetLineItemRepo projectBudgetLineItemRepo;
    private final ChartOfAccountsRepo chartOfAccountRepo;
    private final OrgBudgetRepo orgBudgetRepo;
    private final ProjectRepo projectRepo;
    private final AccountRepo accountRepo;
    private final OrganisationMemberRepo organisationMemberRepo;
    private final ProjectTeamMemberRepo projectTeamMemberRepo;
    private final OrganisationRepo organisationRepo;

    @Override
    public void initialiseProjectBudget(OrgBudgetEntity orgBudget, ProjectEntity project)
            throws ItemNotFoundException {

        validateProject(project, orgBudget.getOrganisation());

        if (orgBudget.getStatus() != OrgBudgetStatus.ACTIVE) {
            throw new ItemNotFoundException("There is no active financial year budget in this organisation");
        }

        if (projectBudgetRepo.existsByProject(project)) {
            throw new ItemNotFoundException("This project already has a budget");
        }

        // Get all EXPENSE accounts that are postable and active
        List<ChartOfAccounts> expenseAccounts = chartOfAccountRepo.findByOrganisationAndAccountTypeAndIsActiveAndIsPostable(
                orgBudget.getOrganisation(), AccountType.EXPENSE, true, true);

        if (expenseAccounts.isEmpty()) {
            throw new ItemNotFoundException("No expense accounts found in this organisation, please configure chats of account to perform this action");
        }


        ProjectBudgetEntity projectBudget = new ProjectBudgetEntity();
        projectBudget.setOrgBudget(orgBudget);
        projectBudget.setProject(project);
        projectBudget.setTotalBudgetAmount(BigDecimal.ZERO);
        projectBudget.setTotalSpentAmount(BigDecimal.ZERO);
        projectBudget.setTotalCommittedAmount(BigDecimal.ZERO);
        projectBudget.setStatus(ProjectBudgetStatus.DRAFT);
        projectBudget.setBudgetNotes("Initialized - awaiting budget distribution");
        projectBudget.setCreatedBy(null);
        projectBudget.setCreatedDate(LocalDateTime.now());

        // Create line items for each expense account with $0
        for (ChartOfAccounts account : expenseAccounts) {
            ProjectBudgetLineItemEntity lineItem = new ProjectBudgetLineItemEntity();
            lineItem.setChartOfAccount(account);
            lineItem.setBudgetAmount(BigDecimal.ZERO);
            lineItem.setSpentAmount(BigDecimal.ZERO);
            lineItem.setCommittedAmount(BigDecimal.ZERO);
            lineItem.setLineItemNotes("Initialized");
            lineItem.setCreatedDate(LocalDateTime.now());


            projectBudget.addLineItem(lineItem);
        }

        projectBudgetRepo.save(projectBudget);
    }

    @Override
    public ProjectBudgetEntity distributeBudgetToProject(DistributeBudgetRequest request, UUID projectBudgetId, UUID organisationId)
            throws ItemNotFoundException, AccessDeniedException {

        AccountEntity authenticatedAccount = getAuthenticatedAccount();

        ProjectBudgetEntity projectBudget = projectBudgetRepo.findById(projectBudgetId)
                .orElseThrow(() -> new ItemNotFoundException("This project does not have a budget. Please initialise budget first."));

        OrganisationEntity organisation = organisationRepo.findById(organisationId).orElseThrow(() -> new ItemNotFoundException("Organisation does not exist"));

        validateProject(projectBudget.getProject(), organisation);

        validateProjectMemberPermissions(authenticatedAccount, projectBudget.getProject(),
                List.of(TeamMemberRole.ACCOUNTANT, TeamMemberRole.OWNER, TeamMemberRole.PROJECT_MANAGER));


        if (projectBudget.getStatus() != ProjectBudgetStatus.DRAFT) {
            throw new ItemNotFoundException("Only draft project budgets can be updated");
        }


        if (projectBudget.getOrgBudget().getStatus() != OrgBudgetStatus.ACTIVE) {
            throw new ItemNotFoundException("Associated organisation budget is not active!");
        }


        BigDecimal totalDistributionAmount = request.getAccountDistributions().stream()
                .map(DistributeBudgetRequest.AccountDistribution::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);


        if (projectBudget.getOrgBudget().getAvailableAmount().compareTo(totalDistributionAmount) < 0) {
            throw new ItemNotFoundException("Insufficient budget available. Available: " +
                    projectBudget.getOrgBudget().getAvailableAmount() + ", Requested: " + totalDistributionAmount);
        }

        for (DistributeBudgetRequest.AccountDistribution distribution : request.getAccountDistributions()) {
            ChartOfAccounts account = chartOfAccountRepo.findById(distribution.getAccountId())
                    .orElseThrow(() -> new ItemNotFoundException("Chart of account not found: " + distribution.getAccountId()));

            ProjectBudgetLineItemEntity lineItem = projectBudgetLineItemRepo.findByProjectBudgetAndChartOfAccount(projectBudget, account)
                    .orElseThrow(() -> new ItemNotFoundException("Line item not found for account: " + account.getName()));

            lineItem.setBudgetAmount(distribution.getAmount());
            lineItem.setLineItemNotes(distribution.getDescription());
            lineItem.setModifiedDate(LocalDateTime.now());

            projectBudgetLineItemRepo.save(lineItem);
        }


        projectBudget.recalculateTotalBudgetAmount();
        projectBudget.setModifiedBy(authenticatedAccount.getAccountId());
        projectBudget.setModifiedDate(LocalDateTime.now());

        // Update organization budget allocated amount
        OrgBudgetEntity orgBudget = projectBudget.getOrgBudget();
        orgBudget.setAllocatedAmount(orgBudget.getAllocatedAmount().add(totalDistributionAmount));
        orgBudgetRepo.save(orgBudget);

        return projectBudgetRepo.save(projectBudget);
    }

    @Override
    public ProjectBudgetEntity getProjectBudget(UUID projectId, UUID organisationId)
            throws ItemNotFoundException, AccessDeniedException {

        AccountEntity authenticatedAccount = getAuthenticatedAccount();

        OrganisationEntity organisation = organisationRepo.findById(organisationId).orElseThrow(() -> new ItemNotFoundException("Organisation does not exist"));

        ProjectEntity project = projectRepo.findById(projectId)
                .orElseThrow(() -> new ItemNotFoundException("Project not found"));

        validateProject(project, organisation);

        validateProjectMemberPermissions(authenticatedAccount, project,
                List.of(TeamMemberRole.ACCOUNTANT, TeamMemberRole.OWNER, TeamMemberRole.PROJECT_MANAGER, TeamMemberRole.MEMBER));

        return projectBudgetRepo.findByProject(project)
                .orElseThrow(() -> new ItemNotFoundException("Project budget not found. Initialize budget first."));
    }



    @Override
    public List<ProjectBudgetSummaryResponse> getProjectBudgetSummary(UUID projectId, UUID organisationId)
            throws ItemNotFoundException, AccessDeniedException {


        AccountEntity authenticatedAccount = getAuthenticatedAccount();
        OrganisationEntity organisation = organisationRepo.findById(organisationId)
                .orElseThrow(() -> new ItemNotFoundException("Organisation not found"));
        ProjectEntity project = projectRepo.findById(projectId)
                .orElseThrow(() -> new ItemNotFoundException("Project not found"));

        validateProject(project, organisation);
        validateProjectMemberPermissions(authenticatedAccount, project,
                List.of(TeamMemberRole.ACCOUNTANT, TeamMemberRole.OWNER,
                        TeamMemberRole.PROJECT_MANAGER, TeamMemberRole.MEMBER));


        ProjectBudgetEntity projectBudget = projectBudgetRepo.findByProject(project)
                .orElseThrow(() -> new ItemNotFoundException("Project budget not found"));


        List<ChartOfAccounts> allExpenseAccounts = chartOfAccountRepo
                .findByOrganisationAndAccountTypeAndIsActive(
                        organisation, AccountType.EXPENSE, true);


        Map<UUID, ChartOfAccounts> accountMap = allExpenseAccounts.stream()
                .collect(Collectors.toMap(ChartOfAccounts::getId, account -> account));

        Map<UUID, ProjectBudgetLineItemEntity> lineItemMap = projectBudget.getLineItems().stream()
                .collect(Collectors.toMap(
                        lineItem -> lineItem.getChartOfAccount().getId(),
                        lineItem -> lineItem));


        return allExpenseAccounts.stream()
                .filter(account -> !account.getIsHeader() && account.getIsPostable())
                .filter(account -> lineItemMap.containsKey(account.getId()))
                .map(account -> createBudgetSummaryResponse(account, lineItemMap.get(account.getId()), accountMap))
                .collect(Collectors.toList());
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

    private void validateProjectMemberPermissions(AccountEntity account, ProjectEntity project, List<TeamMemberRole> allowedRoles)
            throws ItemNotFoundException, AccessDeniedException {

        OrganisationMember organisationMember = organisationMemberRepo.findByAccountAndOrganisation(account, project.getOrganisation())
                .orElseThrow(() -> new ItemNotFoundException("Authenticated user is not part of this organisation"));

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

    private ProjectBudgetSummaryResponse createBudgetSummaryResponse(
            ChartOfAccounts account,
            ProjectBudgetLineItemEntity lineItem,
            Map<UUID, ChartOfAccounts> accountMap) {

        ProjectBudgetSummaryResponse response = new ProjectBudgetSummaryResponse();

        response.setAccountId(lineItem.getLineItemId());
        response.setAccountCode(account.getAccountCode());
        response.setAccountName(account.getName());
        response.setBudgetRemaining(lineItem.getRemainingAmount());
        response.setAvailableBalance(BigDecimal.ZERO);
        response.setNotes(lineItem.getLineItemNotes());


        String parentName = getImmediateParentName(account, accountMap);
        response.setHeadingParent(parentName);

        return response;
    }

    private String getImmediateParentName(ChartOfAccounts account, Map<UUID, ChartOfAccounts> accountMap) {
        if (account.getParentAccountId() != null) {
            ChartOfAccounts parent = accountMap.get(account.getParentAccountId());
            if (parent != null) {
                return parent.getName();
            }
        }
        return "Uncategorized";
    }
}