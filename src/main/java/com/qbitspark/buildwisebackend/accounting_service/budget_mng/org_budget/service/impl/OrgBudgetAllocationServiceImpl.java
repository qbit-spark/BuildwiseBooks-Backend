package com.qbitspark.buildwisebackend.accounting_service.budget_mng.org_budget.service.impl;

import com.qbitspark.buildwisebackend.accounting_service.budget_mng.org_budget.entity.OrgBudgetDetailAllocationEntity;
import com.qbitspark.buildwisebackend.accounting_service.budget_mng.org_budget.entity.OrgBudgetEntity;
import com.qbitspark.buildwisebackend.accounting_service.budget_mng.org_budget.entity.OrgBudgetLineItemEntity;
import com.qbitspark.buildwisebackend.accounting_service.budget_mng.org_budget.paylaods.AllocationSummaryResponse;
import com.qbitspark.buildwisebackend.accounting_service.budget_mng.org_budget.paylaods.AvailableDetailAllocationResponse;
import com.qbitspark.buildwisebackend.accounting_service.budget_mng.org_budget.repo.OrgBudgetDetailAllocationRepo;
import com.qbitspark.buildwisebackend.accounting_service.budget_mng.org_budget.repo.OrgBudgetLineItemRepo;
import com.qbitspark.buildwisebackend.accounting_service.budget_mng.org_budget.repo.OrgBudgetRepo;
import com.qbitspark.buildwisebackend.accounting_service.budget_mng.org_budget.service.OrgBudgetAllocationService;
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
import com.qbitspark.buildwisebackend.organisation_service.roles_mng.service.PermissionCheckerService;
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
    private final PermissionCheckerService permissionChecker;

    @Override
    public List<OrgBudgetDetailAllocationEntity> createBudgetAllocations(
            UUID organisationId, UUID budgetId, CreateBudgetAllocationRequest request)
            throws ItemNotFoundException, AccessDeniedException {

        AccountEntity user = getAuthenticatedAccount();
        OrganisationEntity organisation = getOrganisation(organisationId);
        OrganisationMember member = validateMemberAccess(user, organisation);
        permissionChecker.checkMemberPermission(member, "BUDGET", "allocateBudget");

        OrgBudgetEntity budget = validateBudget(budgetId, organisationId);

        Map<UUID, List<CreateBudgetAllocationRequest.DetailAllocation>> groupedByHeader =
                groupAllocationsByHeader(request.getDetailAllocations(), organisationId);

        List<OrgBudgetDetailAllocationEntity> allNewAllocations = new ArrayList<>();

        for (Map.Entry<UUID, List<CreateBudgetAllocationRequest.DetailAllocation>> entry : groupedByHeader.entrySet()) {
            List<OrgBudgetDetailAllocationEntity> headerAllocations = processHeaderAllocations(
                    entry.getKey(), entry.getValue(), budget, user);
            allNewAllocations.addAll(headerAllocations);
        }

        return allNewAllocations;
    }

    @Override
    public List<OrgBudgetDetailAllocationEntity> getHeaderAllocations(
            UUID organisationId, UUID budgetId, UUID headerLineItemId)
            throws ItemNotFoundException, AccessDeniedException {

        AccountEntity user = getAuthenticatedAccount();
        OrganisationEntity organisation = getOrganisation(organisationId);
        OrganisationMember member = validateMemberAccess(user, organisation);
        permissionChecker.checkMemberPermission(member, "BUDGET", "viewBudget");

        OrgBudgetLineItemEntity headerLineItem = validateHeaderLineItem(headerLineItemId, organisationId);
        return allocationRepo.findByHeaderLineItemOrderByDetailAccountAccountCode(headerLineItem);
    }

    @Override
    public AllocationSummaryResponse getAllocationSummary(
            UUID organisationId, UUID budgetId, UUID headerLineItemId)
            throws ItemNotFoundException, AccessDeniedException {

        AccountEntity user = getAuthenticatedAccount();
        OrganisationEntity organisation = getOrganisation(organisationId);
        OrganisationMember member = validateMemberAccess(user, organisation);
        permissionChecker.checkMemberPermission(member, "BUDGET", "viewBudget");

        List<OrgBudgetDetailAllocationEntity> allocations = getHeaderAllocations(organisationId, budgetId, headerLineItemId);
        OrgBudgetLineItemEntity headerLineItem = validateHeaderLineItem(headerLineItemId, organisationId);
        return buildAllocationSummary(headerLineItem, allocations);
    }

    @Override
    public List<OrgBudgetDetailAllocationEntity> getAllBudgetAllocations(UUID organisationId, UUID budgetId)
            throws ItemNotFoundException, AccessDeniedException {

        AccountEntity user = getAuthenticatedAccount();
        OrganisationEntity organisation = getOrganisation(organisationId);
        OrganisationMember member = validateMemberAccess(user, organisation);
        permissionChecker.checkMemberPermission(member, "BUDGET", "viewBudget");

        OrgBudgetEntity budget = validateBudget(budgetId, organisationId);

        return budget.getLineItems().stream()
                .flatMap(headerLineItem ->
                        allocationRepo.findByHeaderLineItemOrderByDetailAccountAccountCode(headerLineItem).stream())
                .collect(Collectors.toList());
    }

    @Override
    public List<AvailableDetailAllocationResponse> getDetailAccountsForVouchers(
            UUID organisationId, UUID budgetId) throws ItemNotFoundException, AccessDeniedException {

        AccountEntity user = getAuthenticatedAccount();
        OrganisationEntity organisation = getOrganisation(organisationId);
        OrganisationMember member = validateMemberAccess(user, organisation);
        permissionChecker.checkMemberPermission(member, "BUDGET", "viewBudget");

        OrgBudgetEntity budget = validateBudget(budgetId, organisationId);

        List<ChartOfAccounts> allDetailAccounts = chartOfAccountsRepo
                .findByOrganisationAndAccountTypeAndIsActiveAndIsPostable(
                        organisation, AccountType.EXPENSE, true, true)
                .stream()
                .filter(account -> !account.getIsHeader())
                .toList();

        Map<UUID, List<OrgBudgetDetailAllocationEntity>> allocationsByDetailAccount =
                budget.getLineItems().stream()
                        .flatMap(headerLineItem ->
                                allocationRepo.findByHeaderLineItem(headerLineItem).stream())
                        .collect(Collectors.groupingBy(allocation ->
                                allocation.getDetailAccount().getId()));

        return allDetailAccounts.stream()
                .map(account -> createAggregatedAllocationResponse(account,
                        allocationsByDetailAccount.get(account.getId())))
                .sorted(Comparator.comparing(AvailableDetailAllocationResponse::getHeadingParent)
                        .thenComparing(AvailableDetailAllocationResponse::getAccountCode))
                .collect(Collectors.toList());
    }

    private List<OrgBudgetDetailAllocationEntity> processHeaderAllocations(
            UUID headerAccountId, List<CreateBudgetAllocationRequest.DetailAllocation> allocations,
            OrgBudgetEntity budget, AccountEntity user) throws ItemNotFoundException {

        OrgBudgetLineItemEntity headerLineItem = getAndValidateHeaderLineItem(headerAccountId, budget);

        BigDecimal totalAllocationAmount = allocations.stream()
                .map(CreateBudgetAllocationRequest.DetailAllocation::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        validateBudgetAvailability(headerLineItem, totalAllocationAmount);

        List<OrgBudgetDetailAllocationEntity> newAllocations = new ArrayList<>();
        for (CreateBudgetAllocationRequest.DetailAllocation allocation : allocations) {
            OrgBudgetDetailAllocationEntity newAllocation = createAllocation(headerLineItem, allocation, user);
            newAllocations.add(allocationRepo.save(newAllocation));
        }

        return newAllocations;
    }

    private OrgBudgetLineItemEntity getAndValidateHeaderLineItem(UUID headerAccountId, OrgBudgetEntity budget)
            throws ItemNotFoundException {

        ChartOfAccounts headerAccount = chartOfAccountsRepo.findById(headerAccountId)
                .orElseThrow(() -> new ItemNotFoundException("Header account not found"));

        if (!headerAccount.getIsHeader()) {
            throw new ItemNotFoundException("Account is not a header account");
        }

        if (headerAccount.getAccountType() != AccountType.EXPENSE) {
            throw new ItemNotFoundException("Budget allocation only works with expense accounts");
        }

        OrgBudgetLineItemEntity headerLineItem = orgBudgetLineItemRepo
                .findByOrgBudgetAndChartOfAccount(budget, headerAccount)
                .orElseThrow(() -> new ItemNotFoundException("Header account not found in budget"));

        if (headerLineItem.getBudgetAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ItemNotFoundException("Header account has no budget allocated");
        }

        return headerLineItem;
    }

    private void validateBudgetAvailability(OrgBudgetLineItemEntity headerLineItem, BigDecimal requestedAmount)
            throws ItemNotFoundException {

        BigDecimal headerBudgetAmount = headerLineItem.getBudgetAmount();
        BigDecimal currentlyAllocated = getCurrentlyAllocatedAmount(headerLineItem);
        BigDecimal availableForAllocation = headerBudgetAmount.subtract(currentlyAllocated);

        if (requestedAmount.compareTo(availableForAllocation) > 0) {
            throw new ItemNotFoundException("Allocation amount exceeds available budget");
        }
    }

    private BigDecimal getCurrentlyAllocatedAmount(OrgBudgetLineItemEntity headerLineItem) {
        return allocationRepo.findByHeaderLineItem(headerLineItem).stream()
                .map(OrgBudgetDetailAllocationEntity::getAllocatedAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private Map<UUID, List<CreateBudgetAllocationRequest.DetailAllocation>> groupAllocationsByHeader(
            List<CreateBudgetAllocationRequest.DetailAllocation> detailAllocations, UUID organisationId)
            throws ItemNotFoundException {

        Map<UUID, List<CreateBudgetAllocationRequest.DetailAllocation>> groupedAllocations = new HashMap<>();

        for (CreateBudgetAllocationRequest.DetailAllocation allocation : detailAllocations) {
            ChartOfAccounts detailAccount = chartOfAccountsRepo.findById(allocation.getDetailAccountId())
                    .orElseThrow(() -> new ItemNotFoundException("Detail account not found"));

            validateDetailAccount(detailAccount, organisationId);

            UUID headerAccountId = detailAccount.getParentAccountId();
            if (headerAccountId == null) {
                throw new ItemNotFoundException("Detail account has no parent header account");
            }

            groupedAllocations.computeIfAbsent(headerAccountId, k -> new ArrayList<>()).add(allocation);
        }

        return groupedAllocations;
    }

    private OrgBudgetDetailAllocationEntity createAllocation(OrgBudgetLineItemEntity headerLineItem,
                                                             CreateBudgetAllocationRequest.DetailAllocation detailAllocation,
                                                             AccountEntity user) throws ItemNotFoundException {

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
        allocation.setCreatedBy(user.getAccountId());

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

        BigDecimal totalReceiptFunding = allocations.stream()
                .map(OrgBudgetDetailAllocationEntity::getTotalApprovedReceiptAllocations)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        summary.setTotalAllocatedAmount(totalAllocated);
        summary.setTotalSpentAmount(totalSpent);
        summary.setTotalCommittedAmount(totalCommitted);
        summary.setTotalReceiptFunding(totalReceiptFunding);
        summary.setAvailableForAllocation(headerLineItem.getBudgetAmount().subtract(totalAllocated));
        summary.setTotalRemainingAmount(totalAllocated.subtract(totalSpent).subtract(totalCommitted));
        summary.setUnfundedAmount(totalAllocated.subtract(totalReceiptFunding));

        summary.setTotalDetailAccounts(allocations.size());
        summary.setAccountsWithAllocation((int) allocations.stream().filter(OrgBudgetDetailAllocationEntity::hasAllocation).count());
        summary.setAccountsWithoutAllocation(allocations.size() - summary.getAccountsWithAllocation());

        return summary;
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
            throw new ItemNotFoundException("Cannot allocate to header account");
        }

        if (detailAccount.getAccountType() != AccountType.EXPENSE) {
            throw new ItemNotFoundException("Can only allocate to expense accounts");
        }

        if (!detailAccount.getIsPostable()) {
            throw new ItemNotFoundException("Cannot allocate to non-postable account");
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

    private OrganisationEntity getOrganisation(UUID organisationId) throws ItemNotFoundException {
        return organisationRepo.findById(organisationId)
                .orElseThrow(() -> new ItemNotFoundException("Organisation not found"));
    }

    private OrganisationMember validateMemberAccess(AccountEntity account, OrganisationEntity organisation)
            throws ItemNotFoundException {
        OrganisationMember member = organisationMemberRepo.findByAccountAndOrganisation(account, organisation)
                .orElseThrow(() -> new ItemNotFoundException("Member not found in organisation"));

        if (member.getStatus() != MemberStatus.ACTIVE) {
            throw new ItemNotFoundException("Member is not active");
        }

        return member;
    }

    private AvailableDetailAllocationResponse createAggregatedAllocationResponse(
            ChartOfAccounts detailAccount, List<OrgBudgetDetailAllocationEntity> allocations) {

        AvailableDetailAllocationResponse response = new AvailableDetailAllocationResponse();

        response.setDetailAccountId(detailAccount.getId());
        response.setAccountCode(detailAccount.getAccountCode());
        response.setAccountName(detailAccount.getName());
        response.setHeadingParent(getParentHeaderAccountName(detailAccount));

        if (allocations == null || allocations.isEmpty()) {
            response.setAllocationId(null);
            response.setHeaderLineItemId(null);
            response.setAllocatedAmount(BigDecimal.ZERO);
            response.setSpentAmount(BigDecimal.ZERO);
            response.setCommittedAmount(BigDecimal.ZERO);
            response.setBudgetRemaining(BigDecimal.ZERO);
            response.setAvailableBalance(BigDecimal.ZERO);
            response.setReceiptFunding(BigDecimal.ZERO);
            response.setUnfundedAmount(BigDecimal.ZERO);
            response.setHasAllocation(false);
            response.setAllocationStatus("No Allocation");
            response.setNotes("No budget allocated to this account");
        } else {
            BigDecimal totalAllocated = allocations.stream()
                    .map(OrgBudgetDetailAllocationEntity::getAllocatedAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal totalSpent = allocations.stream()
                    .map(OrgBudgetDetailAllocationEntity::getSpentAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal totalCommitted = allocations.stream()
                    .map(OrgBudgetDetailAllocationEntity::getCommittedAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal totalReceiptFunding = allocations.stream()
                    .map(OrgBudgetDetailAllocationEntity::getTotalApprovedReceiptAllocations)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal budgetRemaining = totalAllocated.subtract(totalSpent).subtract(totalCommitted);
            BigDecimal unfundedAmount = totalAllocated.subtract(totalReceiptFunding);

            OrgBudgetDetailAllocationEntity firstAllocation = allocations.getFirst();

            response.setAllocationId(firstAllocation.getAllocationId());
            response.setHeaderLineItemId(firstAllocation.getHeaderLineItem().getLineItemId());
            response.setDetailAccountId(detailAccount.getId());
            response.setAllocatedAmount(totalAllocated);
            response.setSpentAmount(totalSpent);
            response.setCommittedAmount(totalCommitted);
            response.setBudgetRemaining(budgetRemaining);
            response.setAvailableBalance(budgetRemaining);
            response.setReceiptFunding(totalReceiptFunding);
            response.setUnfundedAmount(unfundedAmount);
            response.setHasAllocation(true);

            if (budgetRemaining.compareTo(BigDecimal.ZERO) <= 0) {
                response.setAllocationStatus("Fully Used");
            } else if (unfundedAmount.compareTo(BigDecimal.ZERO) <= 0) {
                response.setAllocationStatus("Fully Funded");
            } else {
                response.setAllocationStatus("Available");
            }

            response.setNotes(allocations.size() == 1 ?
                    firstAllocation.getAllocationNotes() :
                    "Aggregated from " + allocations.size() + " allocations");
        }

        return response;
    }

    private String getParentHeaderAccountName(ChartOfAccounts detailAccount) {
        if (detailAccount.getParentAccountId() == null) {
            return "Uncategorized";
        }

        return chartOfAccountsRepo.findById(detailAccount.getParentAccountId())
                .map(ChartOfAccounts::getName)
                .orElse("Unknown Parent");
    }
}