package com.qbitspark.buildwisebackend.accounting_service.budget_mng.org_budget.service.impl;

import com.qbitspark.buildwisebackend.accounting_service.budget_mng.org_budget.entity.OrgBudgetDetailAllocationEntity;
import com.qbitspark.buildwisebackend.accounting_service.budget_mng.org_budget.entity.OrgBudgetEntity;
import com.qbitspark.buildwisebackend.accounting_service.budget_mng.org_budget.entity.OrgBudgetLineItemEntity;
import com.qbitspark.buildwisebackend.accounting_service.budget_mng.org_budget.paylaods.AllocateMoneyRequest;
import com.qbitspark.buildwisebackend.accounting_service.budget_mng.org_budget.paylaods.AllocationSummaryResponse;
import com.qbitspark.buildwisebackend.accounting_service.budget_mng.org_budget.repo.OrgBudgetDetailAllocationRepo;
import com.qbitspark.buildwisebackend.accounting_service.budget_mng.org_budget.repo.OrgBudgetLineItemRepo;
import com.qbitspark.buildwisebackend.accounting_service.budget_mng.org_budget.repo.OrgBudgetRepo;
import com.qbitspark.buildwisebackend.accounting_service.budget_mng.org_budget.service.OrgBudgetAllocationService;
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
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class OrgBudgetAllocationServiceImpl implements OrgBudgetAllocationService {

    private final OrgBudgetRepo orgBudgetRepo;
    private final OrgBudgetLineItemRepo orgBudgetLineItemRepo;
    private final OrgBudgetDetailAllocationRepo allocationRepo;
    private final ChartOfAccountsRepo chartOfAccountsRepo;
    private final OrganisationRepo organisationRepo;
    private final OrganisationMemberRepo organisationMemberRepo;
    private final AccountRepo accountRepo;

    @Override
    public List<OrgBudgetDetailAllocationEntity> allocateMoneyToDetailAccounts(
            UUID organisationId, UUID budgetId, AllocateMoneyRequest request)
            throws ItemNotFoundException {

        AccountEntity authenticatedAccount = getAuthenticatedAccount();
        validateOrganisationAccess(organisationId, authenticatedAccount,
                Arrays.asList(MemberRole.OWNER, MemberRole.ADMIN));

        OrgBudgetEntity budget = validateBudget(budgetId, organisationId);

        Map<UUID, List<AllocateMoneyRequest.DetailAllocation>> allocationsByHeader =
                groupAllocationsByHeader(request.getDetailAllocations(), organisationId);

        List<OrgBudgetDetailAllocationEntity> allNewAllocations = new ArrayList<>();
        for (Map.Entry<UUID, List<AllocateMoneyRequest.DetailAllocation>> entry : allocationsByHeader.entrySet()) {
            List<OrgBudgetDetailAllocationEntity> headerAllocations = processHeaderAllocations(
                    entry.getKey(), entry.getValue(), budget, authenticatedAccount);
            allNewAllocations.addAll(headerAllocations);
        }

        return allNewAllocations;
    }

    @Override
    public List<OrgBudgetDetailAllocationEntity> getHeaderAllocations(
            UUID organisationId, UUID budgetId, UUID headerLineItemId) throws ItemNotFoundException {

        AccountEntity authenticatedAccount = getAuthenticatedAccount();
        validateOrganisationAccess(organisationId, authenticatedAccount,
                Arrays.asList(MemberRole.OWNER, MemberRole.ADMIN, MemberRole.MEMBER));

        OrgBudgetLineItemEntity headerLineItem = validateHeaderLineItem(headerLineItemId, organisationId);
        return allocationRepo.findByHeaderLineItemOrderByDetailAccountAccountCode(headerLineItem);
    }

    @Override
    public AllocationSummaryResponse getAllocationSummary(
            UUID organisationId, UUID budgetId, UUID headerLineItemId) throws ItemNotFoundException {

        List<OrgBudgetDetailAllocationEntity> allocations = getHeaderAllocations(organisationId, budgetId, headerLineItemId);
        OrgBudgetLineItemEntity headerLineItem = validateHeaderLineItem(headerLineItemId, organisationId);
        return buildAllocationSummary(headerLineItem, allocations);
    }

    @Override
    public List<OrgBudgetDetailAllocationEntity> getAllBudgetAllocations(UUID organisationId, UUID budgetId)
            throws ItemNotFoundException {

        AccountEntity authenticatedAccount = getAuthenticatedAccount();
        validateOrganisationAccess(organisationId, authenticatedAccount,
                Arrays.asList(MemberRole.OWNER, MemberRole.ADMIN, MemberRole.MEMBER));

        OrgBudgetEntity budget = validateBudget(budgetId, organisationId);

        return budget.getLineItems().stream()
                .flatMap(headerLineItem ->
                        allocationRepo.findByHeaderLineItemOrderByDetailAccountAccountCode(headerLineItem).stream())
                .collect(Collectors.toList());
    }

    // ========== BUSINESS LOGIC METHODS ==========

    private List<OrgBudgetDetailAllocationEntity> processHeaderAllocations(
            UUID headerAccountId, List<AllocateMoneyRequest.DetailAllocation> allocations,
            OrgBudgetEntity budget, AccountEntity authenticatedAccount) throws ItemNotFoundException {

        OrgBudgetLineItemEntity headerLineItem = getAndValidateHeaderLineItem(headerAccountId, budget);

        BigDecimal totalAllocationAmount = allocations.stream()
                .map(AllocateMoneyRequest.DetailAllocation::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        validateBudgetAvailability(headerLineItem, totalAllocationAmount);

        List<OrgBudgetDetailAllocationEntity> newAllocations = new ArrayList<>();
        for (AllocateMoneyRequest.DetailAllocation allocation : allocations) {
            OrgBudgetDetailAllocationEntity newAllocation = createAllocation(
                    headerLineItem, allocation, authenticatedAccount);
            newAllocations.add(allocationRepo.save(newAllocation));
        }

        return newAllocations;
    }

    private OrgBudgetLineItemEntity getAndValidateHeaderLineItem(UUID headerAccountId, OrgBudgetEntity budget)
            throws ItemNotFoundException {

        ChartOfAccounts headerAccount = chartOfAccountsRepo.findById(headerAccountId)
                .orElseThrow(() -> new ItemNotFoundException("Header account not found"));

        if (!headerAccount.getIsHeader()) {
            throw new ItemNotFoundException("Account '" + headerAccount.getName() + "' is not a header account");
        }

        if (headerAccount.getAccountType() != AccountType.EXPENSE) {
            throw new ItemNotFoundException("Budget allocation only works with expense accounts");
        }

        OrgBudgetLineItemEntity headerLineItem = orgBudgetLineItemRepo
                .findByOrgBudgetAndChartOfAccount(budget, headerAccount)
                .orElseThrow(() -> new ItemNotFoundException(
                        "Header account '" + headerAccount.getName() + "' not found in budget. " +
                                "Please distribute budget to this header account first."
                ));

        if (headerLineItem.getBudgetAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ItemNotFoundException(
                    "Header account '" + headerAccount.getName() + "' has no budget allocated. " +
                            "Please distribute budget to this header account first."
            );
        }

        return headerLineItem;
    }

    private void validateBudgetAvailability(OrgBudgetLineItemEntity headerLineItem, BigDecimal requestedAmount)
            throws ItemNotFoundException {

        BigDecimal headerBudgetAmount = headerLineItem.getBudgetAmount();
        BigDecimal currentlyAllocated = getCurrentlyAllocatedAmount(headerLineItem);
        BigDecimal availableForAllocation = headerBudgetAmount.subtract(currentlyAllocated);

        if (requestedAmount.compareTo(availableForAllocation) > 0) {
            throw new ItemNotFoundException(String.format(
                    "Allocation amount (%s) exceeds available budget for '%s' (%s). " +
                            "Total Budget: %s, Already Allocated: %s, Available: %s",
                    requestedAmount,
                    headerLineItem.getChartOfAccount().getName(),
                    availableForAllocation,
                    headerBudgetAmount,
                    currentlyAllocated,
                    availableForAllocation
            ));
        }
    }

    private BigDecimal getCurrentlyAllocatedAmount(OrgBudgetLineItemEntity headerLineItem) {
        return allocationRepo.findByHeaderLineItem(headerLineItem).stream()
                .map(OrgBudgetDetailAllocationEntity::getAllocatedAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private Map<UUID, List<AllocateMoneyRequest.DetailAllocation>> groupAllocationsByHeader(
            List<AllocateMoneyRequest.DetailAllocation> detailAllocations, UUID organisationId)
            throws ItemNotFoundException {

        Map<UUID, List<AllocateMoneyRequest.DetailAllocation>> groupedAllocations = new HashMap<>();

        for (AllocateMoneyRequest.DetailAllocation allocation : detailAllocations) {
            ChartOfAccounts detailAccount = chartOfAccountsRepo.findById(allocation.getDetailAccountId())
                    .orElseThrow(() -> new ItemNotFoundException("Detail account not found: " + allocation.getDetailAccountId()));

            validateDetailAccount(detailAccount, organisationId);

            UUID headerAccountId = detailAccount.getParentAccountId();
            if (headerAccountId == null) {
                throw new ItemNotFoundException("Detail account '" + detailAccount.getName() + "' has no parent header account");
            }

            groupedAllocations.computeIfAbsent(headerAccountId, k -> new ArrayList<>()).add(allocation);
        }

        return groupedAllocations;
    }

    private OrgBudgetDetailAllocationEntity createAllocation(OrgBudgetLineItemEntity headerLineItem,
                                                             AllocateMoneyRequest.DetailAllocation detailAllocation,
                                                             AccountEntity authenticatedAccount)
            throws ItemNotFoundException {

        ChartOfAccounts detailAccount = chartOfAccountsRepo.findById(detailAllocation.getDetailAccountId())
                .orElseThrow(() -> new ItemNotFoundException("Detail account not found"));

        OrgBudgetDetailAllocationEntity allocation = new OrgBudgetDetailAllocationEntity();
        allocation.setHeaderLineItem(headerLineItem);
        allocation.setDetailAccount(detailAccount);
        allocation.setAllocatedAmount(detailAllocation.getAmount());
        allocation.setSpentAmount(BigDecimal.ZERO);
        allocation.setCommittedAmount(BigDecimal.ZERO);
        allocation.setAllocationNotes(detailAllocation.getDescription());
        allocation.setCreatedDate(LocalDateTime.now());
        allocation.setCreatedBy(authenticatedAccount.getAccountId());

        return allocation;
    }

    private AllocationSummaryResponse buildAllocationSummary(OrgBudgetLineItemEntity headerLineItem,
                                                             List<OrgBudgetDetailAllocationEntity> allocations) {

        AllocationSummaryResponse summary = new AllocationSummaryResponse();
        summary.setHeaderLineItemId(headerLineItem.getLineItemId());
        summary.setHeaderAccountCode(headerLineItem.getChartOfAccount().getAccountCode());
        summary.setHeaderAccountName(headerLineItem.getChartOfAccount().getName());
        summary.setHeaderBudgetAmount(headerLineItem.getBudgetAmount());

        BigDecimal totalAllocated = allocations.stream()
                .map(OrgBudgetDetailAllocationEntity::getAllocatedAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalSpent = allocations.stream()
                .map(OrgBudgetDetailAllocationEntity::getSpentAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalCommitted = allocations.stream()
                .map(OrgBudgetDetailAllocationEntity::getCommittedAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        summary.setTotalAllocatedAmount(totalAllocated);
        summary.setTotalSpentAmount(totalSpent);
        summary.setTotalCommittedAmount(totalCommitted);
        summary.setAvailableForAllocation(headerLineItem.getBudgetAmount().subtract(totalAllocated));
        summary.setTotalRemainingAmount(totalAllocated.subtract(totalSpent).subtract(totalCommitted));

        summary.setTotalDetailAccounts(allocations.size());
        summary.setAccountsWithAllocation((int) allocations.stream().filter(OrgBudgetDetailAllocationEntity::hasAllocation).count());
        summary.setAccountsWithoutAllocation(allocations.size() - summary.getAccountsWithAllocation());

        return summary;
    }

    // ========== VALIDATION METHODS ==========

    private void validateOrganisationAccess(UUID organisationId, AccountEntity account,
                                            List<MemberRole> allowedRoles) throws ItemNotFoundException {

        OrganisationEntity organisation = organisationRepo.findById(organisationId)
                .orElseThrow(() -> new ItemNotFoundException("Organisation not found"));

        OrganisationMember member = organisationMemberRepo.findByAccountAndOrganisation(account, organisation)
                .orElseThrow(() -> new ItemNotFoundException("Member not found in organisation"));

        if (member.getStatus() != MemberStatus.ACTIVE) {
            throw new ItemNotFoundException("Member is not active");
        }

        if (!allowedRoles.contains(member.getRole())) {
            throw new ItemNotFoundException("Insufficient permissions");
        }

    }

    private OrgBudgetEntity validateBudget(UUID budgetId, UUID organisationId) throws ItemNotFoundException {
        OrgBudgetEntity budget = orgBudgetRepo.findById(budgetId)
                .orElseThrow(() -> new ItemNotFoundException("Budget not found"));

        if (!budget.getOrganisation().getOrganisationId().equals(organisationId)) {
            throw new ItemNotFoundException("Budget does not belong to this organisation");
        }

        return budget;
    }

    private OrgBudgetLineItemEntity validateHeaderLineItem(UUID headerLineItemId, UUID organisationId)
            throws ItemNotFoundException {

        OrgBudgetLineItemEntity headerLineItem = orgBudgetLineItemRepo.findById(headerLineItemId)
                .orElseThrow(() -> new ItemNotFoundException("Header line item not found"));

        if (!headerLineItem.getOrgBudget().getOrganisation().getOrganisationId().equals(organisationId)) {
            throw new ItemNotFoundException("Header line item does not belong to this organisation");
        }

        return headerLineItem;
    }

    private void validateDetailAccount(ChartOfAccounts detailAccount, UUID organisationId)
            throws ItemNotFoundException {

        if (!detailAccount.getOrganisation().getOrganisationId().equals(organisationId)) {
            throw new ItemNotFoundException("Detail account does not belong to this organisation");
        }

        if (detailAccount.getIsHeader()) {
            throw new ItemNotFoundException("Cannot allocate to header account: " + detailAccount.getName());
        }

        if (detailAccount.getAccountType() != AccountType.EXPENSE) {
            throw new ItemNotFoundException("Can only allocate to expense accounts: " + detailAccount.getName());
        }

        if (!detailAccount.getIsPostable()) {
            throw new ItemNotFoundException("Cannot allocate to non-postable account: " + detailAccount.getName());
        }
    }

    private AccountEntity getAuthenticatedAccount() throws ItemNotFoundException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ItemNotFoundException("User is not authenticated");
        }

        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        return accountRepo.findByUserName(userDetails.getUsername())
                .orElseThrow(() -> new ItemNotFoundException("User not found"));
    }
}