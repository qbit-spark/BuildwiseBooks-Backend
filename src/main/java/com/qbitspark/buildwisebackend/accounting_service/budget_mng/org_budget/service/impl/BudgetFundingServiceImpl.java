package com.qbitspark.buildwisebackend.accounting_service.budget_mng.org_budget.service.impl;

import com.qbitspark.buildwisebackend.accounting_service.budget_mng.org_budget.entity.BudgetFundingAllocationEntity;
import com.qbitspark.buildwisebackend.accounting_service.budget_mng.org_budget.entity.OrgBudgetDetailDistributionEntity;
import com.qbitspark.buildwisebackend.accounting_service.budget_mng.org_budget.entity.OrgBudgetEntity;
import com.qbitspark.buildwisebackend.accounting_service.budget_mng.org_budget.enums.FundingType;
import com.qbitspark.buildwisebackend.accounting_service.budget_mng.org_budget.enums.OrgBudgetStatus;
import com.qbitspark.buildwisebackend.accounting_service.budget_mng.org_budget.repo.BudgetFundingAllocationRepo;
import com.qbitspark.buildwisebackend.accounting_service.budget_mng.org_budget.repo.OrgBudgetDetailDistributionRepo;
import com.qbitspark.buildwisebackend.accounting_service.budget_mng.org_budget.repo.OrgBudgetRepo;
import com.qbitspark.buildwisebackend.accounting_service.budget_mng.org_budget.service.BudgetFundingService;
import com.qbitspark.buildwisebackend.accounting_service.coa.entity.ChartOfAccounts;
import com.qbitspark.buildwisebackend.accounting_service.receipt_mng.entity.ReceiptAllocationDetailEntity;
import com.qbitspark.buildwisebackend.accounting_service.receipt_mng.entity.ReceiptAllocationEntity;
import com.qbitspark.buildwisebackend.accounting_service.receipt_mng.enums.AllocationStatus;
import com.qbitspark.buildwisebackend.authentication_service.Repository.AccountRepo;
import com.qbitspark.buildwisebackend.authentication_service.entity.AccountEntity;
import com.qbitspark.buildwisebackend.globeadvice.exceptions.AccessDeniedException;
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
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class BudgetFundingServiceImpl implements BudgetFundingService {

    private final BudgetFundingAllocationRepo budgetFundingAllocationRepo;
    private final OrgBudgetRepo orgBudgetRepo;
    private final OrganisationRepo organisationRepo;
    private final AccountRepo accountRepo;
    private final OrganisationMemberRepo organisationMemberRepo;
    private final OrgBudgetDetailDistributionRepo orgBudgetDetailDistributionRepo;

    @Override
    public List<BudgetFundingAllocationEntity> fundAccountsFromAllocation(UUID organisationId, ReceiptAllocationEntity allocation)
            throws ItemNotFoundException, AccessDeniedException {

        AccountEntity currentUser = getAuthenticatedAccount();
        OrganisationEntity organisation = getOrganisation(organisationId);
        OrganisationMember member = validateOrganisationMemberAccess(currentUser, organisation);

        if (allocation.getStatus() != AllocationStatus.APPROVED) {
            throw new ItemNotFoundException("Only approved allocations can be funded to budget");
        }

        if (!allocation.getReceipt().getOrganisation().getOrganisationId().equals(organisationId)) {
            throw new ItemNotFoundException("Allocation does not belong to this organisation");
        }

        // Find active budget for organisation
        OrgBudgetEntity activeBudget = findActiveBudget(organisation);

        // Validate allocation details and create funding allocations
        List<BudgetFundingAllocationEntity> fundingAllocations = new ArrayList<>();

        for (ReceiptAllocationDetailEntity detail : allocation.getAllocationDetails()) {
            ChartOfAccounts account = detail.getAccount();

            // Validate account belongs to an organisation
            if (!account.getOrganisation().getOrganisationId().equals(organisationId)) {
                throw new ItemNotFoundException("Account does not belong to this organisation: " + account.getAccountCode());
            }

            // Validate account exists in current budget distribution
            List<OrgBudgetDetailDistributionEntity> distributions = orgBudgetDetailDistributionRepo
                    .findByBudgetAndDetailAccount(activeBudget, account);

            if (distributions.isEmpty()) {
                throw new ItemNotFoundException(String.format(
                        "Account %s (%s) is not included in the current budget distribution. " +
                                "Please add this account to the budget before allocating funds.",
                        account.getAccountCode(), account.getName()));
            }

            // Get the distribution for validation (should be only one)
            OrgBudgetDetailDistributionEntity distribution = distributions.get(0);

            // Optional: Check if account has budget allocated (distributed amount > 0)
            if (distribution.getDistributedAmount().compareTo(BigDecimal.ZERO) == 0) {
                throw new ItemNotFoundException(String.format(
                        "Account %s (%s) has no budget allocated. Distributed amount is zero. " +
                                "Please allocate budget to this account before funding.",
                        account.getAccountCode(), account.getName()));
            }

            // Calculate how much budget is already funded for this account
            List<BudgetFundingAllocationEntity> existingFunding = budgetFundingAllocationRepo
                    .findByBudgetAndAccount(activeBudget, account);

            BigDecimal totalAlreadyFunded = existingFunding.stream()
                    .map(BudgetFundingAllocationEntity::getFundedAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            // Check if new funding would exceed budget
            BigDecimal availableBudget = distribution.getDistributedAmount().subtract(totalAlreadyFunded);
            if (detail.getAllocatedAmount().compareTo(availableBudget) > 0) {
                throw new ItemNotFoundException(String.format(
                        "Cannot fund %s to account %s (%s). Budget allocated: %s, Already funded: %s, Available: %s",
                        detail.getAllocatedAmount(), account.getAccountCode(), account.getName(),
                        distribution.getDistributedAmount(), totalAlreadyFunded, availableBudget
                ));
            }

            // Create budget funding allocation
            BudgetFundingAllocationEntity fundingAllocation = new BudgetFundingAllocationEntity();
            fundingAllocation.setBudget(activeBudget);
            fundingAllocation.setAccount(account);
            fundingAllocation.setFundedAmount(detail.getAllocatedAmount());
            fundingAllocation.setDescription(buildFundingDescription(allocation, detail));
            fundingAllocation.setFundingType(FundingType.RECEIPT_ALLOCATION);
            fundingAllocation.setSourceReceiptAllocationDetailId(detail.getDetailId());
            fundingAllocation.setSourceReceiptId(allocation.getReceipt().getReceiptId());
            fundingAllocation.setFundedBy(currentUser.getAccountId());
            fundingAllocation.setFundedDate(LocalDateTime.now());
            fundingAllocation.setReferenceNumber(generateReferenceNumber(allocation));

            BudgetFundingAllocationEntity savedFunding = budgetFundingAllocationRepo.save(fundingAllocation);
            fundingAllocations.add(savedFunding);
        }

        return fundingAllocations;
    }


    // Helper methods
    private OrgBudgetEntity findActiveBudget(OrganisationEntity organisation) throws ItemNotFoundException {
        return orgBudgetRepo.findByOrganisationAndStatus(organisation, OrgBudgetStatus.ACTIVE)
                .orElseThrow(() -> new ItemNotFoundException("No active budget found for organisation"));
    }

    private String buildFundingDescription(ReceiptAllocationEntity allocation, ReceiptAllocationDetailEntity detail) {
        return String.format("Funding from receipt %s - %s",
                allocation.getReceipt().getReceiptNumber(),
                detail.getDescription() != null ? detail.getDescription() : "Budget funding");
    }

    private String generateReferenceNumber(ReceiptAllocationEntity allocation) {
        return String.format("RF-%s-%d",
                allocation.getReceipt().getReceiptNumber(),
                System.currentTimeMillis() % 10000);
    }

    private OrganisationEntity getOrganisation(UUID organisationId) throws ItemNotFoundException {
        return organisationRepo.findById(organisationId)
                .orElseThrow(() -> new ItemNotFoundException("Organisation not found"));
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

    private OrganisationMember validateOrganisationMemberAccess(AccountEntity account, OrganisationEntity organisation)
            throws ItemNotFoundException {
        OrganisationMember member = organisationMemberRepo.findByAccountAndOrganisation(account, organisation)
                .orElseThrow(() -> new ItemNotFoundException("Member not found in organisation"));

        if (member.getStatus() != MemberStatus.ACTIVE) {
            throw new ItemNotFoundException("Member is not active");
        }

        return member;
    }
}