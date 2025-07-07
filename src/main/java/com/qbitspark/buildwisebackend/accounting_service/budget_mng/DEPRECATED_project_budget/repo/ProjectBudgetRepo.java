package com.qbitspark.buildwisebackend.accounting_service.budget_mng.DEPRECATED_project_budget.repo;

import com.qbitspark.buildwisebackend.accounting_service.budget_mng.org_budget.entity.OrgBudgetEntity;
import com.qbitspark.buildwisebackend.accounting_service.budget_mng.DEPRECATED_project_budget.entity.ProjectBudgetEntity;
import com.qbitspark.buildwisebackend.accounting_service.budget_mng.DEPRECATED_project_budget.enums.ProjectBudgetStatus;
import com.qbitspark.buildwisebackend.projectmng_service.entity.ProjectEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Deprecated
public interface ProjectBudgetRepo extends JpaRepository<ProjectBudgetEntity, UUID> {

 Optional<ProjectBudgetEntity> findByProject(ProjectEntity project);

 List<ProjectBudgetEntity> findByOrgBudget(OrgBudgetEntity orgBudget);

 List<ProjectBudgetEntity> findByStatus(ProjectBudgetStatus status);

 boolean existsByProject(ProjectEntity project);
}
