package com.qbitspark.buildwisebackend.accounting_service.budget_mng.org_budget.service.impl;

import com.qbitspark.buildwisebackend.accounting_service.budget_mng.org_budget.entity.OrgBudgetDetailAllocationEntity;
import com.qbitspark.buildwisebackend.accounting_service.budget_mng.org_budget.entity.OrgBudgetEntity;
import com.qbitspark.buildwisebackend.accounting_service.budget_mng.org_budget.entity.OrgBudgetLineItemEntity;
import com.qbitspark.buildwisebackend.accounting_service.budget_mng.org_budget.enums.OrgBudgetStatus;
import com.qbitspark.buildwisebackend.accounting_service.budget_mng.org_budget.paylaods.*;
import com.qbitspark.buildwisebackend.accounting_service.budget_mng.org_budget.repo.OrgBudgetDetailAllocationRepo;
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
import com.qbitspark.buildwisebackend.organisation_service.orgnisation_members_mng.enums.MemberStatus;
import com.qbitspark.buildwisebackend.organisation_service.orgnisation_members_mng.repo.OrganisationMemberRepo;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
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
    private final OrgBudgetLineItemRepo orgBudgetLineItemRepo;
    private final ChartOfAccountsRepo chartOfAccountsRepo;
    private final OrgBudgetDetailAllocationRepo allocationRepo;

    @Override
    public OrgBudgetEntity createBudget(CreateBudgetRequest request, UUID organisationId) throws ItemNotFoundException {

        AccountEntity authenticatedAccount = getAuthenticatedAccount();
        OrganisationEntity organisation = organisationRepo.findById(organisationId)
                .orElseThrow(() -> new ItemNotFoundException("Organisation does not exist"));

        validateNoOverlappingFinancialYear(organisation, request.getFinancialYearStart(), request.getFinancialYearEnd());
        validateMemberPermissions(authenticatedAccount, organisation, Arrays.asList(MemberRole.OWNER, MemberRole.ADMIN));

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

        List<ChartOfAccounts> expenseHeaderAccounts = chartOfAccountsRepo
                .findByOrganisationAndAccountTypeAndIsActive(organisation, AccountType.EXPENSE, true)
                .stream()
                .filter(ChartOfAccounts::getIsHeader)
                .toList();

        if (expenseHeaderAccounts.isEmpty()) {
            throw new ItemNotFoundException("No expense header accounts found in this organisation. Please configure chart of accounts with header accounts first.");
        }

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

        validateMemberPermissions(authenticatedAccount, organisation, Arrays.asList(MemberRole.OWNER, MemberRole.ADMIN));

        OrgBudgetEntity budget = orgBudgetRepo.findById(budgetId)
                .orElseThrow(() -> new ItemNotFoundException("Budget not found"));

        if (!budget.getOrganisation().getOrganisationId().equals(organisationId)) {
            throw new ItemNotFoundException("Budget does not belong to this organisation");
        }

        if (budget.getStatus() != OrgBudgetStatus.DRAFT && budget.getStatus() != OrgBudgetStatus.APPROVED) {
            throw new ItemNotFoundException("Only draft or approved budgets can be updated");
        }

        BigDecimal totalDistributionAmount = request.getAccountDistributions().stream()
                .map(DistributeBudgetRequest.AccountDistribution::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (totalDistributionAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ItemNotFoundException("Total distribution amount must be greater than zero");
        }

        for (DistributeBudgetRequest.AccountDistribution distribution : request.getAccountDistributions()) {
            ChartOfAccounts account = chartOfAccountsRepo.findById(distribution.getAccountId())
                    .orElseThrow(() -> new ItemNotFoundException("Chart of account not found: " + distribution.getAccountId()));

            validateHeaderAccountForDistribution(account, organisationId);

            OrgBudgetLineItemEntity lineItem = orgBudgetLineItemRepo
                    .findByOrgBudgetAndChartOfAccount(budget, account)
                    .orElseGet(() -> createNewLineItem(budget, account, authenticatedAccount));

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

        budget.getLineItems().size(); // Force lazy loading
        return budget;
    }

    @Override
    public OrgBudgetSummaryResponse getBudgetSummary(UUID budgetId, UUID organisationId) throws ItemNotFoundException {

        OrgBudgetEntity budget = getBudgetWithAccounts(budgetId, organisationId);

        BigDecimal totalAllocatedToDetails = calculateTotalAllocatedToDetails(budget);
        BigDecimal totalSpentFromAllocations = calculateTotalSpentFromAllocations(budget);
        BigDecimal totalCommittedFromAllocations = calculateTotalCommittedFromAllocations(budget);

        OrgBudgetSummaryResponse summary = new OrgBudgetSummaryResponse();
        summary.setBudgetId(budget.getBudgetId());
        summary.setBudgetName(budget.getBudgetName());
        summary.setTotalBudgetAmount(budget.getDistributedAmount());
        summary.setDistributedAmount(budget.getDistributedAmount());
        summary.setTotalAllocatedToDetails(totalAllocatedToDetails);
        summary.setAvailableForAllocation(budget.getDistributedAmount().subtract(totalAllocatedToDetails));
        summary.setTotalSpentAmount(totalSpentFromAllocations);
        summary.setTotalCommittedAmount(totalCommittedFromAllocations);
        summary.setTotalRemainingAmount(totalAllocatedToDetails.subtract(totalSpentFromAllocations).subtract(totalCommittedFromAllocations));
        summary.setStatus(budget.getStatus());
        summary.setFinancialYearStart(budget.getFinancialYearStart());
        summary.setFinancialYearEnd(budget.getFinancialYearEnd());

        // Calculate percentages
        if (budget.getDistributedAmount().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal allocationPercentage = totalAllocatedToDetails
                    .multiply(BigDecimal.valueOf(100))
                    .divide(budget.getDistributedAmount(), 2, RoundingMode.HALF_UP);
            summary.setBudgetUtilizationPercentage(allocationPercentage);

            if (totalAllocatedToDetails.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal spendingPercentage = totalSpentFromAllocations
                        .multiply(BigDecimal.valueOf(100))
                        .divide(totalAllocatedToDetails, 2, RoundingMode.HALF_UP);
                summary.setSpendingPercentage(spendingPercentage);
            } else {
                summary.setSpendingPercentage(BigDecimal.ZERO);
            }
        } else {
            summary.setBudgetUtilizationPercentage(BigDecimal.ZERO);
            summary.setSpendingPercentage(BigDecimal.ZERO);
        }

        // Account statistics
        summary.setTotalAccounts(budget.getLineItems().size());
        summary.setAccountsWithBudget((int) budget.getLineItemsWithBudgetCount());
        summary.setAccountsWithoutBudget((int) budget.getLineItemsWithoutBudgetCount());

        return summary;
    }

    @Override
    public void activateBudget(UUID budgetId, UUID organisationId) throws ItemNotFoundException {

        AccountEntity authenticatedAccount = getAuthenticatedAccount();
        OrganisationEntity organisation = organisationRepo.findById(organisationId)
                .orElseThrow(() -> new ItemNotFoundException("Organisation does not exist"));

        validateMemberPermissions(authenticatedAccount, organisation, Arrays.asList(MemberRole.OWNER, MemberRole.ADMIN));

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

        validateMemberPermissions(authenticatedAccount, organisation, Arrays.asList(MemberRole.OWNER, MemberRole.ADMIN));

        OrgBudgetEntity budgetToUpdate = orgBudgetRepo.findById(budgetId)
                .orElseThrow(() -> new ItemNotFoundException("Budget not found"));

        if (!budgetToUpdate.getOrganisation().getOrganisationId().equals(organisationId)) {
            throw new ItemNotFoundException("Budget does not belong to this organisation");
        }

        if (budgetToUpdate.getStatus() != OrgBudgetStatus.DRAFT) {
            throw new ItemNotFoundException("Only draft budgets can be updated");
        }

        if (request.getFinancialYearStart() != null) {
            budgetToUpdate.setFinancialYearStart(request.getFinancialYearStart());
        }

        if (request.getFinancialYearEnd() != null) {
            budgetToUpdate.setFinancialYearEnd(request.getFinancialYearEnd());
        }

        if (request.getDescription() != null) {
            budgetToUpdate.setDescription(request.getDescription());
        }

        if (request.getFinancialYearStart() != null || request.getFinancialYearEnd() != null) {
            String newBudgetName = generateBudgetName(
                    budgetToUpdate.getFinancialYearStart(),
                    budgetToUpdate.getFinancialYearEnd()
            );
            budgetToUpdate.setBudgetName(newBudgetName);
        }

        budgetToUpdate.setModifiedBy(authenticatedAccount.getAccountId());
        budgetToUpdate.setModifiedDate(LocalDateTime.now());

        return orgBudgetRepo.save(budgetToUpdate);
    }


    @Override
    public BudgetHierarchyWithAllocationsResponse getBudgetHierarchyWithAllocations(
            UUID budgetId, UUID organisationId) throws ItemNotFoundException {

        AccountEntity authenticatedAccount = getAuthenticatedAccount();
        OrganisationEntity organisation = organisationRepo.findById(organisationId)
                .orElseThrow(() -> new ItemNotFoundException("Organisation not found"));

        validateMemberPermissions(authenticatedAccount, organisation,
                Arrays.asList(MemberRole.OWNER, MemberRole.ADMIN, MemberRole.MEMBER));

        OrgBudgetEntity budget;

        if (budgetId != null) {

            budget = validateBudget(budgetId, organisationId);

        } else {
            budget = orgBudgetRepo.findByOrganisationAndStatus(organisation, OrgBudgetStatus.ACTIVE).orElseThrow(
                    () -> new ItemNotFoundException("There is no active budget for this organisation")
            );
        }


        List<ChartOfAccounts> allExpenseHeaderAccounts = chartOfAccountsRepo
                .findByOrganisationAndAccountTypeAndIsActive(organisation, AccountType.EXPENSE, true)
                .stream()
                .filter(ChartOfAccounts::getIsHeader)
                .toList();

        List<ChartOfAccounts> allExpenseDetailAccounts = chartOfAccountsRepo
                .findByOrganisationAndAccountTypeAndIsActive(organisation, AccountType.EXPENSE, true)
                .stream()
                .filter(account -> !account.getIsHeader())
                .collect(Collectors.toList());

        Map<UUID, OrgBudgetLineItemEntity> budgetLineItemsByHeaderId = budget.getLineItems().stream()
                .collect(Collectors.toMap(
                        lineItem -> lineItem.getChartOfAccount().getId(),
                        Function.identity()));

        Map<UUID, List<OrgBudgetDetailAllocationEntity>> allocationsByDetailAccount =
                budget.getLineItems().stream()
                        .flatMap(headerLineItem -> allocationRepo.findByHeaderLineItem(headerLineItem).stream())
                        .collect(Collectors.groupingBy(allocation -> allocation.getDetailAccount().getId()));

        List<BudgetHierarchyWithAllocationsResponse.HeaderAccountWithDetails> headerAccounts =
                allExpenseHeaderAccounts.stream()
                        .map(headerAccount -> buildHeaderWithDetails(
                                headerAccount, budgetLineItemsByHeaderId.get(headerAccount.getId()),
                                allExpenseDetailAccounts, allocationsByDetailAccount))
                        .collect(Collectors.toList());

        // Calculate budget-level totals
        BigDecimal totalAllocatedToDetails = calculateTotalAllocatedToDetails(budget);
        BigDecimal totalSpentFromAllocations = calculateTotalSpentFromAllocations(budget);
        BigDecimal totalCommittedFromAllocations = calculateTotalCommittedFromAllocations(budget);
        BigDecimal totalRemainingFromAllocations = totalAllocatedToDetails
                .subtract(totalSpentFromAllocations)
                .subtract(totalCommittedFromAllocations);

        // Build summary statistics
        int totalDetailAccounts = allExpenseDetailAccounts.size();
        int detailsWithAllocation = (int) allocationsByDetailAccount.entrySet().stream()
                .filter(entry -> !entry.getValue().isEmpty())
                .filter(entry -> entry.getValue().stream().anyMatch(OrgBudgetDetailAllocationEntity::hasAllocation))
                .count();


        // Create response
        BudgetHierarchyWithAllocationsResponse response = new BudgetHierarchyWithAllocationsResponse();

        // Budget summary
        response.setBudgetId(budget.getBudgetId());
        response.setBudgetName(budget.getBudgetName());
        response.setFinancialYearStart(budget.getFinancialYearStart());
        response.setFinancialYearEnd(budget.getFinancialYearEnd());
        response.setBudgetStatus(budget.getStatus());
        response.setCreatedAt(budget.getCreatedDate());

        // Budget totals
        response.setTotalBudgetAmount(budget.getDistributedAmount());
        response.setTotalAllocatedToDetails(totalAllocatedToDetails);
        response.setTotalSpentAmount(totalSpentFromAllocations);
        response.setTotalCommittedAmount(totalCommittedFromAllocations);
        response.setTotalRemainingAmount(totalRemainingFromAllocations);
        response.setAvailableForAllocation(budget.getDistributedAmount().subtract(totalAllocatedToDetails));

        // Summary statistics
        response.setBudgetUtilizationPercentage(calculateBudgetUtilizationPercentage(
                budget.getDistributedAmount(), totalAllocatedToDetails));
        response.setSpendingPercentage(calculateSpendingPercentage(
                totalAllocatedToDetails, totalSpentFromAllocations));
        response.setTotalHeaderAccounts(allExpenseHeaderAccounts.size());
        response.setHeadersWithBudget((int) budget.getLineItems().size());
        response.setHeadersWithoutBudget(allExpenseHeaderAccounts.size() - budget.getLineItems().size());
        response.setTotalDetailAccounts(totalDetailAccounts);
        response.setDetailsWithAllocation(detailsWithAllocation);
        response.setDetailsWithoutAllocation(totalDetailAccounts - detailsWithAllocation);

        // Hierarchy data
        response.setHeaderAccounts(headerAccounts);

        return response;
    }

    // ========== ALLOCATION CALCULATION METHODS ==========

    private BigDecimal calculateTotalAllocatedToDetails(OrgBudgetEntity budget) {
        return budget.getLineItems().stream()
                .map(this::calculateAllocatedAmountForHeader)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal calculateTotalSpentFromAllocations(OrgBudgetEntity budget) {
        return budget.getLineItems().stream()
                .flatMap(headerLineItem -> allocationRepo.findByHeaderLineItem(headerLineItem).stream())
                .map(OrgBudgetDetailAllocationEntity::getSpentAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal calculateTotalCommittedFromAllocations(OrgBudgetEntity budget) {
        return budget.getLineItems().stream()
                .flatMap(headerLineItem -> allocationRepo.findByHeaderLineItem(headerLineItem).stream())
                .map(OrgBudgetDetailAllocationEntity::getCommittedAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal calculateAllocatedAmountForHeader(OrgBudgetLineItemEntity headerLineItem) {
        return allocationRepo.findByHeaderLineItem(headerLineItem).stream()
                .map(OrgBudgetDetailAllocationEntity::getAllocatedAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal calculateAvailableForAllocation(OrgBudgetLineItemEntity headerLineItem) {
        BigDecimal allocated = calculateAllocatedAmountForHeader(headerLineItem);
        return headerLineItem.getBudgetAmount().subtract(allocated);
    }

    private BudgetHierarchyWithAllocationsResponse.HeaderAccountWithDetails.DetailAccountAllocation buildDetailAccountAllocation(
            ChartOfAccounts detailAccount,
            List<OrgBudgetDetailAllocationEntity> allocations) {

        BudgetHierarchyWithAllocationsResponse.HeaderAccountWithDetails.DetailAccountAllocation detail =
                new BudgetHierarchyWithAllocationsResponse.HeaderAccountWithDetails.DetailAccountAllocation();

        detail.setDetailAccountId(detailAccount.getId());
        detail.setDetailAccountCode(detailAccount.getAccountCode());
        detail.setDetailAccountName(detailAccount.getName());
        detail.setDetailDescription(detailAccount.getDescription());

        if (allocations == null || allocations.isEmpty()) {
            detail.setAllocationId(null);
            detail.setAllocatedAmount(BigDecimal.ZERO);
            detail.setSpentAmount(BigDecimal.ZERO);
            detail.setCommittedAmount(BigDecimal.ZERO);
            detail.setBudgetRemaining(BigDecimal.ZERO);
            detail.setAllocationStatus("No Allocation");
            detail.setHasAllocation(false);
            detail.setNotes("No budget allocated to this account");
            detail.setUtilizationPercentage(BigDecimal.ZERO);
        } else {
            // Aggregate multiple allocations for the same detail account
            BigDecimal totalAllocated = allocations.stream()
                    .map(OrgBudgetDetailAllocationEntity::getAllocatedAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal totalSpent = allocations.stream()
                    .map(OrgBudgetDetailAllocationEntity::getSpentAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal totalCommitted = allocations.stream()
                    .map(OrgBudgetDetailAllocationEntity::getCommittedAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal budgetRemaining = totalAllocated.subtract(totalSpent).subtract(totalCommitted);

            OrgBudgetDetailAllocationEntity firstAllocation = allocations.get(0);

            detail.setAllocationId(firstAllocation.getAllocationId());
            detail.setAllocatedAmount(totalAllocated);
            detail.setSpentAmount(totalSpent);
            detail.setCommittedAmount(totalCommitted);
            detail.setBudgetRemaining(budgetRemaining);
            detail.setHasAllocation(true);
            detail.setUtilizationPercentage(calculateUtilizationPercentage(totalAllocated, totalSpent.add(totalCommitted)));
            detail.setAllocationCreatedDate(firstAllocation.getCreatedDate());
            detail.setAllocationModifiedDate(firstAllocation.getModifiedDate());

            if (budgetRemaining.compareTo(BigDecimal.ZERO) <= 0) {
                detail.setAllocationStatus("Fully Used");
            } else {
                detail.setAllocationStatus("Available");
            }

            if (allocations.size() == 1) {
                detail.setNotes(firstAllocation.getAllocationNotes() != null ?
                        firstAllocation.getAllocationNotes() : "Available for vouchers");
            } else {
                detail.setNotes(String.format("Aggregated from %d allocations - Total available", allocations.size()));
            }
        }

        return detail;
    }


    private BudgetHierarchyWithAllocationsResponse.HeaderAccountWithDetails buildHeaderWithDetails(
            ChartOfAccounts headerAccount,
            OrgBudgetLineItemEntity budgetLineItem,
            List<ChartOfAccounts> allDetailAccounts,
            Map<UUID, List<OrgBudgetDetailAllocationEntity>> allocationsByDetailAccount) {

        // Get detail accounts under this header
        List<ChartOfAccounts> detailAccountsForHeader = allDetailAccounts.stream()
                .filter(account -> headerAccount.getId().equals(account.getParentAccountId()))
                .toList();

        // Build detail account allocation responses
        List<BudgetHierarchyWithAllocationsResponse.HeaderAccountWithDetails.DetailAccountAllocation> detailAllocations =
                detailAccountsForHeader.stream()
                        .map(detailAccount -> buildDetailAccountAllocation(
                                detailAccount, allocationsByDetailAccount.get(detailAccount.getId())))
                        .collect(Collectors.toList());

        // Calculate header totals from detail allocations
        BigDecimal headerAllocatedToDetails = detailAllocations.stream()
                .map(BudgetHierarchyWithAllocationsResponse.HeaderAccountWithDetails.DetailAccountAllocation::getAllocatedAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal headerSpentAmount = detailAllocations.stream()
                .map(BudgetHierarchyWithAllocationsResponse.HeaderAccountWithDetails.DetailAccountAllocation::getSpentAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal headerCommittedAmount = detailAllocations.stream()
                .map(BudgetHierarchyWithAllocationsResponse.HeaderAccountWithDetails.DetailAccountAllocation::getCommittedAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Determine header budget amount (zero if no budget line item exists)
        BigDecimal headerBudgetAmount = budgetLineItem != null ? budgetLineItem.getBudgetAmount() : BigDecimal.ZERO;  // ← Added null check

        // Build header response
        BudgetHierarchyWithAllocationsResponse.HeaderAccountWithDetails header =
                new BudgetHierarchyWithAllocationsResponse.HeaderAccountWithDetails();

        // ← Changed: Use headerAccount and budgetLineItem with null checks
        header.setHeaderLineItemId(budgetLineItem != null ? budgetLineItem.getLineItemId() : null);
        header.setHeaderAccountId(headerAccount.getId());
        header.setHeaderAccountCode(headerAccount.getAccountCode());
        header.setHeaderAccountName(headerAccount.getName());
        header.setHeaderDescription(headerAccount.getDescription());

        header.setHeaderBudgetAmount(headerBudgetAmount);  // ← Changed: Use calculated amount
        header.setHeaderAllocatedToDetails(headerAllocatedToDetails);
        header.setHeaderAvailableForAllocation(headerBudgetAmount.subtract(headerAllocatedToDetails));  // ← Changed
        header.setHeaderSpentAmount(headerSpentAmount);
        header.setHeaderCommittedAmount(headerCommittedAmount);
        header.setHeaderRemainingAmount(headerAllocatedToDetails.subtract(headerSpentAmount).subtract(headerCommittedAmount));

        header.setHeaderUtilizationPercentage(calculateUtilizationPercentage(
                headerAllocatedToDetails, headerSpentAmount.add(headerCommittedAmount)));
        header.setHeaderAllocationPercentage(calculateAllocationPercentage(
                headerBudgetAmount, headerAllocatedToDetails));  // ← Changed
        header.setHasBudgetAllocated(budgetLineItem != null && budgetLineItem.hasBudgetAllocated());  // ← Added null check
        header.setDetailAccountCount(detailAccountsForHeader.size());
        header.setDetailsWithAllocation((int) detailAllocations.stream()
                .filter(BudgetHierarchyWithAllocationsResponse.HeaderAccountWithDetails.DetailAccountAllocation::isHasAllocation)
                .count());
        header.setDetailsWithoutAllocation(detailAccountsForHeader.size() - header.getDetailsWithAllocation());

        header.setDetailAccounts(detailAllocations);

        return header;
    }

    // ========== HELPER METHODS ==========

    private void validateHeaderAccountForDistribution(ChartOfAccounts account, UUID organisationId)
            throws ItemNotFoundException {

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
    }


    private OrgBudgetLineItemEntity createNewLineItem(OrgBudgetEntity budget, ChartOfAccounts account,
                                                      AccountEntity authenticatedAccount) {
        OrgBudgetLineItemEntity newLineItem = new OrgBudgetLineItemEntity();
        newLineItem.setOrgBudget(budget);
        newLineItem.setChartOfAccount(account);
        newLineItem.setSpentAmount(BigDecimal.ZERO);
        newLineItem.setCommittedAmount(BigDecimal.ZERO);
        newLineItem.setCreatedDate(LocalDateTime.now());
        newLineItem.setCreatedBy(authenticatedAccount.getAccountId());
        return newLineItem;
    }

    private String generateBudgetName(LocalDate startDate, LocalDate endDate) {
        String startMonth = startDate.getMonth().name().substring(0, 3);
        String endMonth = endDate.getMonth().name().substring(0, 3);
        int startYear = startDate.getYear() % 100;
        int endYear = endDate.getYear() % 100;

        return String.format("FY %s%02d-%s%02d", startMonth, startYear, endMonth, endYear);
    }

    private void validateMemberPermissions(AccountEntity account, OrganisationEntity organisation,
                                           List<MemberRole> allowedRoles) throws ItemNotFoundException {

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

    private void validateNoOverlappingFinancialYear(OrganisationEntity organisation,
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


    private BigDecimal calculateBudgetUtilizationPercentage(BigDecimal totalBudget, BigDecimal totalAllocated) {
        if (totalBudget.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return totalAllocated.multiply(BigDecimal.valueOf(100))
                .divide(totalBudget, 2, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateSpendingPercentage(BigDecimal totalAllocated, BigDecimal totalSpent) {
        if (totalAllocated.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return totalSpent.multiply(BigDecimal.valueOf(100))
                .divide(totalAllocated, 2, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateUtilizationPercentage(BigDecimal allocated, BigDecimal used) {
        if (allocated.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return used.multiply(BigDecimal.valueOf(100))
                .divide(allocated, 2, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateAllocationPercentage(BigDecimal headerBudget, BigDecimal allocated) {
        if (headerBudget.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return allocated.multiply(BigDecimal.valueOf(100))
                .divide(headerBudget, 2, RoundingMode.HALF_UP);
    }

    private OrgBudgetEntity validateBudget(UUID budgetId, UUID organisationId) throws ItemNotFoundException {
        OrgBudgetEntity budget = orgBudgetRepo.findById(budgetId)
                .orElseThrow(() -> new ItemNotFoundException("Budget not found"));

        if (!budget.getOrganisation().getOrganisationId().equals(organisationId)) {
            throw new ItemNotFoundException("Budget does not belong to this organisation");
        }

        return budget;
    }

}