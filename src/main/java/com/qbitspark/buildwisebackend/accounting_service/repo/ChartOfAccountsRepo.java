package com.qbitspark.buildwisebackend.accounting_service.repo;

import com.qbitspark.buildwisebackend.accounting_service.entity.ChartOfAccounts;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ChartOfAccountsRepo extends JpaRepository<ChartOfAccounts, UUID> {
}
