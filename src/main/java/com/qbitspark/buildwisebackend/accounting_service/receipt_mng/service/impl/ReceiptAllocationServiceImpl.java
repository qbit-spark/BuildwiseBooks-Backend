package com.qbitspark.buildwisebackend.accounting_service.receipt_mng.service.impl;

import com.qbitspark.buildwisebackend.accounting_service.budget_mng.org_budget.entity.BudgetFundingAllocationEntity;
import com.qbitspark.buildwisebackend.accounting_service.budget_mng.org_budget.entity.OrgBudgetDetailDistributionEntity;
import com.qbitspark.buildwisebackend.accounting_service.budget_mng.org_budget.entity.OrgBudgetEntity;
import com.qbitspark.buildwisebackend.accounting_service.budget_mng.org_budget.enums.OrgBudgetStatus;
import com.qbitspark.buildwisebackend.accounting_service.budget_mng.org_budget.paylaods.CreateReceiptAllocationRequest;
import com.qbitspark.buildwisebackend.accounting_service.budget_mng.org_budget.paylaods.ReceiptAllocationResponse;
import com.qbitspark.buildwisebackend.accounting_service.budget_mng.org_budget.repo.BudgetFundingAllocationRepo;
import com.qbitspark.buildwisebackend.accounting_service.budget_mng.org_budget.repo.OrgBudgetDetailDistributionRepo;
import com.qbitspark.buildwisebackend.accounting_service.budget_mng.org_budget.repo.OrgBudgetRepo;
import com.qbitspark.buildwisebackend.accounting_service.budget_mng.org_budget.service.BudgetFundingService;
import com.qbitspark.buildwisebackend.accounting_service.coa.entity.ChartOfAccounts;
import com.qbitspark.buildwisebackend.accounting_service.coa.repo.ChartOfAccountsRepo;
import com.qbitspark.buildwisebackend.accounting_service.documentflow.invoice.enums.ActionType;
import com.qbitspark.buildwisebackend.accounting_service.receipt_mng.entity.ReceiptAllocationDetailEntity;
import com.qbitspark.buildwisebackend.accounting_service.receipt_mng.entity.ReceiptAllocationEntity;
import com.qbitspark.buildwisebackend.accounting_service.receipt_mng.entity.ReceiptEntity;
import com.qbitspark.buildwisebackend.accounting_service.receipt_mng.enums.AllocationStatus;
import com.qbitspark.buildwisebackend.accounting_service.receipt_mng.enums.ReceiptStatus;
import com.qbitspark.buildwisebackend.accounting_service.receipt_mng.payload.ReceiptAllocationSummaryResponse;
import com.qbitspark.buildwisebackend.accounting_service.receipt_mng.repo.ReceiptAllocationDetailRepo;
import com.qbitspark.buildwisebackend.accounting_service.receipt_mng.repo.ReceiptAllocationRepo;
import com.qbitspark.buildwisebackend.accounting_service.receipt_mng.repo.ReceiptRepo;
import com.qbitspark.buildwisebackend.accounting_service.receipt_mng.service.ReceiptAllocationService;
import com.qbitspark.buildwisebackend.approval_service.enums.ServiceType;
import com.qbitspark.buildwisebackend.approval_service.service.ApprovalIntegrationService;
import com.qbitspark.buildwisebackend.approval_service.service.ApprovalWorkflowService;
import com.qbitspark.buildwisebackend.authentication_service.Repository.AccountRepo;
import com.qbitspark.buildwisebackend.authentication_service.entity.AccountEntity;
import com.qbitspark.buildwisebackend.globeadvice.exceptions.AccessDeniedException;
import com.qbitspark.buildwisebackend.globeadvice.exceptions.ItemNotFoundException;
import com.qbitspark.buildwisebackend.organisation_service.organisation_mng.entity.OrganisationEntity;
import com.qbitspark.buildwisebackend.organisation_service.organisation_mng.repo.OrganisationRepo;
import com.qbitspark.buildwisebackend.organisation_service.orgnisation_members_mng.entities.OrganisationMember;
import com.qbitspark.buildwisebackend.organisation_service.orgnisation_members_mng.enums.MemberStatus;
import com.qbitspark.buildwisebackend.organisation_service.orgnisation_members_mng.repo.OrganisationMemberRepo;
import com.qbitspark.buildwisebackend.organisation_service.roles_mng.service.PermissionCheckerService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class ReceiptAllocationServiceImpl implements ReceiptAllocationService {

    private final AccountRepo accountRepo;
    private final OrganisationMemberRepo organisationMemberRepo;
    private final PermissionCheckerService permissionChecker;
    private final ReceiptAllocationRepo receiptAllocationRepo;
    private final ReceiptAllocationDetailRepo receiptAllocationDetailRepo;
    private final ChartOfAccountsRepo chartOfAccountsRepo;
    private final ReceiptRepo receiptRepo;
    private final OrganisationRepo organisationRepo;
    private final OrgBudgetDetailDistributionRepo detailDistributionRepo;
    private final BudgetFundingAllocationRepo budgetFundingAllocationRepo;
    private final OrgBudgetRepo orgBudgetRepo;
    private final BudgetFundingService budgetFundingService;
    private final ApprovalIntegrationService approvalIntegrationService;
    private final ApprovalWorkflowService approvalWorkflowService;

    @Override
    public ReceiptAllocationEntity createReceiptAllocation(UUID organisationId, CreateReceiptAllocationRequest request, ActionType action)
            throws ItemNotFoundException, AccessDeniedException {

        AccountEntity currentUser = getAuthenticatedAccount();
        OrganisationEntity organisation = getOrganisation(organisationId);
        OrganisationMember member = validateOrganisationMemberAccess(currentUser, organisation);

        permissionChecker.checkMemberPermission(member, "RECEIPTS", "createAllocation");

        ReceiptEntity receipt = receiptRepo.findByReceiptIdAndOrganisation(request.getReceiptId(), organisation)
                .orElseThrow(() -> new ItemNotFoundException("Receipt not found"));

        if (receipt.getStatus() != ReceiptStatus.APPROVED) {
            throw new ItemNotFoundException("Only approved receipts can be allocated");
        }

        // Calculate the already allocated amount (from APPROVED + PENDING allocations)
        List<ReceiptAllocationEntity> existingAllocations = receiptAllocationRepo.findByReceipt(receipt);
        BigDecimal totalAlreadyAllocated = existingAllocations.stream()
                .filter(allocation -> allocation.getStatus() == AllocationStatus.APPROVED ||
                        allocation.getStatus() == AllocationStatus.PENDING_APPROVAL)
                .map(ReceiptAllocationEntity::getTotalAllocatedAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Calculate requested allocation total
        BigDecimal requestedTotal = request.getAllocationDetails().stream()
                .map(CreateReceiptAllocationRequest.AllocationDetailRequest::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Check if we would over-allocate the receipt
        BigDecimal availableAmount = receipt.getTotalAmount().subtract(totalAlreadyAllocated);
        if (requestedTotal.compareTo(availableAmount) > 0) {
            throw new ItemNotFoundException(String.format(
                    "Cannot allocate %s. Receipt total: %s, Already allocated (approved + pending): %s, Available: %s",
                    requestedTotal, receipt.getTotalAmount(), totalAlreadyAllocated, availableAmount
            ));
        }

        // Validate allocation details
        validateAllocationDetails(request.getAllocationDetails(), organisation);

        // Check budget availability for each account before creating allocation
        validateBudgetAvailability(request.getAllocationDetails(), organisationId);

        // Create an allocation entity
        ReceiptAllocationEntity allocation = new ReceiptAllocationEntity();
        allocation.setReceipt(receipt);
        allocation.setNotes(request.getNotes());
        allocation.setStatus(AllocationStatus.DRAFT);
        allocation.setRequestedBy(currentUser.getAccountId());
        allocation.setRequestedAt(LocalDateTime.now());

        // Create allocation details BEFORE saving the main allocation
        List<ReceiptAllocationDetailEntity> detailEntities = new ArrayList<>();
        for (CreateReceiptAllocationRequest.AllocationDetailRequest detailRequest : request.getAllocationDetails()) {
            ChartOfAccounts account = chartOfAccountsRepo.findById(detailRequest.getAccountId())
                    .orElseThrow(() -> new ItemNotFoundException("Account not found"));

            ReceiptAllocationDetailEntity detail = new ReceiptAllocationDetailEntity();
            detail.setAllocation(allocation);
            detail.setAccount(account);
            detail.setAllocatedAmount(detailRequest.getAmount());
            detail.setDescription(detailRequest.getDescription());
            detail.setCreatedBy(currentUser.getAccountId());

            detailEntities.add(detail);
            allocation.addAllocationDetail(detail); // This sets the bidirectional relationship
        }

        // Save the allocation with details (cascade will save details)
        ReceiptAllocationEntity savedAllocation = receiptAllocationRepo.save(allocation);

        // Explicitly flush to ensure data is persisted
        receiptAllocationRepo.flush();


        if (action == ActionType.SAVE_AND_APPROVAL) {
            // 1. Update document status via integration service
            approvalIntegrationService.submitForApproval(
                    ServiceType.RECEIPT_ALLOCATIONS_TO_BUDGET,
                    savedAllocation.getAllocationId(),
                    organisationId,
                    null
            );

            // 2. Start the workflow directly
            approvalWorkflowService.startApprovalWorkflow(
                    ServiceType.RECEIPT_ALLOCATIONS_TO_BUDGET,
                    savedAllocation.getAllocationId(),
                    organisationId,
                    null
            );
        }

        return savedAllocation;
    }


    @Override
    @Transactional(readOnly = true)
    public Page<ReceiptAllocationSummaryResponse> getAllocations(UUID organisationId, AllocationStatus status,
                                                                 int page, int size) throws ItemNotFoundException, AccessDeniedException {

        AccountEntity currentUser = getAuthenticatedAccount();

        OrganisationEntity organisation = organisationRepo.findById(organisationId)
                .orElseThrow(() -> new ItemNotFoundException("Organisation not found"));

        OrganisationMember member = validateOrganisationMemberAccess(currentUser, organisation);

        permissionChecker.checkMemberPermission(member, "RECEIPTS", "viewAllocations");

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<ReceiptAllocationEntity> allocationsPage;
        if (status != null) {
            allocationsPage = receiptAllocationRepo.findAllByReceiptOrganisationAndStatus(organisation, status, pageable);
        } else {
            allocationsPage = receiptAllocationRepo.findAllByReceiptOrganisation(organisation, pageable);
        }

        return allocationsPage.map(this::mapToSummaryResponse);
    }


    @Override
    @Transactional(readOnly = true)
    public ReceiptAllocationResponse getAllocationDetails(UUID organisationId, UUID allocationId)
            throws ItemNotFoundException, AccessDeniedException {

        AccountEntity currentUser = getAuthenticatedAccount();

        OrganisationEntity organisation = organisationRepo.findById(organisationId)
                .orElseThrow(() -> new ItemNotFoundException("Organisation not found"));

        OrganisationMember member = validateOrganisationMemberAccess(currentUser, organisation);

        permissionChecker.checkMemberPermission(member, "RECEIPTS", "viewAllocations");

        ReceiptAllocationEntity allocation = receiptAllocationRepo
                .findByAllocationIdAndReceiptOrganisation(allocationId, organisation)
                .orElseThrow(() -> new ItemNotFoundException("Allocation not found"));

        return mapToDetailedResponse(allocation);
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

    private OrganisationMember validateOrganisationMemberAccess(AccountEntity account, OrganisationEntity organisation) throws ItemNotFoundException {
        OrganisationMember member = organisationMemberRepo.findByAccountAndOrganisation(account, organisation)
                .orElseThrow(() -> new ItemNotFoundException("Member is not belong to this organisation"));

        if (member.getStatus() != MemberStatus.ACTIVE) {
            throw new ItemNotFoundException("Member is not active");
        }

        return member;
    }


    // Helper methods
    private OrganisationEntity getOrganisation(UUID organisationId) throws ItemNotFoundException {
        return organisationRepo.findById(organisationId)
                .orElseThrow(() -> new ItemNotFoundException("Organisation not found"));
    }


    private void validateAllocationDetails(List<CreateReceiptAllocationRequest.AllocationDetailRequest> details,
                                           OrganisationEntity organisation) throws ItemNotFoundException {
        if (details == null || details.isEmpty()) {
            throw new ItemNotFoundException("Allocation details cannot be empty");
        }

        for (CreateReceiptAllocationRequest.AllocationDetailRequest detail : details) {
            ChartOfAccounts account = chartOfAccountsRepo.findById(detail.getAccountId())
                    .orElseThrow(() -> new ItemNotFoundException("Account not found: " + detail.getAccountId()));

            // Validate account belongs to an organisation
            if (!account.getOrganisation().getOrganisationId().equals(organisation.getOrganisationId())) {
                throw new ItemNotFoundException("Account does not belong to this organisation");
            }

            // Validate account is active
            if (!account.getIsActive()) {
                throw new ItemNotFoundException("Account is not active: " + account.getAccountCode());
            }

            // Validate amount is positive
            if (detail.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
                throw new ItemNotFoundException("Amount must be positive for account: " + account.getAccountCode());
            }
        }
    }


    private void validateBudgetAvailability(List<CreateReceiptAllocationRequest.AllocationDetailRequest> details,
                                            UUID organisationId) throws ItemNotFoundException {

        // Find active budget for organisation
        OrganisationEntity organisation = organisationRepo.findById(organisationId)
                .orElseThrow(() -> new ItemNotFoundException("Organisation not found"));

        OrgBudgetEntity activeBudget = orgBudgetRepo.findByOrganisationAndStatus(organisation, OrgBudgetStatus.ACTIVE)
                .orElseThrow(() -> new ItemNotFoundException("No active budget found for organisation"));

        for (CreateReceiptAllocationRequest.AllocationDetailRequest detail : details) {
            ChartOfAccounts account = chartOfAccountsRepo.findById(detail.getAccountId())
                    .orElseThrow(() -> new ItemNotFoundException("Account not found"));

            // Check if an account exists in budget distribution
            List<OrgBudgetDetailDistributionEntity> distributions = detailDistributionRepo
                    .findByBudgetAndDetailAccount(activeBudget, account);

            if (distributions.isEmpty()) {
                throw new ItemNotFoundException(String.format(
                        "Account %s (%s) is not included in the current budget. " +
                                "Cannot create allocation for accounts not in budget.",
                        account.getAccountCode(), account.getName()));
            }

            OrgBudgetDetailDistributionEntity distribution = distributions.get(0);

            // Check if an account has budget allocated
            if (distribution.getDistributedAmount().compareTo(BigDecimal.ZERO) == 0) {
                throw new ItemNotFoundException(String.format(
                        "Account %s (%s) has no budget allocated. " +
                                "Cannot create allocation for accounts with zero budget.",
                        account.getAccountCode(), account.getName()));
            }

            // Calculate available budget (this is the key check!)
            List<BudgetFundingAllocationEntity> existingFundings = budgetFundingAllocationRepo
                    .findByBudgetAndAccount(activeBudget, account);

            BigDecimal totalAlreadyFunded = existingFundings.stream()
                    .map(BudgetFundingAllocationEntity::getFundedAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal availableBudget = distribution.getDistributedAmount().subtract(totalAlreadyFunded);

            if (detail.getAmount().compareTo(availableBudget) > 0) {
                throw new ItemNotFoundException(String.format(
                        "Insufficient budget for account %s (%s). " +
                                "Requested: %s, Budget allocated: %s, Already funded: %s, Available: %s",
                        account.getAccountCode(), account.getName(),
                        detail.getAmount(), distribution.getDistributedAmount(),
                        totalAlreadyFunded, availableBudget
                ));
            }
        }
    }

    private ReceiptAllocationSummaryResponse mapToSummaryResponse(ReceiptAllocationEntity allocation) {
        ReceiptAllocationSummaryResponse response = new ReceiptAllocationSummaryResponse();
        response.setAllocationId(allocation.getAllocationId());
        response.setReceiptId(allocation.getReceipt().getReceiptId());
        response.setReceiptNumber(allocation.getReceipt().getReceiptNumber());
        response.setReceiptAmount(allocation.getReceipt().getTotalAmount());
        response.setStatus(allocation.getStatus());
        response.setNotes(allocation.getNotes());
        response.setTotalAllocatedAmount(allocation.getTotalAllocatedAmount());
        response.setFullyAllocated(allocation.isFullyAllocated());
        response.setRequestedBy(allocation.getRequestedBy());
        response.setRequestedAt(allocation.getRequestedAt());
        response.setCreatedAt(allocation.getCreatedAt());


        response.setTotalAllocationDetails(
                allocation.getAllocationDetails() != null ?
                        allocation.getAllocationDetails().size() : 0
        );

        return response;
    }


    private ReceiptAllocationResponse mapToDetailedResponse(ReceiptAllocationEntity allocation) {
        ReceiptAllocationResponse response = new ReceiptAllocationResponse();
        response.setAllocationId(allocation.getAllocationId());
        response.setReceiptId(allocation.getReceipt().getReceiptId());
        response.setReceiptNumber(allocation.getReceipt().getReceiptNumber());
        response.setReceiptAmount(allocation.getReceipt().getTotalAmount());
        response.setStatus(allocation.getStatus());
        response.setNotes(allocation.getNotes());
        response.setTotalAllocatedAmount(allocation.getTotalAllocatedAmount());
        response.setRemainingAmount(allocation.getRemainingAmount());
        response.setFullyAllocated(allocation.isFullyAllocated());
        response.setRequestedBy(allocation.getRequestedBy());
        response.setCreatedAt(allocation.getCreatedAt());


        // Map allocation details
        if (allocation.getAllocationDetails() != null) {
            List<ReceiptAllocationResponse.AllocationDetailResponse> detailResponses =
                    allocation.getAllocationDetails().stream()
                            .map(detail -> {
                                ReceiptAllocationResponse.AllocationDetailResponse detailResponse =
                                        new ReceiptAllocationResponse.AllocationDetailResponse();
                                detailResponse.setDetailId(detail.getDetailId());
                                detailResponse.setAccountId(detail.getAccount().getId());
                                detailResponse.setAccountCode(detail.getAccount().getAccountCode());
                                detailResponse.setAccountName(detail.getAccount().getName());
                                detailResponse.setAllocatedAmount(detail.getAllocatedAmount());
                                detailResponse.setDescription(detail.getDescription());
                                return detailResponse;
                            })
                            .collect(Collectors.toList());

            response.setAllocationDetails(detailResponses);
        }

        return response;
    }
}