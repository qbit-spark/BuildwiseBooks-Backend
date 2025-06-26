package com.qbitspark.buildwisebackend.accounting_service.budget_mng.project_budget.repo;

import com.qbitspark.buildwisebackend.accounting_service.budget_mng.project_budget.entity.ProjectBudgetEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ProjectBudgetRepo extends JpaRepository<ProjectBudgetEntity, UUID> {
}
