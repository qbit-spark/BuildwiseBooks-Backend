package com.qbitspark.buildwisebackend.accounting_service.receipt_mng.service.impl;

import com.qbitspark.buildwisebackend.accounting_service.budget_mng.org_budget.entity.OrgBudgetDetailAllocationEntity;
import com.qbitspark.buildwisebackend.accounting_service.budget_mng.org_budget.repo.OrgBudgetDetailAllocationRepo;
import com.qbitspark.buildwisebackend.accounting_service.receipt_mng.entity.ReceiptAllocationDetailEntity;
import com.qbitspark.buildwisebackend.accounting_service.receipt_mng.entity.ReceiptAllocationEntity;
import com.qbitspark.buildwisebackend.accounting_service.receipt_mng.entity.ReceiptEntity;
import com.qbitspark.buildwisebackend.accounting_service.receipt_mng.payload.CreateReceiptAllocationRequest;
import com.qbitspark.buildwisebackend.accounting_service.receipt_mng.repo.ReceiptAllocationDetailRepo;
import com.qbitspark.buildwisebackend.accounting_service.receipt_mng.repo.ReceiptAllocationRepo;
import com.qbitspark.buildwisebackend.accounting_service.receipt_mng.repo.ReceiptRepo;
import com.qbitspark.buildwisebackend.accounting_service.receipt_mng.service.ReceiptAllocationService;
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
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class ReceiptAllocationServiceImpl implements ReceiptAllocationService {

    private final ReceiptAllocationRepo receiptAllocationRepo;
    private final ReceiptAllocationDetailRepo allocationDetailRepo;
    private final ReceiptRepo receiptRepo;
    private final OrgBudgetDetailAllocationRepo budgetDetailAllocationRepo;
    private final OrganisationRepo organisationRepo;
    private final OrganisationMemberRepo organisationMemberRepo;
    private final AccountRepo accountRepo;
    private final PermissionCheckerService permissionChecker;

    @Override
    public ReceiptAllocationEntity createAllocation(UUID organisationId, CreateReceiptAllocationRequest request)
            throws ItemNotFoundException, AccessDeniedException {

        AccountEntity currentUser = getAuthenticatedAccount();
        OrganisationEntity organisation = getOrganisation(organisationId);
        OrganisationMember member = validateOrganisationMemberAccess(currentUser, organisation);


        permissionChecker.checkMemberPermission(member, "RECEIPTS", "createReceipt");


        ReceiptEntity receipt = receiptRepo.findByReceiptIdAndOrganisation(request.getReceiptId(), organisation)
                .orElseThrow(() -> new ItemNotFoundException("Receipt not found"));

        BigDecimal totalAllocationAmount = request.getDetailAllocations().stream()
                .map(CreateReceiptAllocationRequest.DetailAllocationRequest::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal availableAmount = receipt.getRemainingAmountForAllocation();
        if (totalAllocationAmount.compareTo(availableAmount) > 0) {
            throw new IllegalArgumentException("Total allocation amount cannot exceed available receipt amount");
        }

        ReceiptAllocationEntity allocation = new ReceiptAllocationEntity();
        allocation.setReceipt(receipt);
        allocation.setNotes(request.getNotes());
        allocation.setRequestedBy(currentUser.getAccountId());

        ReceiptAllocationEntity savedAllocation = receiptAllocationRepo.save(allocation);

        for (CreateReceiptAllocationRequest.DetailAllocationRequest detailRequest : request.getDetailAllocations()) {
            OrgBudgetDetailAllocationEntity budgetDetail = budgetDetailAllocationRepo
                    .findById(detailRequest.getDetailAccountId())
                    .orElseThrow(() -> new ItemNotFoundException("Budget detail allocation not found"));

            ReceiptAllocationDetailEntity detail = new ReceiptAllocationDetailEntity();
            detail.setAllocation(savedAllocation);
            detail.setDetailAccount(budgetDetail);
            detail.setAmount(detailRequest.getAmount());
            detail.setDescription(detailRequest.getDescription());
            detail.setCreatedBy(currentUser.getAccountId());

            allocationDetailRepo.save(detail);
        }

        return receiptAllocationRepo.findById(savedAllocation.getAllocationId()).orElse(savedAllocation);
    }

    @Override
    public ReceiptAllocationEntity updateAllocation(UUID organisationId, UUID allocationId,
                                                    CreateReceiptAllocationRequest request) throws ItemNotFoundException, AccessDeniedException {

        AccountEntity currentUser = getAuthenticatedAccount();
        OrganisationEntity organisation = getOrganisation(organisationId);
        OrganisationMember member = validateOrganisationMemberAccess(currentUser, organisation);

        permissionChecker.checkMemberPermission(member, "RECEIPTS", "updateReceipt");

        ReceiptAllocationEntity allocation = receiptAllocationRepo.findById(allocationId)
                .orElseThrow(() -> new ItemNotFoundException("Allocation not found"));

        if (!allocation.getReceipt().getOrganisation().getOrganisationId().equals(organisationId)) {
            throw new ItemNotFoundException("Allocation not found in this organisation");
        }

        allocationDetailRepo.deleteByAllocation(allocation);

        allocation.setNotes(request.getNotes());

        for (CreateReceiptAllocationRequest.DetailAllocationRequest detailRequest : request.getDetailAllocations()) {
            OrgBudgetDetailAllocationEntity budgetDetail = budgetDetailAllocationRepo
                    .findById(detailRequest.getDetailAccountId())
                    .orElseThrow(() -> new ItemNotFoundException("Budget detail allocation not found"));

            ReceiptAllocationDetailEntity detail = new ReceiptAllocationDetailEntity();
            detail.setAllocation(allocation);
            detail.setDetailAccount(budgetDetail);
            detail.setAmount(detailRequest.getAmount());
            detail.setDescription(detailRequest.getDescription());
            detail.setCreatedBy(currentUser.getAccountId());

            allocationDetailRepo.save(detail);
        }

        return receiptAllocationRepo.save(allocation);
    }

    @Override
    public void deleteAllocation(UUID organisationId, UUID allocationId)
            throws ItemNotFoundException, AccessDeniedException {

        AccountEntity currentUser = getAuthenticatedAccount();
        OrganisationEntity organisation = getOrganisation(organisationId);
        OrganisationMember member = validateOrganisationMemberAccess(currentUser, organisation);

        permissionChecker.checkMemberPermission(member, "RECEIPTS", "deleteReceipt");

        ReceiptAllocationEntity allocation = receiptAllocationRepo.findById(allocationId)
                .orElseThrow(() -> new ItemNotFoundException("Allocation not found"));

        if (!allocation.getReceipt().getOrganisation().getOrganisationId().equals(organisationId)) {
            throw new ItemNotFoundException("Allocation not found in this organisation");
        }

        receiptAllocationRepo.delete(allocation);
    }

    @Override
    public ReceiptAllocationEntity getAllocationById(UUID organisationId, UUID allocationId)
            throws ItemNotFoundException, AccessDeniedException {

        AccountEntity currentUser = getAuthenticatedAccount();
        OrganisationEntity organisation = getOrganisation(organisationId);
        OrganisationMember member = validateOrganisationMemberAccess(currentUser, organisation);

        permissionChecker.checkMemberPermission(member, "RECEIPTS", "viewReceipts");

        ReceiptAllocationEntity allocation = receiptAllocationRepo.findById(allocationId)
                .orElseThrow(() -> new ItemNotFoundException("Allocation not found"));

        if (!allocation.getReceipt().getOrganisation().getOrganisationId().equals(organisationId)) {
            throw new ItemNotFoundException("Allocation not found in this organisation");
        }

        return allocation;
    }

    @Override
    public List<ReceiptAllocationEntity> getReceiptAllocations(UUID organisationId, UUID receiptId)
            throws ItemNotFoundException, AccessDeniedException {

        AccountEntity currentUser = getAuthenticatedAccount();
        OrganisationEntity organisation = getOrganisation(organisationId);
        OrganisationMember member = validateOrganisationMemberAccess(currentUser, organisation);

        permissionChecker.checkMemberPermission(member, "RECEIPTS", "viewReceipts");

        ReceiptEntity receipt = receiptRepo.findByReceiptIdAndOrganisation(receiptId, organisation)
                .orElseThrow(() -> new ItemNotFoundException("Receipt not found"));

        return receiptAllocationRepo.findByReceipt(receipt);
    }

    @Override
    public ReceiptAllocationEntity activateAllocation(UUID organisationId, UUID allocationId)
            throws ItemNotFoundException, AccessDeniedException {

        ReceiptAllocationEntity allocation = getAllocationById(organisationId, allocationId);
        return receiptAllocationRepo.save(allocation);
    }

    @Override
    public ReceiptAllocationEntity deactivateAllocation(UUID organisationId, UUID allocationId)
            throws ItemNotFoundException, AccessDeniedException {

        ReceiptAllocationEntity allocation = getAllocationById(organisationId, allocationId);
        return receiptAllocationRepo.save(allocation);
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

    private OrganisationEntity getOrganisation(UUID organisationId) throws ItemNotFoundException {
        return organisationRepo.findById(organisationId)
                .orElseThrow(() -> new ItemNotFoundException("Organisation not found"));
    }

    private OrganisationMember validateOrganisationMemberAccess(AccountEntity account, OrganisationEntity organisation)
            throws ItemNotFoundException {
        OrganisationMember member = organisationMemberRepo.findByAccountAndOrganisation(account, organisation)
                .orElseThrow(() -> new ItemNotFoundException("Member not found in organisation"));

        if (member.getStatus() != MemberStatus.ACTIVE) {
            throw new ItemNotFoundException("Member is not active");
        }

        return member;
    }
}