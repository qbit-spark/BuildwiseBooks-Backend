package com.qbitspark.buildwisebackend.accounting_service.budget_mng.org_budget.service.impl;

import com.qbitspark.buildwisebackend.accounting_service.budget_mng.org_budget.entity.OrgBudgetDetailDistributionEntity;
import com.qbitspark.buildwisebackend.accounting_service.budget_mng.org_budget.entity.OrgBudgetEntity;
import com.qbitspark.buildwisebackend.accounting_service.budget_mng.org_budget.enums.OrgBudgetStatus;
import com.qbitspark.buildwisebackend.accounting_service.budget_mng.org_budget.paylaods.*;
import com.qbitspark.buildwisebackend.accounting_service.budget_mng.org_budget.repo.OrgBudgetDetailDistributionRepo;
import com.qbitspark.buildwisebackend.accounting_service.budget_mng.org_budget.repo.OrgBudgetRepo;
import com.qbitspark.buildwisebackend.accounting_service.budget_mng.org_budget.service.OrgBudgetService;
import com.qbitspark.buildwisebackend.accounting_service.budget_mng.org_budget.utitls.BudgetAllocationResponseUtils;
import com.qbitspark.buildwisebackend.accounting_service.coa.entity.ChartOfAccounts;
import com.qbitspark.buildwisebackend.accounting_service.coa.enums.AccountType;
import com.qbitspark.buildwisebackend.accounting_service.coa.repo.ChartOfAccountsRepo;
import com.qbitspark.buildwisebackend.accounting_service.documentflow.invoice.enums.ActionType;
import com.qbitspark.buildwisebackend.approval_service.enums.ServiceType;
import com.qbitspark.buildwisebackend.approval_service.service.ApprovalIntegrationService;
import com.qbitspark.buildwisebackend.approval_service.service.ApprovalWorkflowService;
import com.qbitspark.buildwisebackend.authentication_service.Repository.AccountRepo;
import com.qbitspark.buildwisebackend.authentication_service.entity.AccountEntity;
import com.qbitspark.buildwisebackend.globeadvice.exceptions.AccessDeniedException;
import com.qbitspark.buildwisebackend.globeadvice.exceptions.ItemNotFoundException;
import com.qbitspark.buildwisebackend.organisation_service.organisation_mng.entity.OrganisationEntity;
import com.qbitspark.buildwisebackend.organisation_service.organisation_mng.repo.OrganisationRepo;
import com.qbitspark.buildwisebackend.organisation_service.orgnisation_members_mng.entities.OrganisationMember;
import com.qbitspark.buildwisebackend.organisation_service.orgnisation_members_mng.enums.MemberStatus;
import com.qbitspark.buildwisebackend.organisation_service.orgnisation_members_mng.repo.OrganisationMemberRepo;
import com.qbitspark.buildwisebackend.organisation_service.roles_mng.service.PermissionCheckerService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class OrgBudgetServiceImpl implements OrgBudgetService {

    private final OrganisationRepo organisationRepo;
    private final OrganisationMemberRepo organisationMemberRepo;
    private final AccountRepo accountRepo;
    private final OrgBudgetRepo orgBudgetRepo;
    private final ChartOfAccountsRepo chartOfAccountsRepo;
    private final OrgBudgetDetailDistributionRepo detailDistributionRepo;
    private final PermissionCheckerService permissionChecker;
    private final BudgetAllocationResponseUtils budgetAllocationResponseUtils;
    private final ApprovalIntegrationService approvalIntegrationService;
    private final ApprovalWorkflowService approvalWorkflowService;


    @Override
    public OrgBudgetEntity createBudget(CreateBudgetRequest request, UUID organisationId)
            throws ItemNotFoundException, AccessDeniedException {

        AccountEntity currentAccount = getAuthenticatedAccount();
        OrganisationEntity organisation = organisationRepo.findById(organisationId)
                .orElseThrow(() -> new ItemNotFoundException("Organisation not found"));

        OrganisationMember member = validateOrganisationMemberAccess(currentAccount, organisation);
        permissionChecker.checkMemberPermission(member, "BUDGET", "createBudget");

        validateNoOverlappingFinancialYear(organisation, request.getFinancialYearStart(), request.getFinancialYearEnd());

        String budgetName = generateBudgetName(request.getFinancialYearStart(), request.getFinancialYearEnd());

        OrgBudgetEntity budget = new OrgBudgetEntity();
        budget.setOrganisation(organisation);
        budget.setBudgetName(budgetName);
        budget.setFinancialYearStart(request.getFinancialYearStart());
        budget.setFinancialYearEnd(request.getFinancialYearEnd());
        budget.setDescription(request.getDescription());
        budget.setCreatedBy(currentAccount.getAccountId());
        budget.setCreatedDate(LocalDateTime.now());
        budget.setStatus(OrgBudgetStatus.DRAFT);
        budget.setModifiedBy(currentAccount.getAccountId());
        budget.setModifiedDate(LocalDateTime.now());
        budget.setBudgetVersion(1);

        OrgBudgetEntity savedBudget = orgBudgetRepo.save(budget);
        initializeBudgetDistribution(savedBudget, organisation, currentAccount);

        return savedBudget;
    }

    @Override
    public List<OrgBudgetDetailDistributionEntity> distributeToDetails(UUID budgetId, DistributeToDetailsRequest request, UUID organisationId, ActionType action)
            throws ItemNotFoundException, AccessDeniedException {

        AccountEntity authenticatedAccount = getAuthenticatedAccount();
        OrganisationEntity organisation = organisationRepo.findById(organisationId)
                .orElseThrow(() -> new ItemNotFoundException("Organisation not found"));

        OrganisationMember member = validateOrganisationMemberAccess(authenticatedAccount, organisation);
        permissionChecker.checkMemberPermission(member, "BUDGET", "distributeBudget");

        OrgBudgetEntity budget = orgBudgetRepo.findById(budgetId)
                .orElseThrow(() -> new ItemNotFoundException("Budget not found"));

        if (budget.getStatus() != OrgBudgetStatus.DRAFT) {
            throw new ItemNotFoundException("Only draft budgets can be distributed");
        }

        if (!budget.getOrganisation().getOrganisationId().equals(organisationId)) {
            throw new ItemNotFoundException("Budget does not belong to this organisation");
        }


        validateNoDuplicateAccounts(request);

        // Handle approval workflow
        if (action == ActionType.SAVE_AND_APPROVAL) {
            // 1. Update document status via integration service
            approvalIntegrationService.submitForApproval(
                    ServiceType.BUDGET,
                    budget.getBudgetId(),
                    organisationId,
                    null
            );

            // 2. Start the workflow directly
            approvalWorkflowService.startApprovalWorkflow(
                    ServiceType.BUDGET,
                    budget.getBudgetId(),
                    organisationId,
                    null
            );
        }

        return processDistributions(request, budget, organisationId, authenticatedAccount);

    }


    @Override
    public List<OrgBudgetEntity> getBudgets(UUID organisationId)
            throws ItemNotFoundException, AccessDeniedException {

        AccountEntity authenticatedAccount = getAuthenticatedAccount();
        OrganisationEntity organisation = organisationRepo.findById(organisationId)
                .orElseThrow(() -> new ItemNotFoundException("Organisation not found"));

        OrganisationMember member = validateOrganisationMemberAccess(authenticatedAccount, organisation);
        permissionChecker.checkMemberPermission(member, "BUDGET", "viewBudget");

        return orgBudgetRepo.findByOrganisation(organisation);
    }

    @Override
    public OrgBudgetEntity updateBudget(UUID budgetId, UpdateBudgetRequest request, UUID organisationId, ActionType action)
            throws ItemNotFoundException, AccessDeniedException {

        AccountEntity authenticatedAccount = getAuthenticatedAccount();
        OrganisationEntity organisation = organisationRepo.findById(organisationId)
                .orElseThrow(() -> new ItemNotFoundException("Organisation not found"));

        OrganisationMember member = validateOrganisationMemberAccess(authenticatedAccount, organisation);
        permissionChecker.checkMemberPermission(member, "BUDGET", "updateBudget");

        OrgBudgetEntity budget = orgBudgetRepo.findById(budgetId)
                .orElseThrow(() -> new ItemNotFoundException("Budget not found"));

        if (!budget.getOrganisation().getOrganisationId().equals(organisationId)) {
            throw new ItemNotFoundException("Budget does not belong to this organisation");
        }

        if (budget.getStatus() != OrgBudgetStatus.DRAFT) {
            throw new ItemNotFoundException("Only draft budgets can be updated");
        }

        if (request.getFinancialYearStart() != null) {
            budget.setFinancialYearStart(request.getFinancialYearStart());
        }

        if (request.getFinancialYearEnd() != null) {
            budget.setFinancialYearEnd(request.getFinancialYearEnd());
        }

        if (request.getDescription() != null) {
            budget.setDescription(request.getDescription());
        }

        if (request.getFinancialYearStart() != null || request.getFinancialYearEnd() != null) {
            String newBudgetName = generateBudgetName(
                    budget.getFinancialYearStart(),
                    budget.getFinancialYearEnd());
            budget.setBudgetName(newBudgetName);
        }

        budget.setModifiedBy(authenticatedAccount.getAccountId());
        budget.setModifiedDate(LocalDateTime.now());

        OrgBudgetEntity savedBudget =  orgBudgetRepo.save(budget);

        if (action == ActionType.SAVE_AND_APPROVAL) {
            // 1. Update document status via integration service
            approvalIntegrationService.submitForApproval(
                    ServiceType.BUDGET,
                    savedBudget.getBudgetId(),
                    organisationId,
                    null
            );

            // 2. Start the workflow directly
            approvalWorkflowService.startApprovalWorkflow(
                    ServiceType.BUDGET,
                    savedBudget.getBudgetId(),
                    organisationId,
                    null
            );
        }
        return savedBudget;
    }


    public void initializeBudgetDistribution(OrgBudgetEntity budget, OrganisationEntity organisation, AccountEntity currentAccount)
            throws ItemNotFoundException {

        if (!budget.getOrganisation().getOrganisationId().equals(organisation.getOrganisationId())) {
            throw new ItemNotFoundException("Budget does not belong to this organisation");
        }

        // Get all DETAIL expense accounts (not headers)
        List<ChartOfAccounts> detailExpenseAccounts = chartOfAccountsRepo
                .findByOrganisationAndAccountTypeAndIsActive(organisation, AccountType.EXPENSE, true)
                .stream()
                .filter(account -> !account.getIsHeader()) // Only detail accounts
                .filter(account -> account.getParentAccountId() != null) // Must have parent
                .toList();

        if (detailExpenseAccounts.isEmpty()) {
            throw new ItemNotFoundException("No detail expense accounts found. Please configure chart of accounts first.");
        }

        // Create a distribution for each detail account with amount 0
        for (ChartOfAccounts detailAccount : detailExpenseAccounts) {

            // Check if distribution already exists
            List<OrgBudgetDetailDistributionEntity> existing = detailDistributionRepo
                    .findByBudgetAndDetailAccount(budget, detailAccount);

            if (existing.isEmpty()) {
                OrgBudgetDetailDistributionEntity distribution = new OrgBudgetDetailDistributionEntity();
                distribution.setBudget(budget);
                distribution.setDetailAccount(detailAccount);
                distribution.setDistributedAmount(BigDecimal.ZERO); // Start with 0
                distribution.setDescription("Initial distribution - awaiting budget allocation");
                distribution.setCreatedDate(LocalDateTime.now());
                distribution.setCreatedBy(currentAccount.getAccountId());

                detailDistributionRepo.save(distribution);
            }
        }
    }


    @Override
    public BudgetDistributionDetailResponse getBudgetDistributionDetails(UUID budgetId, UUID organisationId)
            throws ItemNotFoundException, AccessDeniedException {

        AccountEntity authenticatedAccount = getAuthenticatedAccount();
        OrganisationEntity organisation = organisationRepo.findById(organisationId)
                .orElseThrow(() -> new ItemNotFoundException("Organisation not found"));

        OrganisationMember member = validateOrganisationMemberAccess(authenticatedAccount, organisation);
        permissionChecker.checkMemberPermission(member, "BUDGET", "viewBudget");

        OrgBudgetEntity budget = orgBudgetRepo.findById(budgetId)
                .orElseThrow(() -> new ItemNotFoundException("Budget not found"));


        if (!budget.getOrganisation().getOrganisationId().equals(organisationId)) {
            throw new ItemNotFoundException("Budget does not belong to this organisation");
        }

        // Get existing distributions
        List<OrgBudgetDetailDistributionEntity> existingDistributions = detailDistributionRepo.findByBudget(budget);

        // Get ALL detail expense accounts (not just those with distributions)
        List<ChartOfAccounts> allDetailAccounts = chartOfAccountsRepo
                .findByOrganisationAndAccountTypeAndIsActive(organisation, AccountType.EXPENSE, true)
                .stream()
                .filter(account -> !account.getIsHeader())
                .toList();

        // Pre-validate for duplicates before creating map
        Map<UUID, Long> accountCounts = existingDistributions.stream()
                .collect(Collectors.groupingBy(
                        dist -> dist.getDetailAccount().getId(),
                        Collectors.counting()
                ));

        Optional<UUID> duplicateAccount = accountCounts.entrySet().stream()
                .filter(entry -> entry.getValue() > 1)
                .map(Map.Entry::getKey)
                .findFirst();

        if (duplicateAccount.isPresent()) {
            OrgBudgetDetailDistributionEntity duplicate = existingDistributions.stream()
                    .filter(dist -> dist.getDetailAccount().getId().equals(duplicateAccount.get()))
                    .findFirst()
                    .orElseThrow();

            throw new ItemNotFoundException(String.format(
                    "Data integrity issue: Multiple distributions found for account %s (%s). " +
                            "Please contact system administrator to resolve duplicate distributions.",
                    duplicate.getDetailAccount().getAccountCode(),
                    duplicate.getDetailAccount().getName()
            ));
        }

        Map<UUID, OrgBudgetDetailDistributionEntity> distributionMap = existingDistributions.stream()
                .collect(Collectors.toMap(
                        dist -> dist.getDetailAccount().getId(),
                        Function.identity()
                ));

        return buildDistributionResponseWithAllAccounts(budget, allDetailAccounts, distributionMap, organisation);
    }


    @Override
    public void activateBudget(UUID budgetId, UUID organisationId)
            throws ItemNotFoundException, AccessDeniedException {

        AccountEntity authenticatedAccount = getAuthenticatedAccount();
        OrganisationEntity organisation = organisationRepo.findById(organisationId)
                .orElseThrow(() -> new ItemNotFoundException("Organisation not found"));

        OrganisationMember member = validateOrganisationMemberAccess(authenticatedAccount, organisation);
        permissionChecker.checkMemberPermission(member, "BUDGET", "activateBudget");

        OrgBudgetEntity budgetToActivate = orgBudgetRepo.findById(budgetId)
                .orElseThrow(() -> new ItemNotFoundException("Budget not found"));

        if (!budgetToActivate.getOrganisation().getOrganisationId().equals(organisationId)) {
            throw new ItemNotFoundException("Budget does not belong to this organisation");
        }

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
    public BudgetAllocationResponse getBudgetAllocationSummary(UUID budgetId, UUID organisationId)
            throws ItemNotFoundException, AccessDeniedException {

        AccountEntity currentUser = getAuthenticatedAccount();
        OrganisationEntity organisation = organisationRepo.findById(organisationId)
                .orElseThrow(() -> new ItemNotFoundException("Organisation not found"));

        OrganisationMember member = validateOrganisationMemberAccess(currentUser, organisation);
        permissionChecker.checkMemberPermission(member, "BUDGET", "viewBudget");

        OrgBudgetEntity budget = orgBudgetRepo.findById(budgetId)
                .orElseThrow(() -> new ItemNotFoundException("Budget not found"));

        if (!budget.getOrganisation().getOrganisationId().equals(organisationId)) {
            throw new ItemNotFoundException("Budget does not belong to this organisation");
        }

        return budgetAllocationResponseUtils.buildBudgetAllocationResponse(budget, organisation);
    }


    // ======= VALIDATION METHODS ==============

    private void validateNoOverlappingFinancialYear(OrganisationEntity organisation,
                                                    LocalDate requestedStart, LocalDate requestedEnd)
            throws ItemNotFoundException {

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

    private OrganisationMember validateOrganisationMemberAccess(AccountEntity account, OrganisationEntity organisation)
            throws ItemNotFoundException {
        OrganisationMember member = organisationMemberRepo.findByAccountAndOrganisation(account, organisation)
                .orElseThrow(() -> new ItemNotFoundException("Member not found in organisation"));

        if (member.getStatus() != MemberStatus.ACTIVE) {
            throw new ItemNotFoundException("Member is not active");
        }

        return member;
    }


    // ========== HELPER METHODS ==========

    private String generateBudgetName(LocalDate startDate, LocalDate endDate) {
        String startMonth = startDate.getMonth().name().substring(0, 3);
        String endMonth = endDate.getMonth().name().substring(0, 3);
        int startYear = startDate.getYear() % 100;
        int endYear = endDate.getYear() % 100;

        return String.format("FY %s%02d-%s%02d", startMonth, startYear, endMonth, endYear);
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


    private BudgetDistributionDetailResponse buildDistributionResponseWithAllAccounts(
            OrgBudgetEntity budget,
            List<ChartOfAccounts> allDetailAccounts,
            Map<UUID, OrgBudgetDetailDistributionEntity> distributionMap,
            OrganisationEntity organisation) {

        // Group detail accounts by header
        Map<UUID, List<ChartOfAccounts>> accountsByHeader = allDetailAccounts.stream()
                .collect(Collectors.groupingBy(ChartOfAccounts::getParentAccountId));

        // Get header accounts
        List<ChartOfAccounts> headerAccounts = chartOfAccountsRepo
                .findByOrganisationAndAccountTypeAndIsActive(organisation, AccountType.EXPENSE, true)
                .stream()
                .filter(ChartOfAccounts::getIsHeader)
                .toList();

        // Build header distributions
        List<BudgetDistributionDetailResponse.HeaderAccountDistribution> headerDistributions =
                headerAccounts.stream()
                        .map(header -> buildHeaderWithAllDetails(header, accountsByHeader.get(header.getId()), distributionMap))
                        .collect(Collectors.toList());

        // Calculate totals only from actual distributions (not zero amounts)
        BigDecimal totalDistributed = distributionMap.values().stream()
                .map(OrgBudgetDetailDistributionEntity::getDistributedAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        int withDistribution = (int) distributionMap.values().stream()
                .filter(d -> d.getDistributedAmount().compareTo(BigDecimal.ZERO) > 0)
                .count();

        // Build response
        BudgetDistributionDetailResponse response = new BudgetDistributionDetailResponse();
        response.setBudgetId(budget.getBudgetId());
        response.setBudgetName(budget.getBudgetName());
        response.setFinancialYearStart(budget.getFinancialYearStart());
        response.setFinancialYearEnd(budget.getFinancialYearEnd());
        response.setBudgetStatus(budget.getStatus().toString());
        response.setTotalDistributedAmount(totalDistributed);
        response.setTotalDetailAccounts(allDetailAccounts.size());
        response.setDetailsWithDistribution(withDistribution);
        response.setDetailsWithoutDistribution(allDetailAccounts.size() - withDistribution);
        response.setHeaderAccounts(headerDistributions);

        return response;
    }

    private BudgetDistributionDetailResponse.HeaderAccountDistribution buildHeaderWithAllDetails(
            ChartOfAccounts headerAccount,
            List<ChartOfAccounts> detailAccounts,
            Map<UUID, OrgBudgetDetailDistributionEntity> distributionMap) {

        if (detailAccounts == null) {
            detailAccounts = new ArrayList<>();
        }

        // Build detail distributions for ALL accounts
        List<BudgetDistributionDetailResponse.DetailAccountDistribution> detailDistributions =
                detailAccounts.stream()
                        .map(account -> buildDetailDistributionFromAccount(account, distributionMap.get(account.getId())))
                        .collect(Collectors.toList());

        // Calculate header totals from actual distributions only
        BigDecimal headerTotal = detailAccounts.stream()
                .map(account -> distributionMap.get(account.getId()))
                .filter(Objects::nonNull)
                .map(OrgBudgetDetailDistributionEntity::getDistributedAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        int withDistribution = (int) detailAccounts.stream()
                .map(account -> distributionMap.get(account.getId()))
                .filter(Objects::nonNull)
                .filter(d -> d.getDistributedAmount().compareTo(BigDecimal.ZERO) > 0)
                .count();

        BudgetDistributionDetailResponse.HeaderAccountDistribution header =
                new BudgetDistributionDetailResponse.HeaderAccountDistribution();

        header.setHeaderAccountId(headerAccount.getId());
        header.setHeaderAccountCode(headerAccount.getAccountCode());
        header.setHeaderAccountName(headerAccount.getName());
        header.setHeaderTotalDistributed(headerTotal);
        header.setDetailAccountCount(detailAccounts.size());
        header.setDetailsWithDistribution(withDistribution);
        header.setDetailsWithoutDistribution(detailAccounts.size() - withDistribution);
        header.setDetailAccounts(detailDistributions);

        return header;
    }

    private BudgetDistributionDetailResponse.DetailAccountDistribution buildDetailDistributionFromAccount(
            ChartOfAccounts detailAccount,
            OrgBudgetDetailDistributionEntity distribution) {

        BudgetDistributionDetailResponse.DetailAccountDistribution detail =
                new BudgetDistributionDetailResponse.DetailAccountDistribution();

        detail.setDetailAccountId(detailAccount.getId());
        detail.setDetailAccountCode(detailAccount.getAccountCode());
        detail.setDetailAccountName(detailAccount.getName());
        detail.setDetailAccountDescription(detailAccount.getDescription());

        if (distribution != null) {
            // Has distribution record
            detail.setDistributionId(distribution.getDistributionId());
            detail.setDistributedAmount(distribution.getDistributedAmount());
            detail.setDescription(distribution.getDescription());
            detail.setCreatedDate(distribution.getCreatedDate());
            detail.setHasDistribution(distribution.getDistributedAmount().compareTo(BigDecimal.ZERO) > 0);
            detail.setDistributionStatus(distribution.getDistributedAmount().compareTo(BigDecimal.ZERO) > 0 ? "Distributed" : "No Distribution");
        } else {
            // No distribution record - new COA account
            detail.setDistributionId(null);
            detail.setDistributedAmount(BigDecimal.ZERO);
            detail.setDescription("Account not yet distributed");
            detail.setCreatedDate(null);
            detail.setHasDistribution(false);
            detail.setDistributionStatus("No Distribution");
        }

        return detail;
    }

// ========================================
// EXTRACTED METHODS (from budget ownership check onward)
// ========================================

    private void validateNoDuplicateAccounts(DistributeToDetailsRequest request)
            throws ItemNotFoundException {

        Set<UUID> accountIds = new HashSet<>();

        for (DistributeToDetailsRequest.DetailDistribution dist : request.getDistributions()) {
            UUID accountId = dist.getDetailAccountId();

            if (accountIds.contains(accountId)) {
                ChartOfAccounts account = chartOfAccountsRepo.findById(accountId).orElse(null);
                String accountInfo = account != null ?
                        String.format("%s (%s)", account.getAccountCode(), account.getName()) :
                        accountId.toString();

                throw new ItemNotFoundException(String.format(
                        "Duplicate account in distribution request: %s. " +
                                "Each account can only appear once in a distribution request.",
                        accountInfo
                ));
            }
            accountIds.add(accountId);
        }
    }

    private List<OrgBudgetDetailDistributionEntity> processDistributions(
            DistributeToDetailsRequest request,
            OrgBudgetEntity budget,
            UUID organisationId,
            AccountEntity authenticatedAccount) throws ItemNotFoundException {

        List<OrgBudgetDetailDistributionEntity> savedDistributions = new ArrayList<>();

        for (DistributeToDetailsRequest.DetailDistribution dist : request.getDistributions()) {
            OrgBudgetDetailDistributionEntity distribution = processSingleDistribution(
                    dist, budget, organisationId, authenticatedAccount);
            savedDistributions.add(distribution);
        }

        return savedDistributions;
    }

    private OrgBudgetDetailDistributionEntity processSingleDistribution(
            DistributeToDetailsRequest.DetailDistribution dist,
            OrgBudgetEntity budget,
            UUID organisationId,
            AccountEntity authenticatedAccount) throws ItemNotFoundException {

        // Validate account
        ChartOfAccounts detailAccount = validateDistributionAccount(dist.getDetailAccountId(), organisationId);

        // Handle existing vs new distribution
        OrgBudgetDetailDistributionEntity distribution = createOrUpdateDistribution(
                dist, budget, detailAccount, authenticatedAccount);

        return detailDistributionRepo.save(distribution);
    }

    private ChartOfAccounts validateDistributionAccount(UUID accountId, UUID organisationId)
            throws ItemNotFoundException {

        ChartOfAccounts detailAccount = chartOfAccountsRepo.findById(accountId)
                .orElseThrow(() -> new ItemNotFoundException("Detail account not found " + accountId));

        if (!detailAccount.getOrganisation().getOrganisationId().equals(organisationId)) {
            throw new ItemNotFoundException("Detail account does not belong to this organisation");
        }

        if (detailAccount.getIsHeader()) {
            throw new ItemNotFoundException(String.format(
                    "You cannot distribute on header account %s (%s) %s",
                    detailAccount.getName(), detailAccount.getAccountCode(), detailAccount.getId()
            ));
        }

        return detailAccount;
    }

    private OrgBudgetDetailDistributionEntity createOrUpdateDistribution(
            DistributeToDetailsRequest.DetailDistribution dist,
            OrgBudgetEntity budget,
            ChartOfAccounts detailAccount,
            AccountEntity authenticatedAccount) throws ItemNotFoundException {

        List<OrgBudgetDetailDistributionEntity> existingDistributions = detailDistributionRepo
                .findByBudgetAndDetailAccount(budget, detailAccount);

        if (existingDistributions.isEmpty()) {
            return createNewDistribution(dist, budget, detailAccount, authenticatedAccount);

        } else if (existingDistributions.size() == 1) {
            return updateExistingDistribution(existingDistributions.get(0), dist);

        } else {
            throw new ItemNotFoundException(String.format(
                    "Data integrity issue: Multiple distributions found for account %s (%s) in budget %s. " +
                            "Please contact system administrator to resolve duplicate distributions.",
                    detailAccount.getAccountCode(),
                    detailAccount.getName(),
                    budget.getBudgetName()
            ));
        }
    }

    private OrgBudgetDetailDistributionEntity createNewDistribution(
            DistributeToDetailsRequest.DetailDistribution dist,
            OrgBudgetEntity budget,
            ChartOfAccounts detailAccount,
            AccountEntity authenticatedAccount) {

        OrgBudgetDetailDistributionEntity distribution = new OrgBudgetDetailDistributionEntity();
        distribution.setBudget(budget);
        distribution.setDetailAccount(detailAccount);
        distribution.setDistributedAmount(dist.getAmount());
        distribution.setDescription(dist.getDescription());
        distribution.setCreatedDate(LocalDateTime.now());
        distribution.setCreatedBy(authenticatedAccount.getAccountId());

        return distribution;
    }

    private OrgBudgetDetailDistributionEntity updateExistingDistribution(
            OrgBudgetDetailDistributionEntity existing,
            DistributeToDetailsRequest.DetailDistribution dist) {

        existing.setDistributedAmount(dist.getAmount());
        existing.setDescription(dist.getDescription());

        return existing;
    }

}