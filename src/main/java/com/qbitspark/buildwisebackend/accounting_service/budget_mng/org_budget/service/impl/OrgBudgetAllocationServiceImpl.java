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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
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
            UUID organisationId, UUID budgetId, UUID headerLineItemId, AllocateMoneyRequest request)
            throws ItemNotFoundException {

        AccountEntity authenticatedAccount = getAuthenticatedAccount();

        // Validate organisation access
        OrganisationEntity organisation = organisationRepo.findById(organisationId)
                .orElseThrow(() -> new ItemNotFoundException("Organisation not found"));

        validateMemberPermissions(authenticatedAccount, organisation,
                Arrays.asList(MemberRole.OWNER, MemberRole.ADMIN));

        // Validate budget exists and belongs to organisation
        OrgBudgetEntity budget = orgBudgetRepo.findById(budgetId)
                .orElseThrow(() -> new ItemNotFoundException("Budget not found"));

        if (!budget.getOrganisation().getOrganisationId().equals(organisationId)) {
            throw new ItemNotFoundException("Budget does not belong to this organisation");
        }

        // Validate header line item
        OrgBudgetLineItemEntity headerLineItem = orgBudgetLineItemRepo.findById(headerLineItemId)
                .orElseThrow(() -> new ItemNotFoundException("Header line item not found"));

        if (!headerLineItem.getOrgBudget().getBudgetId().equals(budgetId)) {
            throw new ItemNotFoundException("Header line item does not belong to this budget");
        }

        // Validate header account is actually a header
        if (!headerLineItem.getChartOfAccount().getIsHeader()) {
            throw new ItemNotFoundException("Account is not a header account");
        }

        // Calculate total allocation amount
        BigDecimal totalAllocationAmount = request.getDetailAllocations().stream()
                .map(AllocateMoneyRequest.DetailAllocation::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Validate total allocation doesn't exceed header budget
        BigDecimal headerBudgetAmount = headerLineItem.getBudgetAmount();
        BigDecimal currentlyAllocated = getCurrentlyAllocatedAmount(headerLineItem);
        BigDecimal availableForAllocation = headerBudgetAmount.subtract(currentlyAllocated);

        if (totalAllocationAmount.compareTo(availableForAllocation) > 0) {
            throw new ItemNotFoundException(String.format(
                    "Total allocation (%s) exceeds available header budget (%s). Header budget: %s, Already allocated: %s",
                    totalAllocationAmount, availableForAllocation, headerBudgetAmount, currentlyAllocated
            ));
        }

        // Process each detail allocation
        List<OrgBudgetDetailAllocationEntity> updatedAllocations = new ArrayList<>();

        for (AllocateMoneyRequest.DetailAllocation detailAllocation : request.getDetailAllocations()) {

            // Validate detail account
            ChartOfAccounts detailAccount = chartOfAccountsRepo.findById(detailAllocation.getDetailAccountId())
                    .orElseThrow(() -> new ItemNotFoundException("Detail account not found: " + detailAllocation.getDetailAccountId()));

            // Validate detail account belongs to organisation
            if (!detailAccount.getOrganisation().getOrganisationId().equals(organisationId)) {
                throw new ItemNotFoundException("Detail account does not belong to this organisation");
            }

            // Validate detail account is child of header account
            if (!headerLineItem.getChartOfAccount().getId().equals(detailAccount.getParentAccountId())) {
                throw new ItemNotFoundException(String.format(
                        "Detail account '%s' is not a child of header account '%s'",
                        detailAccount.getName(), headerLineItem.getChartOfAccount().getName()
                ));
            }

            // Validate detail account is postable (not a header)
            if (detailAccount.getIsHeader()) {
                throw new ItemNotFoundException("Cannot allocate to header account: " + detailAccount.getName());
            }

            // Validate account type is EXPENSE
            if (detailAccount.getAccountType() != AccountType.EXPENSE) {
                throw new ItemNotFoundException("Can only allocate to expense accounts: " + detailAccount.getName());
            }

            // Find or create allocation
            OrgBudgetDetailAllocationEntity allocation = allocationRepo
                    .findByHeaderLineItemAndDetailAccount(headerLineItem, detailAccount)
                    .orElseGet(() -> {
                        OrgBudgetDetailAllocationEntity newAllocation = new OrgBudgetDetailAllocationEntity();
                        newAllocation.setHeaderLineItem(headerLineItem);
                        newAllocation.setDetailAccount(detailAccount);
                        newAllocation.setSpentAmount(BigDecimal.ZERO);
                        newAllocation.setCommittedAmount(BigDecimal.ZERO);
                        newAllocation.setCreatedDate(LocalDateTime.now());
                        newAllocation.setCreatedBy(authenticatedAccount.getAccountId());
                        return newAllocation;
                    });

            // Update allocation
            allocation.setAllocatedAmount(detailAllocation.getAmount());
            allocation.setAllocationNotes(detailAllocation.getDescription());
            allocation.setModifiedDate(LocalDateTime.now());
            allocation.setModifiedBy(authenticatedAccount.getAccountId());

            OrgBudgetDetailAllocationEntity savedAllocation = allocationRepo.save(allocation);
            updatedAllocations.add(savedAllocation);
        }

        return updatedAllocations;
    }

    @Override
    public void initializeDetailAllocations(UUID headerLineItemId, UUID organisationId) throws ItemNotFoundException {

        // Get header line item
        OrgBudgetLineItemEntity headerLineItem = orgBudgetLineItemRepo.findById(headerLineItemId)
                .orElseThrow(() -> new ItemNotFoundException("Header line item not found"));

        // Validate organisation
        if (!headerLineItem.getOrgBudget().getOrganisation().getOrganisationId().equals(organisationId)) {
            throw new ItemNotFoundException("Header line item does not belong to this organisation");
        }

        ChartOfAccounts headerAccount = headerLineItem.getChartOfAccount();

        // Get all detail accounts under this header
        List<ChartOfAccounts> detailAccounts = chartOfAccountsRepo
                .findByOrganisationAndAccountTypeAndIsActive(headerAccount.getOrganisation(), AccountType.EXPENSE, true)
                .stream()
                .filter(account -> !account.getIsHeader()) // Only detail accounts
                .filter(account -> account.getIsPostable()) // Only postable accounts
                .filter(account -> headerAccount.getId().equals(account.getParentAccountId())) // Only children of this header
                .collect(Collectors.toList());

        // Create allocations with $0 for each detail account
        for (ChartOfAccounts detailAccount : detailAccounts) {
            // Check if allocation already exists
            if (!allocationRepo.existsByHeaderLineItemAndDetailAccount(headerLineItem, detailAccount)) {
                OrgBudgetDetailAllocationEntity allocation = new OrgBudgetDetailAllocationEntity();
                allocation.setHeaderLineItem(headerLineItem);
                allocation.setDetailAccount(detailAccount);
                allocation.setAllocatedAmount(BigDecimal.ZERO);
                allocation.setSpentAmount(BigDecimal.ZERO);
                allocation.setCommittedAmount(BigDecimal.ZERO);
                allocation.setAllocationNotes("Initialized - awaiting money allocation");
                allocation.setCreatedDate(LocalDateTime.now());
                allocation.setCreatedBy(headerLineItem.getCreatedBy());

                allocationRepo.save(allocation);
            }
        }
    }

    @Override
    public List<OrgBudgetDetailAllocationEntity> getHeaderAllocations(
            UUID organisationId, UUID budgetId, UUID headerLineItemId) throws ItemNotFoundException {

        AccountEntity authenticatedAccount = getAuthenticatedAccount();

        // Validate access
        OrganisationEntity organisation = organisationRepo.findById(organisationId)
                .orElseThrow(() -> new ItemNotFoundException("Organisation not found"));

        validateMemberPermissions(authenticatedAccount, organisation,
                Arrays.asList(MemberRole.OWNER, MemberRole.ADMIN, MemberRole.MEMBER));

        // Validate header line item
        OrgBudgetLineItemEntity headerLineItem = orgBudgetLineItemRepo.findById(headerLineItemId)
                .orElseThrow(() -> new ItemNotFoundException("Header line item not found"));

        if (!headerLineItem.getOrgBudget().getBudgetId().equals(budgetId)) {
            throw new ItemNotFoundException("Header line item does not belong to this budget");
        }

        return allocationRepo.findByHeaderLineItemOrderByDetailAccountAccountCode(headerLineItem);
    }

    @Override
    public AllocationSummaryResponse getAllocationSummary(
            UUID organisationId, UUID budgetId, UUID headerLineItemId) throws ItemNotFoundException {

        List<OrgBudgetDetailAllocationEntity> allocations = getHeaderAllocations(organisationId, budgetId, headerLineItemId);

        // Get header line item for budget info
        OrgBudgetLineItemEntity headerLineItem = orgBudgetLineItemRepo.findById(headerLineItemId)
                .orElseThrow(() -> new ItemNotFoundException("Header line item not found"));

        AllocationSummaryResponse summary = new AllocationSummaryResponse();
        summary.setHeaderLineItemId(headerLineItemId);
        summary.setHeaderAccountCode(headerLineItem.getChartOfAccount().getAccountCode());
        summary.setHeaderAccountName(headerLineItem.getChartOfAccount().getName());
        summary.setHeaderBudgetAmount(headerLineItem.getBudgetAmount());

        // Calculate totals from allocations
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

        // Count statistics
        summary.setTotalDetailAccounts(allocations.size());
        summary.setAccountsWithAllocation((int) allocations.stream().filter(OrgBudgetDetailAllocationEntity::hasAllocation).count());
        summary.setAccountsWithoutAllocation(allocations.size() - summary.getAccountsWithAllocation());

        return summary;
    }

    @Override
    public List<OrgBudgetDetailAllocationEntity> getAllBudgetAllocations(UUID organisationId, UUID budgetId)
            throws ItemNotFoundException {

        AccountEntity authenticatedAccount = getAuthenticatedAccount();

        // Validate access
        OrganisationEntity organisation = organisationRepo.findById(organisationId)
                .orElseThrow(() -> new ItemNotFoundException("Organisation not found"));

        validateMemberPermissions(authenticatedAccount, organisation,
                Arrays.asList(MemberRole.OWNER, MemberRole.ADMIN, MemberRole.MEMBER));

        // Get budget
        OrgBudgetEntity budget = orgBudgetRepo.findById(budgetId)
                .orElseThrow(() -> new ItemNotFoundException("Budget not found"));

        if (!budget.getOrganisation().getOrganisationId().equals(organisationId)) {
            throw new ItemNotFoundException("Budget does not belong to this organisation");
        }

        // Get all allocations for all header line items in this budget
        List<OrgBudgetDetailAllocationEntity> allAllocations = new ArrayList<>();

        for (OrgBudgetLineItemEntity headerLineItem : budget.getLineItems()) {
            List<OrgBudgetDetailAllocationEntity> headerAllocations =
                    allocationRepo.findByHeaderLineItemOrderByDetailAccountAccountCode(headerLineItem);
            allAllocations.addAll(headerAllocations);
        }

        return allAllocations;
    }

    // Helper methods
    private BigDecimal getCurrentlyAllocatedAmount(OrgBudgetLineItemEntity headerLineItem) {
        List<OrgBudgetDetailAllocationEntity> existingAllocations =
                allocationRepo.findByHeaderLineItem(headerLineItem);

        return existingAllocations.stream()
                .map(OrgBudgetDetailAllocationEntity::getAllocatedAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private OrganisationMember validateMemberPermissions(AccountEntity account, OrganisationEntity organisation,
                                                         List<MemberRole> allowedRoles) throws ItemNotFoundException {

        OrganisationMember member = organisationMemberRepo.findByAccountAndOrganisation(account, organisation)
                .orElseThrow(() -> new ItemNotFoundException("Member is not found in organisation"));

        if (member.getStatus() != MemberStatus.ACTIVE) {
            throw new ItemNotFoundException("Member is not active");
        }

        if (!allowedRoles.contains(member.getRole())) {
            throw new ItemNotFoundException("Member has insufficient permissions");
        }

        return member;
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
}