package com.qbitspark.buildwisebackend.accounting_service.budget_mng.org_budget.controller;

import com.qbitspark.buildwisebackend.accounting_service.budget_mng.org_budget.entity.OrgBudgetEntity;
import com.qbitspark.buildwisebackend.accounting_service.budget_mng.org_budget.entity.OrgBudgetLineItemEntity;
import com.qbitspark.buildwisebackend.accounting_service.budget_mng.org_budget.paylaods.*;
import com.qbitspark.buildwisebackend.accounting_service.budget_mng.org_budget.service.OrgBudgetService;
import com.qbitspark.buildwisebackend.accounting_service.coa.entity.ChartOfAccounts;
import com.qbitspark.buildwisebackend.accounting_service.coa.enums.AccountType;
import com.qbitspark.buildwisebackend.accounting_service.coa.repo.ChartOfAccountsRepo;
import com.qbitspark.buildwisebackend.globeadvice.exceptions.ItemNotFoundException;
import com.qbitspark.buildwisebackend.globeresponsebody.GlobeSuccessResponseBuilder;
import com.qbitspark.buildwisebackend.organisation_service.organisation_mng.entity.OrganisationEntity;
import com.qbitspark.buildwisebackend.organisation_service.organisation_mng.repo.OrganisationRepo;

import jakarta.validation.Valid;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/accounting/{organisationId}/budgets")
@RequiredArgsConstructor
public class OrgBudgetController {

    private final OrgBudgetService orgBudgetService;
    private final ChartOfAccountsRepo chartOfAccountsRepo;
    private final OrganisationRepo organisationRepo;

    @PostMapping
    public ResponseEntity<GlobeSuccessResponseBuilder> createBudget(
            @PathVariable UUID organisationId,
            @Valid @RequestBody CreateBudgetRequest request) throws ItemNotFoundException {

        OrgBudgetEntity createdBudget = orgBudgetService.createBudget(request, organisationId);
        CreateBudgetResponse response = mapToResponse(createdBudget);

        GlobeSuccessResponseBuilder successResponse = GlobeSuccessResponseBuilder.success(
                "Budget created successfully",
                response
        );

        return new ResponseEntity<>(successResponse, HttpStatus.CREATED);
    }

    @PutMapping("/{budgetId}/activate")
    public ResponseEntity<GlobeSuccessResponseBuilder> activateBudget(
            @PathVariable UUID organisationId,
            @PathVariable UUID budgetId) throws ItemNotFoundException {

        orgBudgetService.activateBudget(budgetId, organisationId);

        GlobeSuccessResponseBuilder successResponse = GlobeSuccessResponseBuilder.success(
                "Budget activated successfully. Previous active budget has been closed."
        );

        return new ResponseEntity<>(successResponse, HttpStatus.OK);
    }

    @GetMapping
    public ResponseEntity<GlobeSuccessResponseBuilder> getBudgets(
            @PathVariable UUID organisationId) throws ItemNotFoundException {

        List<OrgBudgetEntity> budgets = orgBudgetService.getBudgets(organisationId);

        List<BudgetListResponse> budgetResponses = budgets.stream()
                .map(this::mapToBudgetListResponse)
                .collect(Collectors.toList());

        GlobeSuccessResponseBuilder successResponse = GlobeSuccessResponseBuilder.success(
                "Budgets retrieved successfully",
                budgetResponses
        );

        return new ResponseEntity<>(successResponse, HttpStatus.OK);
    }

    @PutMapping("/{budgetId}")
    public ResponseEntity<GlobeSuccessResponseBuilder> updateBudget(
            @PathVariable UUID organisationId,
            @PathVariable UUID budgetId,
            @Valid @RequestBody UpdateBudgetRequest request) throws ItemNotFoundException {

        OrgBudgetEntity updatedBudget = orgBudgetService.updateBudget(budgetId, request, organisationId);
        CreateBudgetResponse response = mapToResponse(updatedBudget);

        GlobeSuccessResponseBuilder successResponse = GlobeSuccessResponseBuilder.success(
                "Budget updated successfully",
                response
        );

        return new ResponseEntity<>(successResponse, HttpStatus.OK);
    }

    @GetMapping("/{budgetId}/available-header-accounts")
    public ResponseEntity<GlobeSuccessResponseBuilder> getAvailableHeaderAccounts(
            @PathVariable UUID organisationId,
            @PathVariable UUID budgetId) throws ItemNotFoundException {

        orgBudgetService.getBudgetWithAccounts(budgetId, organisationId);

        List<HeaderAccountResponse> headerAccounts = getExpenseHeaderAccounts(organisationId);

        GlobeSuccessResponseBuilder successResponse = GlobeSuccessResponseBuilder.success(
                "Available header accounts retrieved successfully",
                headerAccounts
        );

        return new ResponseEntity<>(successResponse, HttpStatus.OK);
    }

    @PutMapping("/{budgetId}/distribute")
    public ResponseEntity<GlobeSuccessResponseBuilder> distributeBudget(
            @PathVariable UUID organisationId,
            @PathVariable UUID budgetId,
            @Valid @RequestBody DistributeBudgetRequest request) throws ItemNotFoundException {

        OrgBudgetEntity updatedBudget = orgBudgetService.distributeBudget(budgetId, request, organisationId);

        GlobeSuccessResponseBuilder successResponse = GlobeSuccessResponseBuilder.success(
                "Budget distributed to header accounts successfully",
                mapToResponse(updatedBudget)
        );

        return new ResponseEntity<>(successResponse, HttpStatus.OK);
    }

    @PostMapping("/{budgetId}/initialize-accounts")
    public ResponseEntity<GlobeSuccessResponseBuilder> initializeBudgetAccounts(
            @PathVariable UUID organisationId,
            @PathVariable UUID budgetId) throws ItemNotFoundException {

        orgBudgetService.initializeBudgetWithAccounts(budgetId, organisationId);

        GlobeSuccessResponseBuilder successResponse = GlobeSuccessResponseBuilder.success(
                "Budget initialized with header accounts successfully"
        );

        return new ResponseEntity<>(successResponse, HttpStatus.OK);
    }

    @GetMapping("/{budgetId}/summary")
    public ResponseEntity<GlobeSuccessResponseBuilder> getBudgetSummary(
            @PathVariable UUID organisationId,
            @PathVariable UUID budgetId) throws ItemNotFoundException {

        OrgBudgetSummaryResponse summary = orgBudgetService.getBudgetSummary(budgetId, organisationId);

        GlobeSuccessResponseBuilder successResponse = GlobeSuccessResponseBuilder.success(
                "Budget summary retrieved successfully",
                summary
        );

        return new ResponseEntity<>(successResponse, HttpStatus.OK);
    }

    @GetMapping("/{budgetId}/detailed")
    public ResponseEntity<GlobeSuccessResponseBuilder> getDetailedBudget(
            @PathVariable UUID organisationId,
            @PathVariable UUID budgetId) throws ItemNotFoundException {

        OrgBudgetEntity budget = orgBudgetService.getBudgetWithAccounts(budgetId, organisationId);
        OrgBudgetDetailedResponse response = mapToDetailedResponse(budget);

        GlobeSuccessResponseBuilder successResponse = GlobeSuccessResponseBuilder.success(
                "Detailed budget retrieved successfully",
                response
        );

        return new ResponseEntity<>(successResponse, HttpStatus.OK);
    }

    @GetMapping("/{budgetId}/accounts")
    public ResponseEntity<GlobeSuccessResponseBuilder> getBudgetAccounts(
            @PathVariable UUID organisationId,
            @PathVariable UUID budgetId) throws ItemNotFoundException {

        OrgBudgetEntity budget = orgBudgetService.getBudgetWithAccounts(budgetId, organisationId);

        List<OrgBudgetLineItemResponse> lineItemResponses = budget.getLineItems().stream()
                .map(this::mapToLineItemResponse)
                .collect(Collectors.toList());

        GlobeSuccessResponseBuilder successResponse = GlobeSuccessResponseBuilder.success(
                "Budget accounts retrieved successfully",
                lineItemResponses
        );

        return new ResponseEntity<>(successResponse, HttpStatus.OK);
    }

    // Mapping methods
    private CreateBudgetResponse mapToResponse(OrgBudgetEntity budget) {
        CreateBudgetResponse response = new CreateBudgetResponse();
        response.setBudgetId(budget.getBudgetId());
        response.setBudgetName(budget.getBudgetName());
        response.setFinancialYearStart(budget.getFinancialYearStart());
        response.setFinancialYearEnd(budget.getFinancialYearEnd());
        response.setTotalBudgetAmount(budget.getTotalBudgetAmount());
        response.setAllocatedAmount(budget.getDistributedAmount());
        response.setAvailableAmount(budget.getAvailableAmount());
        response.setStatus(budget.getStatus());
        response.setDescription(budget.getDescription());
        response.setCreatedDate(budget.getCreatedDate());
        return response;
    }

    private BudgetListResponse mapToBudgetListResponse(OrgBudgetEntity budget) {
        BudgetListResponse response = new BudgetListResponse();
        response.setBudgetId(budget.getBudgetId());
        response.setBudgetName(budget.getBudgetName());
        response.setFinancialYearStart(budget.getFinancialYearStart());
        response.setFinancialYearEnd(budget.getFinancialYearEnd());
        response.setTotalBudgetAmount(budget.getTotalBudgetAmount());
        response.setAllocatedAmount(budget.getDistributedAmount());
        response.setAvailableAmount(budget.getAvailableAmount());
        response.setStatus(budget.getStatus());
        response.setCreatedDate(budget.getCreatedDate());
        return response;
    }

    private OrgBudgetDetailedResponse mapToDetailedResponse(OrgBudgetEntity budget) {
        OrgBudgetDetailedResponse response = new OrgBudgetDetailedResponse();
        response.setBudgetId(budget.getBudgetId());
        response.setBudgetName(budget.getBudgetName());
        response.setFinancialYearStart(budget.getFinancialYearStart());
        response.setFinancialYearEnd(budget.getFinancialYearEnd());
        response.setTotalBudgetAmount(budget.getTotalBudgetAmount());
        response.setDistributedAmount(budget.getDistributedAmount());
        response.setAvailableAmount(budget.getAvailableAmount());
        response.setTotalSpentAmount(budget.getTotalSpentFromLineItems());
        response.setTotalCommittedAmount(budget.getTotalCommittedAmount());
        response.setTotalRemainingAmount(budget.getTotalRemainingAmount());
        response.setStatus(budget.getStatus());
        response.setDescription(budget.getDescription());
        response.setCreatedDate(budget.getCreatedDate());
        response.setModifiedDate(budget.getModifiedDate());

        response.setOrganisationId(budget.getOrganisation().getOrganisationId());
        response.setOrganisationName(budget.getOrganisation().getOrganisationName());

        response.setBudgetUtilizationPercentage(budget.getBudgetUtilizationPercentage());
        response.setSpendingPercentage(budget.getSpendingPercentage());
        response.setTotalAccounts(budget.getLineItems().size());
        response.setAccountsWithBudget((int) budget.getLineItemsWithBudgetCount());
        response.setAccountsWithoutBudget((int) budget.getLineItemsWithoutBudgetCount());

        response.setAccountGroups(createAccountGroups(budget));

        return response;
    }

    private OrgBudgetLineItemResponse mapToLineItemResponse(OrgBudgetLineItemEntity lineItem) {
        OrgBudgetLineItemResponse response = new OrgBudgetLineItemResponse();
        response.setLineItemId(lineItem.getLineItemId());
        response.setAccountId(lineItem.getChartOfAccount().getId());
        response.setAccountCode(lineItem.getChartOfAccount().getAccountCode());
        response.setAccountName(lineItem.getChartOfAccount().getName());
        response.setAccountDescription(lineItem.getChartOfAccount().getDescription());

        response.setBudgetAmount(lineItem.getBudgetAmount());
        response.setAllocatedToDetails(lineItem.getAllocatedToDetails());
        response.setAvailableForAllocation(lineItem.getAvailableForAllocation());

        response.setSpentAmount(lineItem.getSpentAmount());
        response.setCommittedAmount(lineItem.getCommittedAmount());
        response.setRemainingAmount(lineItem.getRemainingAmount());
        response.setLineItemNotes(lineItem.getLineItemNotes());
        response.setHasBudgetAllocated(lineItem.hasBudgetAllocated());
        response.setUtilizationPercentage(lineItem.getUtilizationPercentage());
        response.setCreatedDate(lineItem.getCreatedDate());
        response.setModifiedDate(lineItem.getModifiedDate());
        return response;
    }

    private List<OrgBudgetDetailedResponse.AccountGroupResponse> createAccountGroups(OrgBudgetEntity budget) {
        return budget.getLineItems().stream()
                .collect(Collectors.groupingBy(lineItem -> {
                    var parentId = lineItem.getChartOfAccount().getParentAccountId();
                    return parentId != null ? parentId.toString() : "No Parent";
                }))
                .values().stream()
                .map(groupLineItems -> {
                    var groupResponse = new OrgBudgetDetailedResponse.AccountGroupResponse();

                    if (!groupLineItems.isEmpty() && groupLineItems.get(0).getChartOfAccount().getParentAccountId() != null) {
                        groupResponse.setHeaderAccountCode("Header");
                        groupResponse.setHeaderAccountName("Account Group");
                    } else {
                        groupResponse.setHeaderAccountCode("UNGROUPED");
                        groupResponse.setHeaderAccountName("Ungrouped Accounts");
                    }

                    groupResponse.setGroupTotalBudget(
                            groupLineItems.stream()
                                    .map(OrgBudgetLineItemEntity::getBudgetAmount)
                                    .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add)
                    );

                    groupResponse.setGroupTotalSpent(
                            groupLineItems.stream()
                                    .map(OrgBudgetLineItemEntity::getSpentAmount)
                                    .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add)
                    );

                    groupResponse.setGroupTotalCommitted(
                            groupLineItems.stream()
                                    .map(OrgBudgetLineItemEntity::getCommittedAmount)
                                    .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add)
                    );

                    groupResponse.setGroupTotalRemaining(
                            groupLineItems.stream()
                                    .map(OrgBudgetLineItemEntity::getRemainingAmount)
                                    .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add)
                    );

                    groupResponse.setTotalAccounts(groupLineItems.size());
                    groupResponse.setAccountsWithBudget(
                            (int) groupLineItems.stream()
                                    .filter(OrgBudgetLineItemEntity::hasBudgetAllocated)
                                    .count()
                    );

                    groupResponse.setLineItems(
                            groupLineItems.stream()
                                    .map(this::mapToLineItemResponse)
                                    .collect(Collectors.toList())
                    );

                    return groupResponse;
                })
                .collect(Collectors.toList());
    }

    private List<HeaderAccountResponse> getExpenseHeaderAccounts(UUID organisationId) throws ItemNotFoundException {

        OrganisationEntity organisation = organisationRepo.findById(organisationId)
                .orElseThrow(() -> new ItemNotFoundException("Organisation not found"));

        List<ChartOfAccounts> headerAccounts = chartOfAccountsRepo
                .findByOrganisationAndAccountTypeAndIsActive(organisation, AccountType.EXPENSE, true)
                .stream()
                .filter(ChartOfAccounts::getIsHeader)
                .toList();

        return headerAccounts.stream()
                .map(this::mapToHeaderAccountResponse)
                .collect(Collectors.toList());
    }

    private HeaderAccountResponse mapToHeaderAccountResponse(ChartOfAccounts account) {
        HeaderAccountResponse response = new HeaderAccountResponse();
        response.setAccountId(account.getId());
        response.setAccountCode(account.getAccountCode());
        response.setAccountName(account.getName());
        response.setDescription(account.getDescription());

        List<ChartOfAccounts> childAccounts = chartOfAccountsRepo
                .findByOrganisationAndAccountTypeAndIsActive(
                        account.getOrganisation(), AccountType.EXPENSE, true)
                .stream()
                .filter(child -> account.getId().equals(child.getParentAccountId()))
                .toList();

        response.setHasChildren(!childAccounts.isEmpty());
        response.setChildAccountCount(childAccounts.size());

        return response;
    }


}