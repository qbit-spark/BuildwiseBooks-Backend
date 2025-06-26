package com.qbitspark.buildwisebackend.accounting_service.budget_mng.org_budget.service.impl;

import com.qbitspark.buildwisebackend.accounting_service.budget_mng.org_budget.entity.OrgBudgetEntity;
import com.qbitspark.buildwisebackend.accounting_service.budget_mng.org_budget.enums.OrgBudgetStatus;
import com.qbitspark.buildwisebackend.accounting_service.budget_mng.org_budget.paylaods.CreateBudgetRequest;
import com.qbitspark.buildwisebackend.accounting_service.budget_mng.org_budget.paylaods.UpdateBudgetRequest;
import com.qbitspark.buildwisebackend.accounting_service.budget_mng.org_budget.repo.OrgBudgetRepo;
import com.qbitspark.buildwisebackend.accounting_service.budget_mng.org_budget.service.OrgBudgetService;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrgBudgetServiceImpl implements OrgBudgetService {

    private final OrganisationRepo organisationRepo;
    private final OrganisationMemberRepo organisationMemberRepo;
    private final AccountRepo accountRepo;
    private final OrgBudgetRepo orgBudgetRepo;

    @Override
    public OrgBudgetEntity createBudget(CreateBudgetRequest request, UUID organisationId) throws ItemNotFoundException {

        AccountEntity authenticatedAccount = getAuthenticatedAccount();

        OrganisationEntity organisation = organisationRepo.findById(organisationId)
                .orElseThrow(() -> new ItemNotFoundException("Organisation does not exist"));

        OrganisationMember creator = validateMemberPermissions(authenticatedAccount, organisation,
                Arrays.asList(MemberRole.OWNER, MemberRole.ADMIN));

        // Auto-generate budget name based on financial year
        String budgetName = generateBudgetName(request.getFinancialYearStart(), request.getFinancialYearEnd());

        OrgBudgetEntity budget = new OrgBudgetEntity();
        budget.setOrganisation(organisation);
        budget.setBudgetName(budgetName);
        budget.setFinancialYearStart(request.getFinancialYearStart());
        budget.setFinancialYearEnd(request.getFinancialYearEnd());
        budget.setTotalBudgetAmount(request.getTotalBudgetAmount());
        budget.setAllocatedAmount(BigDecimal.ZERO);
        budget.setDescription(request.getDescription());
        budget.setCreatedBy(authenticatedAccount.getAccountId());
        budget.setCreatedDate(LocalDateTime.now());
        budget.setStatus(OrgBudgetStatus.DRAFT);
        budget.setModifiedBy(authenticatedAccount.getAccountId());
        budget.setModifiedDate(LocalDateTime.now());
        budget.setBudgetVersion(1);

        // Save and return
        return orgBudgetRepo.save(budget);
    }

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

        // CRITICAL: Deactivate any existing active budget
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

        // CRITICAL: Only allow updates to DRAFT budgets
        if (budgetToUpdate.getStatus() != OrgBudgetStatus.DRAFT) {
            throw new ItemNotFoundException("Only draft budgets can be updated");
        }


        if (request.getFinancialYearStart() != null) {
            budgetToUpdate.setFinancialYearStart(request.getFinancialYearStart());
        }

        if (request.getFinancialYearEnd() != null) {
            budgetToUpdate.setFinancialYearEnd(request.getFinancialYearEnd());
        }

        if (request.getTotalBudgetAmount() != null) {
            budgetToUpdate.setTotalBudgetAmount(request.getTotalBudgetAmount());
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

    private String generateBudgetName(LocalDate startDate, LocalDate endDate) {
        int startYear = startDate.getYear();
        int endYear = endDate.getYear();

        if (startYear == endYear) {
            return "FY" + startYear;
        } else {
            return "FY" + startYear + "-" + endYear;
        }
    }


    private OrganisationMember validateMemberPermissions(AccountEntity account, OrganisationEntity organisation, List<MemberRole> allowedRoles) throws ItemNotFoundException {

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
