package com.qbitspark.buildwisebackend.accounting_service.budget_mng.org_budget.service.impl;

import com.qbitspark.buildwisebackend.accounting_service.budget_mng.org_budget.entity.OrgBudgetEntity;
import com.qbitspark.buildwisebackend.accounting_service.budget_mng.org_budget.entity.OrgBudgetLineItemEntity;
import com.qbitspark.buildwisebackend.accounting_service.budget_mng.org_budget.enums.OrgBudgetStatus;
import com.qbitspark.buildwisebackend.accounting_service.budget_mng.org_budget.paylaods.CreateBudgetRequest;
import com.qbitspark.buildwisebackend.accounting_service.budget_mng.org_budget.paylaods.DistributeBudgetRequest;
import com.qbitspark.buildwisebackend.accounting_service.budget_mng.org_budget.paylaods.OrgBudgetSummaryResponse;
import com.qbitspark.buildwisebackend.accounting_service.budget_mng.org_budget.paylaods.UpdateBudgetRequest;
import com.qbitspark.buildwisebackend.accounting_service.budget_mng.org_budget.repo.OrgBudgetLineItemRepo;
import com.qbitspark.buildwisebackend.accounting_service.budget_mng.org_budget.repo.OrgBudgetRepo;
import com.qbitspark.buildwisebackend.accounting_service.budget_mng.org_budget.service.OrgBudgetService;
import com.qbitspark.buildwisebackend.accounting_service.coa.entity.ChartOfAccounts;
import com.qbitspark.buildwisebackend.accounting_service.coa.enums.AccountType;
import com.qbitspark.buildwisebackend.accounting_service.coa.repo.ChartOfAccountsRepo;
import com.qbitspark.buildwisebackend.authentication_service.Repository.AccountRepo;
import com.qbitspark.buildwisebackend.authentication_service.entity.AccountEntity;
import com.qbitspark.buildwisebackend.globeadvice.exceptions.ItemNotFoundException;
import com.qbitspark.buildwisebackend.organisation_service.organisation_mng.entity.OrganisationEntity;
import com.qbitspark.buildwisebackend.organisation_service.organisation_mng.repo.OrganisationRepo;
import com.qbitspark.buildwisebackend.organisation_service.orgnisation_members_mng.entities.OrganisationMember;
import com.qbitspark.buildwisebackend.organisation_service.orgnisation_members_mng.enums.MemberRole;
import com.qbitspark.buildwisebackend.organisation_service.orgnisation_members_mng.enums.MemberStatus;
import com.qbitspark.buildwisebackend.organisation_service.orgnisation_members_mng.repo.OrganisationMemberRepo;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class OrgBudgetServiceImpl implements OrgBudgetService {

    private final OrganisationRepo organisationRepo;
    private final OrganisationMemberRepo organisationMemberRepo;
    private final AccountRepo accountRepo;
    private final OrgBudgetRepo orgBudgetRepo;
    private final OrgBudgetLineItemRepo orgBudgetLineItemRepo;
    private final ChartOfAccountsRepo chartOfAccountsRepo;

    @Override
    public OrgBudgetEntity createBudget(CreateBudgetRequest request, UUID organisationId) throws ItemNotFoundException {

        AccountEntity authenticatedAccount = getAuthenticatedAccount();


        OrganisationEntity organisation = organisationRepo.findById(organisationId)
                .orElseThrow(() -> new ItemNotFoundException("Organisation does not exist"));

        validateNoOverlappingFinancialYearWithQuery(organisation, request.getFinancialYearStart(), request.getFinancialYearEnd());


        validateMemberPermissions(authenticatedAccount, organisation,
                Arrays.asList(MemberRole.OWNER, MemberRole.ADMIN));

        // Auto-generate budget name based on financial year
        String budgetName = generateBudgetName(request.getFinancialYearStart(), request.getFinancialYearEnd());

        OrgBudgetEntity budget = new OrgBudgetEntity();
        budget.setOrganisation(organisation);
        budget.setBudgetName(budgetName);
        budget.setFinancialYearStart(request.getFinancialYearStart());
        budget.setFinancialYearEnd(request.getFinancialYearEnd());
        budget.setSpentAmount(BigDecimal.ZERO);
        budget.setDescription(request.getDescription());
        budget.setCreatedBy(authenticatedAccount.getAccountId());
        budget.setCreatedDate(LocalDateTime.now());
        budget.setStatus(OrgBudgetStatus.DRAFT);
        budget.setModifiedBy(authenticatedAccount.getAccountId());
        budget.setModifiedDate(LocalDateTime.now());
        budget.setBudgetVersion(1);

        OrgBudgetEntity savedBudget = orgBudgetRepo.save(budget);

        initializeBudgetWithAccounts(savedBudget.getBudgetId(), organisationId);

        return savedBudget;
    }

    @Override
    public void initializeBudgetWithAccounts(UUID budgetId, UUID organisationId) throws ItemNotFoundException {

        OrgBudgetEntity budget = orgBudgetRepo.findById(budgetId)
                .orElseThrow(() -> new ItemNotFoundException("Budget not found"));

        OrganisationEntity organisation = budget.getOrganisation();

        // Get all EXPENSE HEADER accounts that are active
        List<ChartOfAccounts> expenseHeaderAccounts = chartOfAccountsRepo
                .findByOrganisationAndAccountTypeAndIsActive(organisation, AccountType.EXPENSE, true)
                .stream()
                .filter(ChartOfAccounts::getIsHeader)
                .toList();

        if (expenseHeaderAccounts.isEmpty()) {
            throw new ItemNotFoundException("No expense header accounts found in this organisation. Please configure chart of accounts with header accounts first.");
        }

        // Create line items for each expense HEADER account with Ths 0 budget
        for (ChartOfAccounts headerAccount : expenseHeaderAccounts) {
            if (!orgBudgetLineItemRepo.existsByOrgBudgetAndChartOfAccount(budget, headerAccount)) {
                OrgBudgetLineItemEntity lineItem = new OrgBudgetLineItemEntity();
                lineItem.setOrgBudget(budget);
                lineItem.setChartOfAccount(headerAccount);
                lineItem.setBudgetAmount(BigDecimal.ZERO);
                lineItem.setSpentAmount(BigDecimal.ZERO);
                lineItem.setCommittedAmount(BigDecimal.ZERO);
                lineItem.setLineItemNotes("Header account - awaiting budget distribution");
                lineItem.setCreatedDate(LocalDateTime.now());
                lineItem.setCreatedBy(budget.getCreatedBy());

                orgBudgetLineItemRepo.save(lineItem);
            }
        }
    }

    @Override
    public OrgBudgetEntity distributeBudget(UUID budgetId, DistributeBudgetRequest request, UUID organisationId)
            throws ItemNotFoundException {

        AccountEntity authenticatedAccount = getAuthenticatedAccount();

        OrganisationEntity organisation = organisationRepo.findById(organisationId)
                .orElseThrow(() -> new ItemNotFoundException("Organisation does not exist"));

        validateMemberPermissions(authenticatedAccount, organisation,
                Arrays.asList(MemberRole.OWNER, MemberRole.ADMIN));

        OrgBudgetEntity budget = orgBudgetRepo.findById(budgetId)
                .orElseThrow(() -> new ItemNotFoundException("Budget not found"));

        if (!budget.getOrganisation().getOrganisationId().equals(organisationId)) {
            throw new ItemNotFoundException("Budget does not belong to this organisation");
        }

        if (budget.getStatus() != OrgBudgetStatus.DRAFT && budget.getStatus() != OrgBudgetStatus.APPROVED) {
            throw new ItemNotFoundException("Only draft or approved budgets can be updated");
        }

        // Calculate the total distribution amount
        BigDecimal totalDistributionAmount = request.getAccountDistributions().stream()
                .map(DistributeBudgetRequest.AccountDistribution::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (totalDistributionAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ItemNotFoundException("Total distribution amount must be greater than zero");
        }

        for (DistributeBudgetRequest.AccountDistribution distribution : request.getAccountDistributions()) {
            ChartOfAccounts account = chartOfAccountsRepo.findById(distribution.getAccountId())
                    .orElseThrow(() -> new ItemNotFoundException("Chart of account not found: " + distribution.getAccountId()));

            if (!account.getOrganisation().getOrganisationId().equals(organisationId)) {
                throw new ItemNotFoundException("Account does not belong to this organisation");
            }

            if (!account.getIsHeader()) {
                throw new ItemNotFoundException("Budget can only be distributed to header accounts. Account '" +
                        account.getName() + "' is not a header account.");
            }

            if (account.getAccountType() != AccountType.EXPENSE) {
                throw new ItemNotFoundException("Budget can only be distributed to expense accounts. Account '" +
                        account.getName() + "' is not an expense account.");
            }

            OrgBudgetLineItemEntity lineItem = orgBudgetLineItemRepo
                    .findByOrgBudgetAndChartOfAccount(budget, account)
                    .orElseGet(() -> {
                        OrgBudgetLineItemEntity newLineItem = new OrgBudgetLineItemEntity();
                        newLineItem.setOrgBudget(budget);
                        newLineItem.setChartOfAccount(account);
                        newLineItem.setSpentAmount(BigDecimal.ZERO);
                        newLineItem.setCommittedAmount(BigDecimal.ZERO);
                        newLineItem.setCreatedDate(LocalDateTime.now());
                        newLineItem.setCreatedBy(authenticatedAccount.getAccountId());
                        return newLineItem;
                    });


            lineItem.setBudgetAmount(distribution.getAmount());
            lineItem.setLineItemNotes(distribution.getDescription());
            lineItem.setModifiedDate(LocalDateTime.now());
            lineItem.setModifiedBy(authenticatedAccount.getAccountId());

            orgBudgetLineItemRepo.save(lineItem);
        }


        budget.setModifiedBy(authenticatedAccount.getAccountId());
        budget.setModifiedDate(LocalDateTime.now());

        return orgBudgetRepo.save(budget);
    }

    @Override
    public OrgBudgetEntity getBudgetWithAccounts(UUID budgetId, UUID organisationId) throws ItemNotFoundException {

        AccountEntity authenticatedAccount = getAuthenticatedAccount();

        OrganisationEntity organisation = organisationRepo.findById(organisationId)
                .orElseThrow(() -> new ItemNotFoundException("Organisation does not exist"));

        validateMemberPermissions(authenticatedAccount, organisation,
                Arrays.asList(MemberRole.OWNER, MemberRole.ADMIN, MemberRole.MEMBER));

        OrgBudgetEntity budget = orgBudgetRepo.findById(budgetId)
                .orElseThrow(() -> new ItemNotFoundException("Budget not found"));

        if (!budget.getOrganisation().getOrganisationId().equals(organisationId)) {
            throw new ItemNotFoundException("Budget does not belong to this organisation");
        }

        // Force lazy loading of line items
        budget.getLineItems().size();

        return budget;
    }

    @Override
    public OrgBudgetSummaryResponse getBudgetSummary(UUID budgetId, UUID organisationId) throws ItemNotFoundException {

        OrgBudgetEntity budget = getBudgetWithAccounts(budgetId, organisationId);

        OrgBudgetSummaryResponse summary = new OrgBudgetSummaryResponse();
        summary.setBudgetId(budget.getBudgetId());
        summary.setBudgetName(budget.getBudgetName());
        summary.setTotalBudgetAmount(budget.getTotalBudgetAmount()); // Calculated from line items
        summary.setDistributedAmount(budget.getDistributedAmount());
        summary.setAvailableAmount(budget.getAvailableAmount()); // Always ZERO
        summary.setTotalSpentAmount(budget.getTotalSpentFromLineItems());
        summary.setTotalCommittedAmount(budget.getTotalCommittedAmount());
        summary.setTotalRemainingAmount(budget.getTotalRemainingAmount());
        summary.setStatus(budget.getStatus());
        summary.setFinancialYearStart(budget.getFinancialYearStart());
        summary.setFinancialYearEnd(budget.getFinancialYearEnd());
        summary.setBudgetUtilizationPercentage(budget.getBudgetUtilizationPercentage()); // Always 100%
        summary.setSpendingPercentage(budget.getSpendingPercentage());

        // Account statistics
        summary.setTotalAccounts(budget.getLineItems().size());
        summary.setAccountsWithBudget((int) budget.getLineItemsWithBudgetCount());
        summary.setAccountsWithoutBudget((int) budget.getLineItemsWithoutBudgetCount());

        return summary;
    }

    // Existing methods remain the same...
    @Override
    public void activateBudget(UUID budgetId, UUID organisationId) throws ItemNotFoundException {

        AccountEntity authenticatedAccount = getAuthenticatedAccount();

        OrganisationEntity organisation = organisationRepo.findById(organisationId)
                .orElseThrow(() -> new ItemNotFoundException("Organisation does not exist"));

        validateMemberPermissions(authenticatedAccount, organisation,
                Arrays.asList(MemberRole.OWNER, MemberRole.ADMIN));

        OrgBudgetEntity budgetToActivate = orgBudgetRepo.findById(budgetId)
                .orElseThrow(() -> new ItemNotFoundException("Budget not found"));

        if (!budgetToActivate.getOrganisation().getOrganisationId().equals(organisationId)) {
            throw new ItemNotFoundException("Budget does not belong to this organisation");
        }

        // Validate budget status (only APPROVED budgets can be activated)
        if (budgetToActivate.getStatus() != OrgBudgetStatus.APPROVED) {
            throw new ItemNotFoundException("Only approved budgets can be activated");
        }

        // Deactivate any existing active budget
        Optional<OrgBudgetEntity> currentActiveBudget = orgBudgetRepo
                .findByOrganisationAndStatus(organisation, OrgBudgetStatus.ACTIVE);

        if (currentActiveBudget.isPresent()) {
            OrgBudgetEntity activeBudget = currentActiveBudget.get();
            activeBudget.setStatus(OrgBudgetStatus.CLOSED);
            activeBudget.setModifiedBy(authenticatedAccount.getAccountId());
            activeBudget.setModifiedDate(LocalDateTime.now());
            orgBudgetRepo.save(activeBudget);
        }

        // Activate the new budget
        budgetToActivate.setStatus(OrgBudgetStatus.ACTIVE);
        budgetToActivate.setModifiedBy(authenticatedAccount.getAccountId());
        budgetToActivate.setModifiedDate(LocalDateTime.now());
        orgBudgetRepo.save(budgetToActivate);
    }

    @Override
    public List<OrgBudgetEntity> getBudgets(UUID organisationId) throws ItemNotFoundException {

        AccountEntity authenticatedAccount = getAuthenticatedAccount();

        OrganisationEntity organisation = organisationRepo.findById(organisationId)
                .orElseThrow(() -> new ItemNotFoundException("Organisation does not exist"));

        validateMemberPermissions(authenticatedAccount, organisation,
                Arrays.asList(MemberRole.OWNER, MemberRole.ADMIN, MemberRole.MEMBER));

        return orgBudgetRepo.findByOrganisation(organisation);
    }

    @Override
    public OrgBudgetEntity updateBudget(UUID budgetId, UpdateBudgetRequest request, UUID organisationId) throws ItemNotFoundException {

        AccountEntity authenticatedAccount = getAuthenticatedAccount();

        OrganisationEntity organisation = organisationRepo.findById(organisationId)
                .orElseThrow(() -> new ItemNotFoundException("Organisation does not exist"));

        validateMemberPermissions(authenticatedAccount, organisation,
                Arrays.asList(MemberRole.OWNER, MemberRole.ADMIN));

        OrgBudgetEntity budgetToUpdate = orgBudgetRepo.findById(budgetId)
                .orElseThrow(() -> new ItemNotFoundException("Budget not found"));

        if (!budgetToUpdate.getOrganisation().getOrganisationId().equals(organisationId)) {
            throw new ItemNotFoundException("Budget does not belong to this organisation");
        }

        // Only allow updates to DRAFT budgets
        if (budgetToUpdate.getStatus() != OrgBudgetStatus.DRAFT) {
            throw new ItemNotFoundException("Only draft budgets can be updated");
        }

        // Update fields if provided
        if (request.getFinancialYearStart() != null) {
            budgetToUpdate.setFinancialYearStart(request.getFinancialYearStart());
        }

        if (request.getFinancialYearEnd() != null) {
            budgetToUpdate.setFinancialYearEnd(request.getFinancialYearEnd());
        }


        if (request.getDescription() != null) {
            budgetToUpdate.setDescription(request.getDescription());
        }

        // Update budget name if dates changed
        if (request.getFinancialYearStart() != null || request.getFinancialYearEnd() != null) {
            String newBudgetName = generateBudgetName(
                    budgetToUpdate.getFinancialYearStart(),
                    budgetToUpdate.getFinancialYearEnd()
            );
            budgetToUpdate.setBudgetName(newBudgetName);
        }

        // Update metadata
        budgetToUpdate.setModifiedBy(authenticatedAccount.getAccountId());
        budgetToUpdate.setModifiedDate(LocalDateTime.now());

        return orgBudgetRepo.save(budgetToUpdate);
    }

    //Generate budget name
    private String generateBudgetName(LocalDate startDate, LocalDate endDate) {
        String startMonth = startDate.getMonth().name().substring(0, 3);
        String endMonth = endDate.getMonth().name().substring(0, 3);
        int startYear = startDate.getYear() % 100;
        int endYear = endDate.getYear() % 100;

        return String.format("FY %s%02d-%s%02d",
                startMonth, startYear, endMonth, endYear);
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


    private void validateNoOverlappingFinancialYearWithQuery(OrganisationEntity organisation,
                                                             LocalDate requestedStart, LocalDate requestedEnd) throws ItemNotFoundException {

        List<OrgBudgetEntity> overlappingBudgets = orgBudgetRepo
                .findByOrganisationAndFinancialYearStartLessThanEqualAndFinancialYearEndGreaterThanEqual(
                        organisation, requestedEnd, requestedStart);

        if (!overlappingBudgets.isEmpty()) {
            OrgBudgetEntity conflictingBudget = overlappingBudgets.getFirst();
            throw new ItemNotFoundException(String.format(
                    "Financial year conflict detected. The requested period (%s to %s) overlaps with existing budget '%s' (%s to %s)",
                    requestedStart, requestedEnd,
                    conflictingBudget.getBudgetName(),
                    conflictingBudget.getFinancialYearStart(), conflictingBudget.getFinancialYearEnd()
            ));
        }
    }
}