package com.qbitspark.buildwisebackend.accounting_service.budget_mng.org_budget.utitls;

import com.qbitspark.buildwisebackend.accounting_service.budget_mng.org_budget.entity.BudgetFundingAllocationEntity;
import com.qbitspark.buildwisebackend.accounting_service.budget_mng.org_budget.entity.BudgetSpendingEntity;
import com.qbitspark.buildwisebackend.accounting_service.budget_mng.org_budget.entity.OrgBudgetDetailDistributionEntity;
import com.qbitspark.buildwisebackend.accounting_service.budget_mng.org_budget.entity.OrgBudgetEntity;
import com.qbitspark.buildwisebackend.accounting_service.budget_mng.org_budget.enums.DetailAccountStatus;
import com.qbitspark.buildwisebackend.accounting_service.budget_mng.org_budget.paylaods.BudgetAllocationResponse;
import com.qbitspark.buildwisebackend.accounting_service.budget_mng.org_budget.paylaods.DetailAccountAllocationResponse;
import com.qbitspark.buildwisebackend.accounting_service.budget_mng.org_budget.paylaods.HeaderAccountAllocationResponse;
import com.qbitspark.buildwisebackend.accounting_service.budget_mng.org_budget.repo.BudgetFundingAllocationRepo;
import com.qbitspark.buildwisebackend.accounting_service.budget_mng.org_budget.repo.BudgetSpendingRepo;
import com.qbitspark.buildwisebackend.accounting_service.budget_mng.org_budget.repo.OrgBudgetDetailDistributionRepo;
import com.qbitspark.buildwisebackend.accounting_service.coa.entity.ChartOfAccounts;
import com.qbitspark.buildwisebackend.accounting_service.coa.enums.AccountType;
import com.qbitspark.buildwisebackend.accounting_service.coa.repo.ChartOfAccountsRepo;
import com.qbitspark.buildwisebackend.organisation_service.organisation_mng.entity.OrganisationEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BudgetAllocationResponseUtils {

    private final ChartOfAccountsRepo chartOfAccountsRepo;
    private final OrgBudgetDetailDistributionRepo detailDistributionRepo;
    private final BudgetFundingAllocationRepo budgetFundingAllocationRepo;
    private final BudgetSpendingRepo budgetSpendingRepo;

    public BudgetAllocationResponse buildBudgetAllocationResponse(OrgBudgetEntity budget, OrganisationEntity organisation) {
        BudgetAllocationResponse response = new BudgetAllocationResponse();

        // Basic budget info
        response.setBudgetId(budget.getBudgetId());
        response.setBudgetName(budget.getBudgetName());
        response.setBudgetStatus(budget.getStatus().toString());
        response.setFinancialYearStart(budget.getFinancialYearStart().toString());
        response.setFinancialYearEnd(budget.getFinancialYearEnd().toString());
        response.setCreatedAt(budget.getCreatedDate());

        // Get all expense accounts (headers and details)
        List<ChartOfAccounts> allExpenseAccounts = chartOfAccountsRepo
                .findByOrganisationAndAccountTypeAndIsActive(organisation, AccountType.EXPENSE, true);

        List<ChartOfAccounts> headerAccounts = allExpenseAccounts.stream()
                .filter(ChartOfAccounts::getIsHeader)
                .toList();

        List<ChartOfAccounts> detailAccounts = allExpenseAccounts.stream()
                .filter(account -> !account.getIsHeader())
                .collect(Collectors.toList());

        // Get distributions (budget allocations)
        List<OrgBudgetDetailDistributionEntity> distributions = detailDistributionRepo.findByBudget(budget);
        Map<UUID, OrgBudgetDetailDistributionEntity> distributionMap = distributions.stream()
                .collect(Collectors.toMap(
                        dist -> dist.getDetailAccount().getId(),
                        Function.identity()
                ));

        // Get funding allocations (receipt allocations)
        List<BudgetFundingAllocationEntity> fundingAllocations = budgetFundingAllocationRepo.findByBudget(budget);

        // Map for total funded amounts per account
        Map<UUID, BigDecimal> fundingMap = fundingAllocations.stream()
                .collect(Collectors.groupingBy(
                        funding -> funding.getAccount().getId(),
                        Collectors.mapping(
                                BudgetFundingAllocationEntity::getFundedAmount,
                                Collectors.reducing(BigDecimal.ZERO, BigDecimal::add)
                        )
                ));

        // Map for funding allocation entities (for getting allocation IDs and dates)
        Map<UUID, List<BudgetFundingAllocationEntity>> fundingAllocationMap = fundingAllocations.stream()
                .collect(Collectors.groupingBy(
                        funding -> funding.getAccount().getId()
                ));

        // Get spending amounts
        Map<UUID, BigDecimal> spendingMap = detailAccounts.stream()
                .collect(Collectors.toMap(
                        ChartOfAccounts::getId,
                        account -> {
                            List<BudgetSpendingEntity> spending = budgetSpendingRepo.findByAccount(account);
                            return spending.stream()
                                    .map(BudgetSpendingEntity::getSpentAmount)
                                    .reduce(BigDecimal.ZERO, BigDecimal::add);
                        }
                ));

        // Build header accounts with their details
        List<HeaderAccountAllocationResponse> headerResponses = headerAccounts.stream()
                .map(header -> buildHeaderAccountAllocationResponse(
                        header, detailAccounts, distributionMap, fundingMap, spendingMap, fundingAllocationMap))
                .collect(Collectors.toList());

        response.setHeaderAccounts(headerResponses);

        // Calculate totals
        BigDecimal totalBudgetAmount = distributions.stream()
                .map(OrgBudgetDetailDistributionEntity::getDistributedAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalSpentAmount = spendingMap.values().stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalFundedAmount = fundingMap.values().stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalRemainingAmount = totalFundedAmount.subtract(totalSpentAmount);
        BigDecimal availableForAllocation = totalBudgetAmount.subtract(totalFundedAmount);

        response.setTotalBudgetAmount(totalBudgetAmount);
        response.setTotalAllocatedToDetails(totalBudgetAmount);
        response.setTotalSpentAmount(totalSpentAmount);
        response.setTotalCommittedAmount(BigDecimal.ZERO);
        response.setTotalRemainingAmount(totalRemainingAmount);
        response.setAvailableForAllocation(availableForAllocation);

        // Calculate percentages
        if (totalBudgetAmount.compareTo(BigDecimal.ZERO) > 0) {
            response.setBudgetUtilizationPercentage(
                    totalFundedAmount.multiply(BigDecimal.valueOf(100))
                            .divide(totalBudgetAmount, 2, RoundingMode.HALF_UP));
            response.setSpendingPercentage(
                    totalSpentAmount.multiply(BigDecimal.valueOf(100))
                            .divide(totalBudgetAmount, 2, RoundingMode.HALF_UP));
        } else {
            response.setBudgetUtilizationPercentage(BigDecimal.ZERO);
            response.setSpendingPercentage(BigDecimal.ZERO);
        }

        // Set counts
        response.setTotalHeaderAccounts(headerAccounts.size());
        response.setHeadersWithBudget((int) headerAccounts.stream()
                .filter(header -> hasHeaderBudget(header, detailAccounts, distributionMap))
                .count());
        response.setHeadersWithoutBudget(headerAccounts.size() - response.getHeadersWithBudget());

        response.setTotalDetailAccounts(detailAccounts.size());
        response.setDetailsWithAllocation((int) detailAccounts.stream()
                .filter(detail -> distributionMap.containsKey(detail.getId()) &&
                        distributionMap.get(detail.getId()).getDistributedAmount().compareTo(BigDecimal.ZERO) > 0)
                .count());
        response.setDetailsWithoutAllocation(detailAccounts.size() - response.getDetailsWithAllocation());

        return response;
    }

    private HeaderAccountAllocationResponse buildHeaderAccountAllocationResponse(
            ChartOfAccounts header,
            List<ChartOfAccounts> allDetailAccounts,
            Map<UUID, OrgBudgetDetailDistributionEntity> distributionMap,
            Map<UUID, BigDecimal> fundingMap,
            Map<UUID, BigDecimal> spendingMap,
            Map<UUID, List<BudgetFundingAllocationEntity>> fundingAllocationMap) {

        HeaderAccountAllocationResponse headerResponse = new HeaderAccountAllocationResponse();
        headerResponse.setHeaderLineItemId(header.getId());
        headerResponse.setHeaderAccountId(header.getId());
        headerResponse.setHeaderAccountCode(header.getAccountCode());
        headerResponse.setHeaderAccountName(header.getName());
        headerResponse.setHeaderDescription(header.getDescription());

        // Get detail accounts under this header
        List<ChartOfAccounts> detailsUnderHeader = allDetailAccounts.stream()
                .filter(detail -> header.getId().equals(detail.getParentAccountId()))
                .toList();

        // Build detail account responses
        List<DetailAccountAllocationResponse> detailResponses = detailsUnderHeader.stream()
                .map(detail -> buildDetailAccountAllocationResponse(
                        detail, distributionMap, fundingMap, spendingMap, fundingAllocationMap))
                .collect(Collectors.toList());

        headerResponse.setDetailAccounts(detailResponses);

        // Calculate header totals
        BigDecimal headerBudgetAmount = detailsUnderHeader.stream()
                .map(detail -> distributionMap.get(detail.getId()))
                .filter(Objects::nonNull)
                .map(OrgBudgetDetailDistributionEntity::getDistributedAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal headerSpentAmount = detailsUnderHeader.stream()
                .map(detail -> spendingMap.getOrDefault(detail.getId(), BigDecimal.ZERO))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal headerFundedAmount = detailsUnderHeader.stream()
                .map(detail -> fundingMap.getOrDefault(detail.getId(), BigDecimal.ZERO))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        headerResponse.setHeaderBudgetAmount(headerBudgetAmount);
        headerResponse.setHeaderAllocatedToDetails(headerBudgetAmount);
        headerResponse.setHeaderSpentAmount(headerSpentAmount);
        headerResponse.setHeaderCommittedAmount(BigDecimal.ZERO);
        headerResponse.setHeaderRemainingAmount(headerFundedAmount.subtract(headerSpentAmount));
        headerResponse.setHeaderAvailableForAllocation(headerBudgetAmount.subtract(headerFundedAmount));

        // Calculate percentages
        if (headerBudgetAmount.compareTo(BigDecimal.ZERO) > 0) {
            headerResponse.setHeaderUtilizationPercentage(
                    headerFundedAmount.multiply(BigDecimal.valueOf(100))
                            .divide(headerBudgetAmount, 2, RoundingMode.HALF_UP));
            headerResponse.setHeaderAllocationPercentage(BigDecimal.valueOf(100));
        } else {
            headerResponse.setHeaderUtilizationPercentage(BigDecimal.ZERO);
            headerResponse.setHeaderAllocationPercentage(BigDecimal.ZERO);
        }

        headerResponse.setHasBudgetAllocated(headerBudgetAmount.compareTo(BigDecimal.ZERO) > 0);
        headerResponse.setDetailAccountCount(detailsUnderHeader.size());
        headerResponse.setDetailsWithAllocation((int) detailsUnderHeader.stream()
                .filter(detail -> distributionMap.containsKey(detail.getId()) &&
                        distributionMap.get(detail.getId()).getDistributedAmount().compareTo(BigDecimal.ZERO) > 0)
                .count());
        headerResponse.setDetailsWithoutAllocation(detailsUnderHeader.size() - headerResponse.getDetailsWithAllocation());

        return headerResponse;
    }

    private DetailAccountAllocationResponse buildDetailAccountAllocationResponse(
            ChartOfAccounts detail,
            Map<UUID, OrgBudgetDetailDistributionEntity> distributionMap,
            Map<UUID, BigDecimal> fundingMap,
            Map<UUID, BigDecimal> spendingMap,
            Map<UUID, List<BudgetFundingAllocationEntity>> fundingAllocationMap) {

        DetailAccountAllocationResponse detailResponse = new DetailAccountAllocationResponse();
        detailResponse.setDetailAccountId(detail.getId());
        detailResponse.setDetailAccountCode(detail.getAccountCode());
        detailResponse.setDetailAccountName(detail.getName());
        detailResponse.setDetailDescription(detail.getDescription());

        OrgBudgetDetailDistributionEntity distribution = distributionMap.get(detail.getId());
        List<BudgetFundingAllocationEntity> fundingAllocations = fundingAllocationMap.get(detail.getId());

        BigDecimal allocatedAmount = distribution != null ? distribution.getDistributedAmount() : BigDecimal.ZERO;
        BigDecimal fundedAmount = fundingMap.getOrDefault(detail.getId(), BigDecimal.ZERO);
        BigDecimal spentAmount = spendingMap.getOrDefault(detail.getId(), BigDecimal.ZERO);

        // Get the most recent funding allocation for allocation ID and date
        BudgetFundingAllocationEntity latestFundingAllocation = null;
        if (fundingAllocations != null && !fundingAllocations.isEmpty()) {
            latestFundingAllocation = fundingAllocations.stream()
                    .max(Comparator.comparing(BudgetFundingAllocationEntity::getFundedDate))
                    .orElse(null);
        }

        // Set allocation ID from the latest funding allocation
        detailResponse.setAllocationId(latestFundingAllocation != null ? latestFundingAllocation.getFundingId() : null);
        detailResponse.setAllocatedAmount(allocatedAmount);
        detailResponse.setSpentAmount(spentAmount);
        detailResponse.setCommittedAmount(BigDecimal.ZERO);
        detailResponse.setBudgetRemaining(fundedAmount.subtract(spentAmount));

        detailResponse.setAllocationStatus(determineDetailAccountStatus(allocatedAmount, fundedAmount, spentAmount));
        detailResponse.setHasAllocation(allocatedAmount.compareTo(BigDecimal.ZERO) > 0);
        detailResponse.setNotes(distribution != null ? distribution.getDescription() : "");

        // Calculate utilization percentage
        if (allocatedAmount.compareTo(BigDecimal.ZERO) > 0) {
            detailResponse.setUtilizationPercentage(
                    spentAmount.multiply(BigDecimal.valueOf(100))
                            .divide(allocatedAmount, 2, RoundingMode.HALF_UP));
        } else {
            detailResponse.setUtilizationPercentage(BigDecimal.ZERO);
        }

        // Set allocation dates from the latest funding allocation
        if (latestFundingAllocation != null) {
            detailResponse.setAllocationCreatedDate(latestFundingAllocation.getFundedDate());
            detailResponse.setAllocationModifiedDate(null); // You can add modification tracking
        }

        return detailResponse;
    }

    private DetailAccountStatus determineDetailAccountStatus(BigDecimal allocated, BigDecimal funded, BigDecimal spent) {
        if (allocated.compareTo(BigDecimal.ZERO) == 0) {
            return DetailAccountStatus.UNALLOCATED;
        } else if (funded.compareTo(BigDecimal.ZERO) == 0) {
            return DetailAccountStatus.AWAITING_FUNDING;
        } else if (spent.compareTo(funded) > 0) {
            return DetailAccountStatus.OVERSPENT;
        } else if (spent.compareTo(funded) == 0) {
            return DetailAccountStatus.FULLY_SPENT;
        } else if (spent.compareTo(BigDecimal.ZERO) > 0) {
            return DetailAccountStatus.PARTIALLY_SPENT;
        } else {
            return DetailAccountStatus.ACTIVE;
        }
    }

    private boolean hasHeaderBudget(ChartOfAccounts header, List<ChartOfAccounts> detailAccounts,
                                    Map<UUID, OrgBudgetDetailDistributionEntity> distributionMap) {
        return detailAccounts.stream()
                .filter(detail -> header.getId().equals(detail.getParentAccountId()))
                .anyMatch(detail -> distributionMap.containsKey(detail.getId()) &&
                        distributionMap.get(detail.getId()).getDistributedAmount().compareTo(BigDecimal.ZERO) > 0);
    }
}