package com.qbitspark.buildwisebackend.accounting_service.budget_mng.org_budget.repo;

import com.qbitspark.buildwisebackend.accounting_service.budget_mng.org_budget.entity.BudgetSpendingEntity;
import com.qbitspark.buildwisebackend.accounting_service.coa.entity.ChartOfAccounts;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface BudgetSpendingRepo extends JpaRepository<BudgetSpendingEntity, UUID> {

    List<BudgetSpendingEntity> findByAccount(ChartOfAccounts account);

    List<BudgetSpendingEntity> findBySourceVoucherId(UUID voucherId);
}