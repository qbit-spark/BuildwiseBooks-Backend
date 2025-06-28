package com.qbitspark.buildwisebackend.accounting_service.budget_mng.project_budget.controller;

import com.qbitspark.buildwisebackend.accounting_service.budget_mng.project_budget.entity.ProjectBudgetEntity;
import com.qbitspark.buildwisebackend.accounting_service.budget_mng.project_budget.entity.ProjectBudgetLineItemEntity;
import com.qbitspark.buildwisebackend.accounting_service.budget_mng.project_budget.payload.DistributeBudgetRequest;
import com.qbitspark.buildwisebackend.accounting_service.budget_mng.project_budget.payload.ProjectBudgetResponse;
import com.qbitspark.buildwisebackend.accounting_service.budget_mng.project_budget.payload.ProjectBudgetSummaryResponse;
import com.qbitspark.buildwisebackend.accounting_service.budget_mng.project_budget.service.ProjectBudgetService;
import com.qbitspark.buildwisebackend.accounting_service.coa.entity.ChartOfAccounts;
import com.qbitspark.buildwisebackend.accounting_service.coa.enums.AccountType;
import com.qbitspark.buildwisebackend.accounting_service.coa.repo.ChartOfAccountsRepo;
import com.qbitspark.buildwisebackend.globeadvice.exceptions.AccessDeniedException;
import com.qbitspark.buildwisebackend.globeadvice.exceptions.ItemNotFoundException;
import com.qbitspark.buildwisebackend.globeresponsebody.GlobeSuccessResponseBuilder;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/accounting/{organisationId}/project-budgets")
@RequiredArgsConstructor
public class ProjectBudgetController {

    private final ProjectBudgetService projectBudgetService;
    private final ChartOfAccountsRepo chartOfAccountsRepo;

    /**
     * Distribute budget to project accounts
     * PUT /api/v1/project-budgets/{projectBudgetId}/distribute
     */
    @PutMapping("/{projectBudgetId}/distribute")
    public ResponseEntity<GlobeSuccessResponseBuilder> distributeBudget(
            @PathVariable UUID projectBudgetId,
            @Valid @RequestBody DistributeBudgetRequest request,
            @PathVariable UUID organisationId)
            throws ItemNotFoundException, AccessDeniedException {

        ProjectBudgetEntity updatedBudget = projectBudgetService.distributeBudgetToProject(request, projectBudgetId, organisationId);
        ProjectBudgetResponse response = mapToCompleteResponse(updatedBudget);

        GlobeSuccessResponseBuilder successResponse = GlobeSuccessResponseBuilder.success(
                "Budget distributed successfully",
                response
        );

        return new ResponseEntity<>(successResponse, HttpStatus.OK);
    }

    /**
     * Get project budget with complete COA view
     * GET /api/v1/project-budgets/project/{projectId}
     */
    @GetMapping("/project/{projectId}")
    public ResponseEntity<GlobeSuccessResponseBuilder> getProjectBudget(
            @PathVariable UUID projectId, @PathVariable UUID organisationId)
            throws ItemNotFoundException, AccessDeniedException {

        ProjectBudgetEntity projectBudget = projectBudgetService.getProjectBudget(projectId, organisationId);
        ProjectBudgetResponse response = mapToCompleteResponse(projectBudget);

        GlobeSuccessResponseBuilder successResponse = GlobeSuccessResponseBuilder.success(
                "Project budget retrieved successfully",
                response
        );

        return new ResponseEntity<>(successResponse, HttpStatus.OK);
    }


    @GetMapping("/project/{projectId}/summary-list")
    public ResponseEntity<GlobeSuccessResponseBuilder> getProjectBudgetSummary(
            @PathVariable UUID projectId,
            @PathVariable UUID organisationId)
            throws ItemNotFoundException, AccessDeniedException {

        List<ProjectBudgetSummaryResponse> summaries = projectBudgetService.getProjectBudgetSummary(projectId, organisationId);

        return ResponseEntity.ok(
                GlobeSuccessResponseBuilder.success(
                        "Project budget summary retrieved successfully",
                        summaries
                )
        );
    }

    /**
     * Map entity to response with COMPLETE chart of accounts view grouped by headers
     */
    private ProjectBudgetResponse mapToCompleteResponse(ProjectBudgetEntity budget) {
        ProjectBudgetResponse response = new ProjectBudgetResponse();
        response.setProjectBudgetId(budget.getProjectBudgetId());
        response.setProjectId(budget.getProject().getProjectId());
        response.setProjectName(budget.getProject().getName());
        response.setOrgBudgetId(budget.getOrgBudget().getBudgetId());
        response.setOrgBudgetName(budget.getOrgBudget().getBudgetName());
        response.setTotalBudgetAmount(budget.getTotalBudgetAmount());
        response.setTotalSpentAmount(budget.getTotalSpentAmount());
        response.setTotalCommittedAmount(budget.getTotalCommittedAmount());
        response.setTotalRemainingAmount(budget.getTotalRemainingAmount());
        response.setStatus(budget.getStatus());
        response.setBudgetNotes(budget.getBudgetNotes());
        response.setCreatedDate(budget.getCreatedDate());

        // Get ALL expense accounts (both headers and detail accounts)
        List<ChartOfAccounts> allExpenseAccounts = chartOfAccountsRepo
                .findByOrganisationAndAccountTypeAndIsActive(
                        budget.getOrgBudget().getOrganisation(),
                        AccountType.EXPENSE,
                        true
                );

        // Separate header accounts and detail accounts
        List<ChartOfAccounts> headerAccounts = allExpenseAccounts.stream()
                .filter(ChartOfAccounts::getIsHeader)
                .toList();

        List<ChartOfAccounts> detailAccounts = allExpenseAccounts.stream()
                .filter(account -> !account.getIsHeader() && account.getIsPostable())
                .collect(Collectors.toList());

        // Create a map of existing line items by account ID for a quick lookup
        Map<UUID, ProjectBudgetLineItemEntity> lineItemMap = budget.getLineItems().stream()
                .collect(Collectors.toMap(
                        lineItem -> lineItem.getChartOfAccount().getId(),
                        lineItem -> lineItem
                ));

        // Group detail accounts by their parent (header account)
        Map<UUID, List<ChartOfAccounts>> accountsByParent = detailAccounts.stream()
                .collect(Collectors.groupingBy(ChartOfAccounts::getParentAccountId));

        // Create account groups
        List<ProjectBudgetResponse.AccountGroupResponse> accountGroups = headerAccounts.stream()
                .map(headerAccount -> createAccountGroup(headerAccount, accountsByParent.get(headerAccount.getId()), lineItemMap))
                .collect(Collectors.toList());

        // Calculate project statistics
        ProjectBudgetResponse.ProjectBudgetStatistics statistics = calculateProjectStatistics(
                budget, detailAccounts, accountGroups, lineItemMap
        );

        response.setAccountGroups(accountGroups);
        response.setProjectStatistics(statistics);
        return response;
    }

    /**
     * Create an account group with statistics
     */
    private ProjectBudgetResponse.AccountGroupResponse createAccountGroup(
            ChartOfAccounts headerAccount,
            List<ChartOfAccounts> detailAccounts,
            Map<UUID, ProjectBudgetLineItemEntity> lineItemMap) {

        ProjectBudgetResponse.AccountGroupResponse group = new ProjectBudgetResponse.AccountGroupResponse();
        group.setHeaderAccountId(headerAccount.getId());
        group.setHeaderAccountCode(headerAccount.getAccountCode());
        group.setHeaderAccountName(headerAccount.getName());
        group.setHeaderAccountDescription(headerAccount.getDescription());

        if (detailAccounts == null || detailAccounts.isEmpty()) {
            // No detail accounts under this header
            group.setLineItems(List.of());
            group.setTotalAccounts(0);
            group.setAccountsWithBudget(0);
            group.setAccountsWithoutBudget(0);
            group.setGroupTotalBudget(BigDecimal.ZERO);
            group.setGroupTotalSpent(BigDecimal.ZERO);
            group.setGroupTotalCommitted(BigDecimal.ZERO);
            group.setGroupTotalRemaining(BigDecimal.ZERO);
            return group;
        }

        // Create line items for detail accounts
        List<ProjectBudgetResponse.LineItemResponse> lineItems = detailAccounts.stream()
                .map(account -> {
                    ProjectBudgetLineItemEntity lineItem = lineItemMap.get(account.getId());
                    return mapAccountToLineItemResponse(account, lineItem);
                })
                .collect(Collectors.toList());

        // Calculate group statistics
        BigDecimal groupTotalBudget = lineItems.stream()
                .map(ProjectBudgetResponse.LineItemResponse::getBudgetAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal groupTotalSpent = lineItems.stream()
                .map(ProjectBudgetResponse.LineItemResponse::getSpentAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal groupTotalCommitted = lineItems.stream()
                .map(ProjectBudgetResponse.LineItemResponse::getCommittedAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        int accountsWithBudget = (int) lineItems.stream()
                .filter(ProjectBudgetResponse.LineItemResponse::isHasBudgetDistributed)
                .count();

        group.setLineItems(lineItems);
        group.setTotalAccounts(detailAccounts.size());
        group.setAccountsWithBudget(accountsWithBudget);
        group.setAccountsWithoutBudget(detailAccounts.size() - accountsWithBudget);
        group.setGroupTotalBudget(groupTotalBudget);
        group.setGroupTotalSpent(groupTotalSpent);
        group.setGroupTotalCommitted(groupTotalCommitted);
        group.setGroupTotalRemaining(groupTotalBudget.subtract(groupTotalSpent).subtract(groupTotalCommitted));

        return group;
    }

    /**
     * Map chart of an account to line item response (with or without budget allocation)
     */
    private ProjectBudgetResponse.LineItemResponse mapAccountToLineItemResponse(
            ChartOfAccounts account,
            ProjectBudgetLineItemEntity lineItem) {

        ProjectBudgetResponse.LineItemResponse response = new ProjectBudgetResponse.LineItemResponse();
        response.setAccountId(account.getId());
        response.setAccountCode(account.getAccountCode());
        response.setAccountName(account.getName());

        if (lineItem != null) {
            // Account has budget distributed
            response.setLineItemId(lineItem.getLineItemId());
            response.setBudgetAmount(lineItem.getBudgetAmount());
            response.setSpentAmount(lineItem.getSpentAmount());
            response.setCommittedAmount(lineItem.getCommittedAmount());
            response.setRemainingAmount(lineItem.getRemainingAmount());
            response.setLineItemNotes(lineItem.getLineItemNotes());
            response.setHasBudgetDistributed(true);
        } else {
            // Account has no budget distributed - show as $0
            response.setLineItemId(null);
            response.setBudgetAmount(BigDecimal.ZERO);
            response.setSpentAmount(BigDecimal.ZERO);
            response.setCommittedAmount(BigDecimal.ZERO);
            response.setRemainingAmount(BigDecimal.ZERO);
            response.setLineItemNotes("No budget distributed");
            response.setHasBudgetDistributed(false);
        }

        return response;
    }

    /**
     * Calculate comprehensive project budget statistics
     */
    private ProjectBudgetResponse.ProjectBudgetStatistics calculateProjectStatistics(
            ProjectBudgetEntity budget,
            List<ChartOfAccounts> detailAccounts,
            List<ProjectBudgetResponse.AccountGroupResponse> accountGroups,
            Map<UUID, ProjectBudgetLineItemEntity> lineItemMap) {

        ProjectBudgetResponse.ProjectBudgetStatistics stats = new ProjectBudgetResponse.ProjectBudgetStatistics();

        // Basic budget information
        stats.setTotalBudgetDistributed(budget.getTotalBudgetAmount());
        stats.setTotalBudgetAvailable(budget.getOrgBudget().getAvailableAmount());
        stats.setTotalSpentAmount(budget.getTotalSpentAmount());
        stats.setTotalCommittedAmount(budget.getTotalCommittedAmount());
        stats.setTotalAvailableToSpend(budget.getTotalRemainingAmount());

        // Account-level statistics
        int accountsWithBudget = (int) lineItemMap.values().stream()
                .filter(lineItem -> lineItem.getBudgetAmount().compareTo(BigDecimal.ZERO) > 0)
                .count();

        stats.setTotalExpenseAccounts(detailAccounts.size());
        stats.setAccountsWithBudgetDistributed(accountsWithBudget);
        stats.setAccountsWithoutBudget(detailAccounts.size() - accountsWithBudget);

        // Calculate percentages
        if (detailAccounts.size() > 0) {
            BigDecimal accountPercentage = BigDecimal.valueOf(accountsWithBudget)
                    .multiply(BigDecimal.valueOf(100))
                    .divide(BigDecimal.valueOf(detailAccounts.size()), 2, BigDecimal.ROUND_HALF_UP);
            stats.setPercentageAccountsWithBudget(accountPercentage);
        } else {
            stats.setPercentageAccountsWithBudget(BigDecimal.ZERO);
        }

        // Budget utilization from organization budget
        if (budget.getOrgBudget().getTotalBudgetAmount().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal utilization = budget.getTotalBudgetAmount()
                    .multiply(BigDecimal.valueOf(100))
                    .divide(budget.getOrgBudget().getTotalBudgetAmount(), 2, BigDecimal.ROUND_HALF_UP);
            stats.setBudgetUtilizationPercentage(utilization);
        } else {
            stats.setBudgetUtilizationPercentage(BigDecimal.ZERO);
        }

        // Spending percentages
        if (budget.getTotalBudgetAmount().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal spendingPercentage = budget.getTotalSpentAmount()
                    .multiply(BigDecimal.valueOf(100))
                    .divide(budget.getTotalBudgetAmount(), 2, BigDecimal.ROUND_HALF_UP);
            stats.setSpendingPercentage(spendingPercentage);

            BigDecimal commitmentPercentage = budget.getTotalCommittedAmount()
                    .multiply(BigDecimal.valueOf(100))
                    .divide(budget.getTotalBudgetAmount(), 2, BigDecimal.ROUND_HALF_UP);
            stats.setCommitmentPercentage(commitmentPercentage);
        } else {
            stats.setSpendingPercentage(BigDecimal.ZERO);
            stats.setCommitmentPercentage(BigDecimal.ZERO);
        }

        // Group-level statistics
        int groupsWithBudget = (int) accountGroups.stream()
                .filter(group -> group.getGroupTotalBudget().compareTo(BigDecimal.ZERO) > 0)
                .count();

        stats.setTotalAccountGroups(accountGroups.size());
        stats.setGroupsWithBudget(groupsWithBudget);
        stats.setGroupsWithoutBudget(accountGroups.size() - groupsWithBudget);

        // Financial health indicators
        BigDecimal totalCommittedAndSpent = budget.getTotalSpentAmount().add(budget.getTotalCommittedAmount());

        if (totalCommittedAndSpent.compareTo(budget.getTotalBudgetAmount()) > 0) {
            stats.setBudgetStatus("Over Budget");
            stats.setHasOverspendingRisk(true);
            stats.setProjectedBudgetShortfall(totalCommittedAndSpent.subtract(budget.getTotalBudgetAmount()));
        } else if (totalCommittedAndSpent.compareTo(budget.getTotalBudgetAmount().multiply(BigDecimal.valueOf(0.9))) > 0) {
            stats.setBudgetStatus("At Risk");
            stats.setHasOverspendingRisk(true);
            stats.setProjectedBudgetShortfall(BigDecimal.ZERO);
        } else if (totalCommittedAndSpent.compareTo(budget.getTotalBudgetAmount().multiply(BigDecimal.valueOf(0.7))) > 0) {
            stats.setBudgetStatus("On Track");
            stats.setHasOverspendingRisk(false);
            stats.setProjectedBudgetShortfall(BigDecimal.ZERO);
        } else {
            stats.setBudgetStatus("Under Budget");
            stats.setHasOverspendingRisk(false);
            stats.setProjectedBudgetShortfall(BigDecimal.ZERO);
        }
        return stats;
    }
}