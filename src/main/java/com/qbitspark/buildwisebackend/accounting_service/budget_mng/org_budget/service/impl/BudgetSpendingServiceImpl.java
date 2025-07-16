package com.qbitspark.buildwisebackend.accounting_service.budget_mng.org_budget.service.impl;

import com.qbitspark.buildwisebackend.accounting_service.budget_mng.org_budget.entity.BudgetFundingAllocationEntity;
import com.qbitspark.buildwisebackend.accounting_service.budget_mng.org_budget.entity.BudgetSpendingEntity;
import com.qbitspark.buildwisebackend.accounting_service.budget_mng.org_budget.enums.SpendingType;
import com.qbitspark.buildwisebackend.accounting_service.budget_mng.org_budget.repo.BudgetFundingAllocationRepo;
import com.qbitspark.buildwisebackend.accounting_service.budget_mng.org_budget.repo.BudgetSpendingRepo;
import com.qbitspark.buildwisebackend.accounting_service.budget_mng.org_budget.service.BudgetSpendingService;
import com.qbitspark.buildwisebackend.accounting_service.coa.entity.ChartOfAccounts;
import com.qbitspark.buildwisebackend.accounting_service.coa.enums.AccountType;
import com.qbitspark.buildwisebackend.accounting_service.coa.repo.ChartOfAccountsRepo;
import com.qbitspark.buildwisebackend.accounting_service.documentflow.voucher.entity.VoucherBeneficiaryEntity;
import com.qbitspark.buildwisebackend.accounting_service.documentflow.voucher.entity.VoucherDeductionEntity;
import com.qbitspark.buildwisebackend.accounting_service.documentflow.voucher.entity.VoucherEntity;
import com.qbitspark.buildwisebackend.authentication_service.Repository.AccountRepo;
import com.qbitspark.buildwisebackend.authentication_service.entity.AccountEntity;
import com.qbitspark.buildwisebackend.globeadvice.exceptions.AccessDeniedException;
import com.qbitspark.buildwisebackend.globeadvice.exceptions.ItemNotFoundException;
import com.qbitspark.buildwisebackend.organisation_service.organisation_mng.entity.OrganisationEntity;
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
public class BudgetSpendingServiceImpl implements BudgetSpendingService {

    private final BudgetSpendingRepo budgetSpendingRepo;
    private final BudgetFundingAllocationRepo budgetFundingAllocationRepo;
    private final ChartOfAccountsRepo chartOfAccountsRepo;
    private final AccountRepo accountRepo;

    @Override
    public List<BudgetSpendingEntity> recordSpendingFromVoucher(UUID organisationId, VoucherEntity voucher)
            throws ItemNotFoundException {

        AccountEntity currentUser = getAuthenticatedAccount();
        List<BudgetSpendingEntity> spendingRecords = new ArrayList<>();

        // Validate voucher has an account
        if (voucher.getAccount() == null) {
            throw new ItemNotFoundException("Voucher must have an account assigned");
        }

        // Validate account belongs to an organisation
        if (!voucher.getAccount().getOrganisation().getOrganisationId().equals(organisationId)) {
            throw new ItemNotFoundException("Account does not belong to this organisation");
        }

        for (VoucherBeneficiaryEntity beneficiary : voucher.getBeneficiaries()) {
            // Create a spending record for beneficiary payment
            BudgetSpendingEntity beneficiarySpending = createBeneficiarySpending(
                    voucher, beneficiary, currentUser);
            spendingRecords.add(beneficiarySpending);

            // Create spending records for deductions
            List<BudgetSpendingEntity> deductionSpending = createDeductionSpending(
                    voucher, beneficiary, currentUser);
            spendingRecords.addAll(deductionSpending);
        }

        return budgetSpendingRepo.saveAll(spendingRecords);
    }

    @Override
    public BigDecimal getAccountAvailableBalance(UUID accountId) {
        ChartOfAccounts account = chartOfAccountsRepo.findById(accountId)
                .orElseThrow(() -> new RuntimeException("Account not found"));

        BigDecimal totalFunded = getTotalFunded(account);
        BigDecimal totalSpent = getAccountTotalSpent(accountId);

        return totalFunded.subtract(totalSpent);
    }

    @Override
    public BigDecimal getAccountTotalSpent(UUID accountId) {
        ChartOfAccounts account = chartOfAccountsRepo.findById(accountId)
                .orElseThrow(() -> new RuntimeException("Account not found"));

        return budgetSpendingRepo.findByAccount(account).stream()
                .map(BudgetSpendingEntity::getSpentAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Override
    public boolean canSpendFromAccount(UUID accountId, BigDecimal amount) {
        BigDecimal availableBalance = getAccountAvailableBalance(accountId);
        return availableBalance.compareTo(amount) >= 0;
    }

    @Override
    public List<BudgetSpendingEntity> getVoucherSpendingRecords(UUID voucherId) {
        return budgetSpendingRepo.findBySourceVoucherId(voucherId);
    }

    // Helper methods
    private BudgetSpendingEntity createBeneficiarySpending(VoucherEntity voucher,
                                                           VoucherBeneficiaryEntity beneficiary, AccountEntity currentUser) {

        BudgetSpendingEntity spending = new BudgetSpendingEntity();
        spending.setAccount(voucher.getAccount());
        spending.setSpentAmount(beneficiary.getAmount());
        spending.setSpendingType(SpendingType.VOUCHER_SPENDING);
        spending.setSourceVoucherId(voucher.getId());
        spending.setSourceVoucherBeneficiaryId(beneficiary.getId());
        spending.setSpentBy(currentUser.getAccountId());
        spending.setReferenceNumber(generateReferenceNumber(voucher, beneficiary));
        spending.setDescription(buildSpendingDescription(voucher, beneficiary));

        return spending;
    }

    private List<BudgetSpendingEntity> createDeductionSpending(VoucherEntity voucher,
                                                               VoucherBeneficiaryEntity beneficiary, AccountEntity currentUser) {

        List<BudgetSpendingEntity> deductionRecords = new ArrayList<>();

        for (VoucherDeductionEntity deduction : beneficiary.getDeductions()) {
            // Get a deduction account (placeholder for now)
            ChartOfAccounts deductionAccount = getDeductionAccount(deduction.getDeductId(),
                    voucher.getOrganisation());

            BudgetSpendingEntity deductionSpending = new BudgetSpendingEntity();
            deductionSpending.setAccount(deductionAccount);
            deductionSpending.setSpentAmount(deduction.getDeductionAmount());
            deductionSpending.setSpendingType(SpendingType.VOUCHER_SPENDING);
            deductionSpending.setSourceVoucherId(voucher.getId());
            deductionSpending.setSourceVoucherBeneficiaryId(beneficiary.getId());
            deductionSpending.setSpentBy(currentUser.getAccountId());
            deductionSpending.setReferenceNumber(generateDeductionReferenceNumber(voucher, deduction));
            deductionSpending.setDescription(buildDeductionDescription(voucher, beneficiary, deduction));

            deductionRecords.add(deductionSpending);
        }

        return deductionRecords;
    }

    private ChartOfAccounts getDeductionAccount(UUID deductId, OrganisationEntity organisation) {
        // TODO: Implement proper deduction account mapping
        // For now, return a placeholder account or the same account
        // This will be implemented when deduction accounts are properly configured

        // Placeholder: Return the first liability account for this organisation
        return chartOfAccountsRepo.findByOrganisationAndAccountTypeAndIsActive(
                        organisation, AccountType.LIABILITY, true)
                .stream()
                .findFirst()
                .orElseThrow(() -> new RuntimeException("No liability account found for deductions"));
    }

    private BigDecimal getTotalFunded(ChartOfAccounts account) {
        return budgetFundingAllocationRepo.findByAccount(account).stream()
                .map(BudgetFundingAllocationEntity::getFundedAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private String generateReferenceNumber(VoucherEntity voucher, VoucherBeneficiaryEntity beneficiary) {
        return String.format("SP-%s-%s", voucher.getVoucherNumber(),
                System.currentTimeMillis() % 1000);
    }

    private String generateDeductionReferenceNumber(VoucherEntity voucher, VoucherDeductionEntity deduction) {
        return String.format("SP-%s-DED-%s", voucher.getVoucherNumber(),
                deduction.getDeductName().toUpperCase());
    }

    private String buildSpendingDescription(VoucherEntity voucher, VoucherBeneficiaryEntity beneficiary) {
        return String.format("Payment to %s via voucher %s",
                beneficiary.getVendor().getName(), voucher.getVoucherNumber());
    }

    private String buildDeductionDescription(VoucherEntity voucher, VoucherBeneficiaryEntity beneficiary,
                                             VoucherDeductionEntity deduction) {
        return String.format("%s deduction from %s payment (voucher %s)",
                deduction.getDeductName(), beneficiary.getVendor().getName(), voucher.getVoucherNumber());
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
}