package com.qbitspark.buildwisebackend.accounting_service.budget_mng.org_budget.service;

import com.qbitspark.buildwisebackend.accounting_service.budget_mng.org_budget.entity.BudgetSpendingEntity;
import com.qbitspark.buildwisebackend.accounting_service.documentflow.voucher.entity.VoucherEntity;
import com.qbitspark.buildwisebackend.globeadvice.exceptions.AccessDeniedException;
import com.qbitspark.buildwisebackend.globeadvice.exceptions.ItemNotFoundException;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface BudgetSpendingService {

    /**
     * Records spending from approved voucher
     * Creates spending records for each beneficiary + deductions
     */
    List<BudgetSpendingEntity> recordSpendingFromVoucher(UUID organisationId, VoucherEntity voucher)
            throws ItemNotFoundException, AccessDeniedException;

    /**
     * Calculates available balance for an account
     * Available = Total Funded - Total Spent
     */
    BigDecimal getAccountAvailableBalance(UUID accountId);

    /**
     * Gets total amount spent from an account
     */
    BigDecimal getAccountTotalSpent(UUID accountId);

    /**
     * Checks if account has sufficient balance for spending
     */
    boolean canSpendFromAccount(UUID accountId, BigDecimal amount);

    /**
     * Gets all spending records for a voucher
     */
    List<BudgetSpendingEntity> getVoucherSpendingRecords(UUID voucherId);

}