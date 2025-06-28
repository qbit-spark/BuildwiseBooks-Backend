package com.qbitspark.buildwisebackend.accounting_service.documentflow.voucher.service.impl;

import com.qbitspark.buildwisebackend.accounting_service.budget_mng.project_budget.entity.ProjectBudgetLineItemEntity;
import com.qbitspark.buildwisebackend.accounting_service.budget_mng.project_budget.enums.ProjectBudgetStatus;
import com.qbitspark.buildwisebackend.accounting_service.budget_mng.project_budget.repo.ProjectBudgetLineItemRepo;
import com.qbitspark.buildwisebackend.accounting_service.budget_mng.project_budget.repo.ProjectBudgetRepo;
import com.qbitspark.buildwisebackend.accounting_service.coa.entity.ChartOfAccounts;
import com.qbitspark.buildwisebackend.accounting_service.coa.entity.JournalEntry;
import com.qbitspark.buildwisebackend.accounting_service.documentflow.voucher.entity.VoucherBeneficiaryEntity;
import com.qbitspark.buildwisebackend.accounting_service.documentflow.voucher.entity.VoucherDeductionEntity;
import com.qbitspark.buildwisebackend.accounting_service.documentflow.voucher.entity.VoucherEntity;
import com.qbitspark.buildwisebackend.accounting_service.documentflow.voucher.enums.PaymentStatus;
import com.qbitspark.buildwisebackend.accounting_service.documentflow.voucher.enums.VoucherStatus;
import com.qbitspark.buildwisebackend.accounting_service.documentflow.voucher.paylaod.*;
import com.qbitspark.buildwisebackend.accounting_service.documentflow.voucher.repo.VoucherRepo;
import com.qbitspark.buildwisebackend.accounting_service.documentflow.voucher.service.VoucherNumberService;
import com.qbitspark.buildwisebackend.accounting_service.documentflow.voucher.service.VoucherService;
import com.qbitspark.buildwisebackend.authentication_service.Repository.AccountRepo;
import com.qbitspark.buildwisebackend.authentication_service.entity.AccountEntity;
import com.qbitspark.buildwisebackend.globeadvice.exceptions.AccessDeniedException;
import com.qbitspark.buildwisebackend.globeadvice.exceptions.ItemNotFoundException;
import com.qbitspark.buildwisebackend.organisation_service.organisation_mng.entity.OrganisationEntity;
import com.qbitspark.buildwisebackend.organisation_service.organisation_mng.repo.OrganisationRepo;
import com.qbitspark.buildwisebackend.organisation_service.orgnisation_members_mng.entities.OrganisationMember;
import com.qbitspark.buildwisebackend.organisation_service.orgnisation_members_mng.enums.MemberRole;
import com.qbitspark.buildwisebackend.organisation_service.orgnisation_members_mng.enums.MemberStatus;
import com.qbitspark.buildwisebackend.organisation_service.orgnisation_members_mng.repo.OrganisationMemberRepo;
import com.qbitspark.buildwisebackend.projectmng_service.entity.ProjectEntity;
import com.qbitspark.buildwisebackend.projectmng_service.entity.ProjectTeamMemberEntity;
import com.qbitspark.buildwisebackend.projectmng_service.enums.TeamMemberRole;
import com.qbitspark.buildwisebackend.projectmng_service.repo.ProjectRepo;
import com.qbitspark.buildwisebackend.projectmng_service.repo.ProjectTeamMemberRepo;
import com.qbitspark.buildwisebackend.vendormng_service.entity.VendorEntity;
import com.qbitspark.buildwisebackend.vendormng_service.repo.VendorsRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class VoucherServiceImpl implements VoucherService {

    private final VoucherRepo voucherRepo;
    private final OrganisationRepo organisationRepo;
    private final ProjectRepo projectRepo;
    private final VendorsRepo vendorsRepo;
    private final AccountRepo accountRepo;
    private final OrganisationMemberRepo organisationMemberRepo;
    private final VoucherNumberService voucherNumberService;
    private final ProjectTeamMemberRepo projectTeamMemberRepo;
    private final ProjectBudgetRepo projectBudgetRepo;
    private final ProjectBudgetLineItemRepo projectBudgetLineItemRepo;

    @Override
    public VoucherEntity createVoucher(UUID organisationId, CreateVoucherRequest request)
            throws ItemNotFoundException, AccessDeniedException {

        AccountEntity currentUser = getAuthenticatedAccount();

        OrganisationEntity organisation = organisationRepo.findById(organisationId)
                .orElseThrow(() -> new ItemNotFoundException("Organisation not found"));

        OrganisationMember organisationMember = validateOrganisationAccess(currentUser, organisation,
                List.of(MemberRole.OWNER, MemberRole.ADMIN, MemberRole.MEMBER));

        ProjectEntity project = projectRepo.findById(request.getProjectId())
                .orElseThrow(() -> new ItemNotFoundException("Project not found"));

        validateProject(project, organisation);

        validateProjectMemberPermissions(currentUser, project,
                List.of(TeamMemberRole.ACCOUNTANT, TeamMemberRole.OWNER, TeamMemberRole.PROJECT_MANAGER));


        ProjectBudgetLineItemEntity budgetLineItem = validateAndGetBudgetLineItem(
                request.getProjectBudgetAccountId(), project);

        String voucherNumber = voucherNumberService.generateVoucherNumber(project, organisation);

        // Create beneficiaries and calculate totals
        List<VoucherBeneficiaryEntity> beneficiaryEntities = new ArrayList<>();
        BigDecimal totalAmount = BigDecimal.ZERO;
        BigDecimal totalDeductions = BigDecimal.ZERO;

        for (VoucherBeneficiaryRequest beneficiaryRequest : request.getBeneficiaries()) {
            // Validate vendor
            VendorEntity vendor = vendorsRepo.findById(beneficiaryRequest.getVendorId())
                    .orElseThrow(() -> new ItemNotFoundException("Vendor(s) not found!"));

            if (!vendor.getOrganisation().getOrganisationId().equals(organisationId)) {
                throw new ItemNotFoundException("Vendor does not belong to this organisation");
            }

            // Create beneficiary
            VoucherBeneficiaryEntity beneficiaryEntity = new VoucherBeneficiaryEntity();
            beneficiaryEntity.setVendor(vendor);
            beneficiaryEntity.setDescription(beneficiaryRequest.getDescription());
            beneficiaryEntity.setAmount(beneficiaryRequest.getAmount());

            // Create deductions
            List<VoucherDeductionEntity> deductionEntities = new ArrayList<>();
            BigDecimal beneficiaryDeductionTotal = BigDecimal.ZERO;

            for (VoucherDeductionRequest deductionRequest : beneficiaryRequest.getDeductions()) {
                VoucherDeductionEntity deductionEntity = new VoucherDeductionEntity();
                deductionEntity.setBeneficiary(beneficiaryEntity);
                deductionEntity.setDeductionType(deductionRequest.getDeductionType());
                deductionEntity.setPercentage(deductionRequest.getPercentage());

                // Calculate deduction amount
                BigDecimal deductionAmount = beneficiaryRequest.getAmount()
                        .multiply(deductionRequest.getPercentage())
                        .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

                deductionEntity.setDeductionAmount(deductionAmount);
                deductionEntities.add(deductionEntity);

                beneficiaryDeductionTotal = beneficiaryDeductionTotal.add(deductionAmount);
            }

            beneficiaryEntity.setDeductions(deductionEntities);
            beneficiaryEntities.add(beneficiaryEntity);

            totalAmount = totalAmount.add(beneficiaryRequest.getAmount());
            totalDeductions = totalDeductions.add(beneficiaryDeductionTotal);
        }


        validateBudgetAvailability(budgetLineItem, totalAmount);

        // Create a voucher with budget line item association
        VoucherEntity voucher = new VoucherEntity();
        voucher.setVoucherNumber(voucherNumber);
        voucher.setVoucherDate(LocalDateTime.now());
        voucher.setOverallDescription(request.getGeneralDescription());
        voucher.setCreatedBy(organisationMember);
        voucher.setOrganisation(organisation);
        voucher.setProject(project);
        voucher.setProjectBudgetLineItem(budgetLineItem); // NEW: Set budget line item
        voucher.setStatus(VoucherStatus.DRAFT);
        voucher.setCurrency("TSh");

        // Set voucher reference for beneficiaries
        beneficiaryEntities.forEach(beneficiary -> beneficiary.setVoucher(voucher));
        voucher.setBeneficiaries(beneficiaryEntities);
        voucher.setTotalAmount(totalAmount);

        return voucherRepo.save(voucher);
    }


    @Override
    public Page<VoucherEntity> getProjectVouchers(UUID organisationId, UUID projectId, Pageable pageable)
            throws ItemNotFoundException, AccessDeniedException {

        AccountEntity currentUser = getAuthenticatedAccount();

        OrganisationEntity organisation = organisationRepo.findById(organisationId)
                .orElseThrow(() -> new ItemNotFoundException("Organisation not found"));

        validateOrganisationAccess(currentUser, organisation,
                List.of(MemberRole.OWNER, MemberRole.ADMIN, MemberRole.MEMBER));

        ProjectEntity project = projectRepo.findById(projectId)
                .orElseThrow(() -> new ItemNotFoundException("Project not found"));

        validateProject(project, organisation);

        validateProjectMemberPermissions(currentUser, project,
                List.of(TeamMemberRole.ACCOUNTANT, TeamMemberRole.OWNER,
                        TeamMemberRole.PROJECT_MANAGER, TeamMemberRole.MEMBER));


        return voucherRepo.findAllByProject(project, pageable);
    }



    @Override
    public VoucherEntity updateVoucher(UUID organisationId, UUID voucherId, UpdateVoucherRequest request)
            throws ItemNotFoundException, AccessDeniedException {


        AccountEntity currentUser = getAuthenticatedAccount();
        OrganisationEntity organisation = validateOrganisationExists(organisationId);
        OrganisationMember organisationMember = validateOrganisationAccess(currentUser, organisation,
                List.of(MemberRole.OWNER, MemberRole.ADMIN, MemberRole.MEMBER));

        // Get existing voucher
        VoucherEntity existingVoucher = validateVoucherExists(voucherId, organisation);

        // Validate voucher can be updated
        validateVoucherCanBeUpdated(existingVoucher);

        // Validate project permissions
        validateProjectMemberPermissions(currentUser, existingVoucher.getProject(),
                List.of(TeamMemberRole.ACCOUNTANT, TeamMemberRole.OWNER, TeamMemberRole.PROJECT_MANAGER));

        // Update voucher fields
        updateVoucherBasicFields(existingVoucher, request);

        // Update budget line item if provided
        if (request.getProjectBudgetLineItemId() != null) {
            updateVoucherBudgetLineItem(existingVoucher, request.getProjectBudgetLineItemId());
        }

        // Update beneficiaries if provided
        if (request.getBeneficiaries() != null && !request.getBeneficiaries().isEmpty()) {
            updateVoucherBeneficiaries(existingVoucher, request.getBeneficiaries(), organisationId);
        }

        // Update attachments if provided
        if (request.getAttachmentIds() != null) {
            existingVoucher.setAttachments(request.getAttachmentIds());
        }

        // Save and return
        VoucherEntity updatedVoucher = voucherRepo.save(existingVoucher);

        log.info("Voucher {} updated by user {}",
                updatedVoucher.getVoucherNumber(), organisationMember.getAccount().getUserName());

        return updatedVoucher;
    }



    // ====================================================================
    // HELPER METHODS
    // ====================================================================


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


    private OrganisationMember validateOrganisationAccess(AccountEntity account, OrganisationEntity organisation,
                                                          List<MemberRole> allowedRoles) throws ItemNotFoundException, AccessDeniedException {

        OrganisationMember member = organisationMemberRepo.findByAccountAndOrganisation(account, organisation)
                .orElseThrow(() -> new ItemNotFoundException("User is not a member of this organisation"));

        if (member.getStatus() != MemberStatus.ACTIVE) {
            throw new AccessDeniedException("User membership is not active");
        }

        if (!allowedRoles.contains(member.getRole())) {
            throw new AccessDeniedException("User does not have sufficient permissions");
        }

        return member; // RETURN THE MEMBER
    }

    private void validateProject(ProjectEntity project, OrganisationEntity organisation) throws ItemNotFoundException {
        if (!project.getOrganisation().getOrganisationId().equals(organisation.getOrganisationId())) {
            throw new ItemNotFoundException("Project does not belong to this organisation");
        }
    }

    private ProjectTeamMemberEntity validateProjectMemberPermissions(AccountEntity account, ProjectEntity project, List<TeamMemberRole> allowedRoles) throws ItemNotFoundException, AccessDeniedException {

        // Step 1: Find the organisation member
        OrganisationMember organisationMember = organisationMemberRepo.findByAccountAndOrganisation(account, project.getOrganisation())
                .orElseThrow(() -> new ItemNotFoundException("Member is not found in organisation"));

        // Step 2: Check if an organisation member is active
        if (organisationMember.getStatus() != MemberStatus.ACTIVE) {
            throw new ItemNotFoundException("Member is not active");
        }


        // Step 3: Get the project team member details
        ProjectTeamMemberEntity projectTeamMember = projectTeamMemberRepo.findByOrganisationMemberAndProject(organisationMember, project)
                .orElseThrow(() -> new ItemNotFoundException("Project team member not found"));

        // Step 5: Check if the member has one of the allowed roles
        if (!allowedRoles.contains(projectTeamMember.getRole())) {
            throw new AccessDeniedException("Member has insufficient permissions for this operation");
        }

        return projectTeamMember;
    }


    private ProjectBudgetLineItemEntity validateAndGetBudgetLineItem(UUID budgetLineItemId, ProjectEntity project)
            throws ItemNotFoundException {

        ProjectBudgetLineItemEntity budgetLineItem = projectBudgetLineItemRepo.findById(budgetLineItemId)
                .orElseThrow(() -> new ItemNotFoundException("Project Budget Line Item not found"));

        // Validate budget line item belonging to this project
        if (!budgetLineItem.getProjectBudget().getProject().getProjectId().equals(project.getProjectId())) {
            throw new ItemNotFoundException("Budget Line Item does not belong to this project");
        }

        // Validate project budget is active
        if (budgetLineItem.getProjectBudget().getStatus() != ProjectBudgetStatus.ACTIVE &&
                budgetLineItem.getProjectBudget().getStatus() != ProjectBudgetStatus.APPROVED) {
            throw new ItemNotFoundException("Project budget is not active. Cannot create vouchers.");
        }

        return budgetLineItem;
    }

    private void validateBudgetAvailability(ProjectBudgetLineItemEntity budgetLineItem, BigDecimal requestedAmount)
            throws ItemNotFoundException {

        BigDecimal availableBudget = budgetLineItem.getRemainingAmount();

        if (availableBudget.compareTo(requestedAmount) < 0) {
            ChartOfAccounts account = budgetLineItem.getChartOfAccount();
            throw new ItemNotFoundException(String.format(
                    "Insufficient budget for account %s - %s. Available: TSh %s, Requested: TSh %s",
                    account.getAccountCode(),
                    account.getName(),
                    availableBudget,
                    requestedAmount
            ));
        }
    }

    private OrganisationEntity validateOrganisationExists(UUID organisationId)
            throws ItemNotFoundException {
        return organisationRepo.findById(organisationId)
                .orElseThrow(() -> new ItemNotFoundException("Organisation not found"));
    }

    private VoucherEntity validateVoucherExists(UUID voucherId, OrganisationEntity organisation)
            throws ItemNotFoundException {
        return voucherRepo.findByIdAndOrganisation(voucherId, organisation)
                .orElseThrow(() -> new ItemNotFoundException("Voucher not found"));
    }

    private void validateVoucherCanBeUpdated(VoucherEntity voucher)
            throws ItemNotFoundException {
        if (voucher.getStatus() != VoucherStatus.DRAFT) {
            throw new ItemNotFoundException("Only vouchers in DRAFT status can be updated. Current status: " + voucher.getStatus());
        }
    }

    private void updateVoucherBasicFields(VoucherEntity voucher, UpdateVoucherRequest request) {
        if (request.getGeneralDescription() != null) {
            voucher.setOverallDescription(request.getGeneralDescription());
        }
        voucher.setUpdatedAt(LocalDateTime.now());
    }

    private void updateVoucherBudgetLineItem(VoucherEntity voucher, UUID newBudgetLineItemId)
            throws ItemNotFoundException {

        ProjectBudgetLineItemEntity newBudgetLineItem = validateAndGetBudgetLineItem(
                newBudgetLineItemId, voucher.getProject());

        voucher.setProjectBudgetLineItem(newBudgetLineItem);
    }

    private void updateVoucherBeneficiaries(VoucherEntity voucher, List<VoucherBeneficiaryRequest> newBeneficiaries,
                                            UUID organisationId) throws ItemNotFoundException {

        // Calculate new total amount
        BigDecimal newTotalAmount = newBeneficiaries.stream()
                .map(VoucherBeneficiaryRequest::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Validate budget availability with new amount
        validateBudgetAvailability(voucher.getProjectBudgetLineItem(), newTotalAmount);

        // Clear existing beneficiaries
        voucher.getBeneficiaries().clear();

        // Create new beneficiaries
        List<VoucherBeneficiaryEntity> beneficiaryEntities = new ArrayList<>();
        BigDecimal totalDeductions = BigDecimal.ZERO;

        for (VoucherBeneficiaryRequest beneficiaryRequest : newBeneficiaries) {
            // Validate vendor
            VendorEntity vendor = vendorsRepo.findById(beneficiaryRequest.getVendorId())
                    .orElseThrow(() -> new ItemNotFoundException("Vendor not found: " + beneficiaryRequest.getVendorId()));

            if (!vendor.getOrganisation().getOrganisationId().equals(organisationId)) {
                throw new ItemNotFoundException("Vendor does not belong to this organisation");
            }

            // Create beneficiary
            VoucherBeneficiaryEntity beneficiaryEntity = new VoucherBeneficiaryEntity();
            beneficiaryEntity.setVoucher(voucher);
            beneficiaryEntity.setVendor(vendor);
            beneficiaryEntity.setDescription(beneficiaryRequest.getDescription());
            beneficiaryEntity.setAmount(beneficiaryRequest.getAmount());

            // Create deductions
            List<VoucherDeductionEntity> deductionEntities = new ArrayList<>();
            BigDecimal beneficiaryDeductionTotal = BigDecimal.ZERO;

            for (VoucherDeductionRequest deductionRequest : beneficiaryRequest.getDeductions()) {
                VoucherDeductionEntity deductionEntity = new VoucherDeductionEntity();
                deductionEntity.setBeneficiary(beneficiaryEntity);
                deductionEntity.setDeductionType(deductionRequest.getDeductionType());
                deductionEntity.setPercentage(deductionRequest.getPercentage());

                // Calculate deduction amount
                BigDecimal deductionAmount = beneficiaryRequest.getAmount()
                        .multiply(deductionRequest.getPercentage())
                        .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

                deductionEntity.setDeductionAmount(deductionAmount);
                deductionEntities.add(deductionEntity);

                beneficiaryDeductionTotal = beneficiaryDeductionTotal.add(deductionAmount);
            }

            beneficiaryEntity.setDeductions(deductionEntities);
            beneficiaryEntities.add(beneficiaryEntity);
            totalDeductions = totalDeductions.add(beneficiaryDeductionTotal);
        }

        voucher.setBeneficiaries(beneficiaryEntities);
        voucher.setTotalAmount(newTotalAmount);
    }

}