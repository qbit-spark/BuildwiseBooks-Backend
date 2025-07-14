package com.qbitspark.buildwisebackend.accounting_service.receipt_mng.service.impl;

import com.qbitspark.buildwisebackend.accounting_service.budget_mng.org_budget.entity.OrgBudgetDetailAllocationEntity;
import com.qbitspark.buildwisebackend.accounting_service.budget_mng.org_budget.repo.OrgBudgetDetailAllocationRepo;
import com.qbitspark.buildwisebackend.accounting_service.receipt_mng.entity.ReceiptAllocationDetailEntity;
import com.qbitspark.buildwisebackend.accounting_service.receipt_mng.entity.ReceiptAllocationEntity;
import com.qbitspark.buildwisebackend.accounting_service.receipt_mng.entity.ReceiptEntity;
import com.qbitspark.buildwisebackend.accounting_service.receipt_mng.payload.CreateAllocationRequest;
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
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class ReceiptAllocationServiceImpl implements ReceiptAllocationService {

    private final ReceiptAllocationRepo allocationRepo;
    private final ReceiptAllocationDetailRepo detailRepo;
    private final ReceiptRepo receiptRepo;
    private final OrgBudgetDetailAllocationRepo budgetDetailRepo;
    private final OrganisationRepo organisationRepo;
    private final OrganisationMemberRepo organisationMemberRepo;
    private final AccountRepo accountRepo;
    private final PermissionCheckerService permissionChecker;

    @Override
    public ReceiptAllocationEntity createAllocation(UUID organisationId, CreateAllocationRequest request)
            throws ItemNotFoundException, AccessDeniedException {

        AccountEntity currentUser = getAuthenticatedAccount();
        OrganisationEntity organisation = getOrganisation(organisationId);
        OrganisationMember member = validateMemberAccess(currentUser, organisation);

        permissionChecker.checkMemberPermission(member, "RECEIPTS", "createReceipt");

        ReceiptEntity receipt = receiptRepo.findByReceiptIdAndOrganisation(request.getReceiptId(), organisation)
                .orElseThrow(() -> new ItemNotFoundException("Receipt not found"));

        BigDecimal totalAmount = request.getDetails().stream()
                .map(CreateAllocationRequest.AllocationDetail::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (!receipt.canAllocate(totalAmount)) {
            throw new ItemNotFoundException("Cannot allocate more than receipt amount");
        }

        ReceiptAllocationEntity allocation = new ReceiptAllocationEntity();
        allocation.setReceipt(receipt);
        allocation.setNotes(request.getNotes());

        ReceiptAllocationEntity savedAllocation = allocationRepo.save(allocation);

        List<ReceiptAllocationDetailEntity> details = new ArrayList<>();
        for (CreateAllocationRequest.AllocationDetail detailRequest : request.getDetails()) {

            OrgBudgetDetailAllocationEntity budgetDetail = budgetDetailRepo.findById(detailRequest.getBudgetDetailAllocationId())
                    .orElseThrow(() -> new ItemNotFoundException("Budget detail allocation not found"));

            validateBudgetDetail(budgetDetail, organisation);

            ReceiptAllocationDetailEntity detail = new ReceiptAllocationDetailEntity();
            detail.setAllocation(savedAllocation);
            detail.setBudgetDetailAllocation(budgetDetail);
            detail.setAmount(detailRequest.getAmount());

            details.add(detailRepo.save(detail));
        }

        savedAllocation.setDetails(details);
        return savedAllocation;
    }

    @Override
    public ReceiptAllocationEntity updateAllocation(UUID organisationId, UUID allocationId, CreateAllocationRequest request)
            throws ItemNotFoundException, AccessDeniedException {

        AccountEntity currentUser = getAuthenticatedAccount();
        OrganisationEntity organisation = getOrganisation(organisationId);
        OrganisationMember member = validateMemberAccess(currentUser, organisation);

        permissionChecker.checkMemberPermission(member, "RECEIPTS", "updateReceipt");

        ReceiptAllocationEntity allocation = allocationRepo.findById(allocationId)
                .orElseThrow(() -> new ItemNotFoundException("Allocation not found"));

        validateAllocationAccess(allocation, organisation);

        if (!allocation.canEdit()) {
            throw new ItemNotFoundException("Cannot edit allocation in status: " + allocation.getStatus());
        }

        detailRepo.deleteByAllocation(allocation);

        allocation.setNotes(request.getNotes());

        List<ReceiptAllocationDetailEntity> details = new ArrayList<>();
        for (CreateAllocationRequest.AllocationDetail detailRequest : request.getDetails()) {

            OrgBudgetDetailAllocationEntity budgetDetail = budgetDetailRepo.findById(detailRequest.getBudgetDetailAllocationId())
                    .orElseThrow(() -> new ItemNotFoundException("Budget detail allocation not found"));

            ReceiptAllocationDetailEntity detail = new ReceiptAllocationDetailEntity();
            detail.setAllocation(allocation);
            detail.setBudgetDetailAllocation(budgetDetail);
            detail.setAmount(detailRequest.getAmount());

            details.add(detailRepo.save(detail));
        }

        allocation.setDetails(details);
        return allocationRepo.save(allocation);
    }

    @Override
    public List<ReceiptAllocationEntity> getReceiptAllocations(UUID organisationId, UUID receiptId)
            throws ItemNotFoundException, AccessDeniedException {

        AccountEntity currentUser = getAuthenticatedAccount();
        OrganisationEntity organisation = getOrganisation(organisationId);
        OrganisationMember member = validateMemberAccess(currentUser, organisation);

        permissionChecker.checkMemberPermission(member, "RECEIPTS", "viewReceipts");

        ReceiptEntity receipt = receiptRepo.findByReceiptIdAndOrganisation(receiptId, organisation)
                .orElseThrow(() -> new ItemNotFoundException("Receipt not found"));

        return allocationRepo.findByReceipt(receipt);
    }

    @Override
    public ReceiptAllocationEntity getAllocationById(UUID organisationId, UUID allocationId)
            throws ItemNotFoundException, AccessDeniedException {

        AccountEntity currentUser = getAuthenticatedAccount();
        OrganisationEntity organisation = getOrganisation(organisationId);
        OrganisationMember member = validateMemberAccess(currentUser, organisation);

        permissionChecker.checkMemberPermission(member, "RECEIPTS", "viewReceipts");

        ReceiptAllocationEntity allocation = allocationRepo.findById(allocationId)
                .orElseThrow(() -> new ItemNotFoundException("Allocation not found"));

        validateAllocationAccess(allocation, organisation);
        return allocation;
    }


    @Override
    public void cancelAllocation(UUID organisationId, UUID allocationId)
            throws ItemNotFoundException, AccessDeniedException {

        AccountEntity currentUser = getAuthenticatedAccount();
        OrganisationEntity organisation = getOrganisation(organisationId);
        OrganisationMember member = validateMemberAccess(currentUser, organisation);

        permissionChecker.checkMemberPermission(member, "RECEIPTS", "updateReceipt");

        ReceiptAllocationEntity allocation = allocationRepo.findById(allocationId)
                .orElseThrow(() -> new ItemNotFoundException("Allocation not found"));

        validateAllocationAccess(allocation, organisation);

        allocation.cancel();
        allocationRepo.save(allocation);
    }

    // Helper methods
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

    private OrganisationMember validateMemberAccess(AccountEntity account, OrganisationEntity organisation)
            throws ItemNotFoundException {
        OrganisationMember member = organisationMemberRepo.findByAccountAndOrganisation(account, organisation)
                .orElseThrow(() -> new ItemNotFoundException("Member not found in organisation"));

        if (member.getStatus() != MemberStatus.ACTIVE) {
            throw new ItemNotFoundException("Member is not active");
        }
        return member;
    }

    private void validateAllocationAccess(ReceiptAllocationEntity allocation, OrganisationEntity organisation)
            throws ItemNotFoundException {
        if (!allocation.getReceipt().getOrganisation().getOrganisationId().equals(organisation.getOrganisationId())) {
            throw new ItemNotFoundException("Allocation not found in this organisation");
        }
    }

    private void validateBudgetDetail(OrgBudgetDetailAllocationEntity budgetDetail, OrganisationEntity organisation)
            throws ItemNotFoundException {
        if (!budgetDetail.getHeaderLineItem().getOrgBudget().getOrganisation()
                .getOrganisationId().equals(organisation.getOrganisationId())) {
            throw new ItemNotFoundException("Budget detail allocation does not belong to this organisation");
        }
    }
}