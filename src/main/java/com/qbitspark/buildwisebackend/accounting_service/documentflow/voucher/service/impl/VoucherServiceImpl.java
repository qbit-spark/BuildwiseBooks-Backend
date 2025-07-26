package com.qbitspark.buildwisebackend.accounting_service.documentflow.voucher.service.impl;

import com.qbitspark.buildwisebackend.accounting_service.budget_mng.org_budget.service.BudgetSpendingService;
import com.qbitspark.buildwisebackend.accounting_service.coa.entity.ChartOfAccounts;
import com.qbitspark.buildwisebackend.accounting_service.coa.repo.ChartOfAccountsRepo;
import com.qbitspark.buildwisebackend.accounting_service.deducts_mng.entity.DeductsEntity;
import com.qbitspark.buildwisebackend.accounting_service.deducts_mng.repo.DeductRepo;
import com.qbitspark.buildwisebackend.accounting_service.documentflow.invoice.enums.ActionType;
import com.qbitspark.buildwisebackend.accounting_service.documentflow.voucher.entity.VoucherBeneficiaryEntity;
import com.qbitspark.buildwisebackend.accounting_service.documentflow.voucher.entity.VoucherDeductionEntity;
import com.qbitspark.buildwisebackend.accounting_service.documentflow.voucher.entity.VoucherEntity;
import com.qbitspark.buildwisebackend.accounting_service.documentflow.voucher.enums.VoucherStatus;
import com.qbitspark.buildwisebackend.accounting_service.documentflow.voucher.paylaod.*;
import com.qbitspark.buildwisebackend.accounting_service.documentflow.voucher.repo.VoucherBeneficiaryRepo;
import com.qbitspark.buildwisebackend.accounting_service.documentflow.voucher.repo.VoucherRepo;
import com.qbitspark.buildwisebackend.accounting_service.documentflow.voucher.service.VoucherNumberService;
import com.qbitspark.buildwisebackend.accounting_service.documentflow.voucher.service.VoucherService;
import com.qbitspark.buildwisebackend.approval_service.enums.ServiceType;
import com.qbitspark.buildwisebackend.approval_service.service.ApprovalIntegrationService;
import com.qbitspark.buildwisebackend.approval_service.service.ApprovalWorkflowService;
import com.qbitspark.buildwisebackend.authentication_service.repo.AccountRepo;
import com.qbitspark.buildwisebackend.authentication_service.entity.AccountEntity;
import com.qbitspark.buildwisebackend.drive_mng.entity.OrgFileEntity;
import com.qbitspark.buildwisebackend.drive_mng.repo.OrgFileRepo;
import com.qbitspark.buildwisebackend.globeadvice.exceptions.AccessDeniedException;
import com.qbitspark.buildwisebackend.globeadvice.exceptions.ItemNotFoundException;
import com.qbitspark.buildwisebackend.organisation_service.organisation_mng.entity.OrganisationEntity;
import com.qbitspark.buildwisebackend.organisation_service.organisation_mng.repo.OrganisationRepo;
import com.qbitspark.buildwisebackend.organisation_service.orgnisation_members_mng.entities.OrganisationMember;
import com.qbitspark.buildwisebackend.organisation_service.orgnisation_members_mng.enums.MemberStatus;
import com.qbitspark.buildwisebackend.organisation_service.orgnisation_members_mng.repo.OrganisationMemberRepo;
import com.qbitspark.buildwisebackend.organisation_service.roles_mng.service.PermissionCheckerService;
import com.qbitspark.buildwisebackend.projectmng_service.entity.ProjectEntity;
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
import java.util.*;
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
    private final DeductRepo deductRepo;
    private final VoucherBeneficiaryRepo voucherBeneficiaryRepo;
    private final OrgFileRepo orgFileRepo;
    private final PermissionCheckerService permissionChecker;
    private final BudgetSpendingService budgetSpendingService;
    private final ChartOfAccountsRepo chartOfAccountsRepo;
    private final ApprovalIntegrationService approvalIntegrationService;
    private final ApprovalWorkflowService approvalWorkflowService;

    @Override
    public VoucherEntity createVoucher(UUID organisationId, CreateVoucherRequest request, ActionType action)
            throws ItemNotFoundException, AccessDeniedException {

        AccountEntity currentUser = getAuthenticatedAccount();
        OrganisationEntity organisation = organisationRepo.findById(organisationId)
                .orElseThrow(() -> new ItemNotFoundException("Organisation not found"));

        ProjectEntity project = projectRepo.findById(request.getProjectId())
                .orElseThrow(() -> new ItemNotFoundException("Project not found"));

        OrganisationMember member = validateProjectAndOrganisationAccess(currentUser, project, organisation);

        permissionChecker.checkMemberPermission(member, "VOUCHERS","createVoucher");

        // Validate and get an account
        ChartOfAccounts account = validateVoucherAccount(request.getAccountId(), organisationId);

        String voucherNumber = voucherNumberService.generateVoucherNumber(project, organisation);

        List<VoucherBeneficiaryEntity> beneficiaryEntities = new ArrayList<>();
        BigDecimal totalAmount = BigDecimal.ZERO;

        for (VoucherBeneficiaryRequest beneficiaryRequest : request.getBeneficiaries()) {
            VendorEntity vendor = vendorsRepo.findById(beneficiaryRequest.getVendorId())
                    .orElseThrow(() -> new ItemNotFoundException("Vendor(s) not found!"));

            if (!vendor.getOrganisation().getOrganisationId().equals(organisationId)) {
                throw new ItemNotFoundException("Vendor does not belong to this organisation");
            }

            List<DeductsEntity> validDeducts = validateAndFetchDeducts(
                    beneficiaryRequest.getDeductions(), organisationId);

            VoucherBeneficiaryEntity beneficiaryEntity = new VoucherBeneficiaryEntity();
            beneficiaryEntity.setVendor(vendor);
            beneficiaryEntity.setDescription(beneficiaryRequest.getDescription());
            beneficiaryEntity.setAmount(beneficiaryRequest.getAmount());

            List<VoucherDeductionEntity> deductionEntities = new ArrayList<>();
            for (DeductsEntity deductEntity : validDeducts) {
                VoucherDeductionEntity deductionEntity = new VoucherDeductionEntity();
                deductionEntity.setBeneficiary(beneficiaryEntity);
                deductionEntity.setPercentage(deductEntity.getDeductPercent());
                deductionEntity.setDeductId(deductEntity.getDeductId());
                deductionEntity.setDeductName(deductEntity.getDeductName());

                BigDecimal deductionAmount = beneficiaryRequest.getAmount()
                        .multiply(deductEntity.getDeductPercent())
                        .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

                deductionEntity.setDeductionAmount(deductionAmount);
                deductionEntities.add(deductionEntity);
            }

            beneficiaryEntity.setDeductions(deductionEntities);
            beneficiaryEntities.add(beneficiaryEntity);
            totalAmount = totalAmount.add(beneficiaryRequest.getAmount());
        }

        // SOFT BALANCE CHECK - warn but allow creation
        performSoftBalanceCheck(account.getId(), totalAmount);

        VoucherEntity voucher = new VoucherEntity();
        voucher.setVoucherNumber(voucherNumber);
        voucher.setVoucherDate(LocalDateTime.now());
        voucher.setAccount(account);
        voucher.setOverallDescription(request.getGeneralDescription());
        voucher.setCreatedBy(member);
        voucher.setOrganisation(organisation);
        voucher.setProject(project);
        voucher.setStatus(VoucherStatus.DRAFT);
        voucher.setCurrency("TSh");
        voucher.setAttachments(request.getAttachments());

        beneficiaryEntities.forEach(beneficiary -> beneficiary.setVoucher(voucher));
        voucher.setBeneficiaries(beneficiaryEntities);
        voucher.setTotalAmount(totalAmount);

        VoucherEntity savedVoucher =  voucherRepo.save(voucher);

        // Handle approval workflow
        if (action == ActionType.SAVE_AND_APPROVAL) {
            // 1. Update document status via integration service
            approvalIntegrationService.submitForApproval(
                    ServiceType.VOUCHER,
                    savedVoucher.getId(),
                    organisationId,
                    project.getProjectId()
            );

            // 2. Start the workflow directly
            approvalWorkflowService.startApprovalWorkflow(
                    ServiceType.VOUCHER,
                    savedVoucher.getId(),
                    organisationId,
                    project.getProjectId()
            );
        }

        return savedVoucher;
    }


    @Override
    public Page<VoucherEntity> getProjectVouchers(UUID organisationId, UUID projectId, Pageable pageable)
            throws ItemNotFoundException, AccessDeniedException {

        AccountEntity currentUser = getAuthenticatedAccount();

        OrganisationEntity organisation = organisationRepo.findById(organisationId).orElseThrow(() -> new ItemNotFoundException("Organisation not found"));

        ProjectEntity project = projectRepo.findById(projectId)
                .orElseThrow(() -> new ItemNotFoundException("Project not found"));

        OrganisationMember member = validateProjectAndOrganisationAccess(currentUser, project, organisation);

        permissionChecker.checkMemberPermission(member, "VOUCHERS","viewVouchers");

        return voucherRepo.findAllByProject(project, pageable);
    }

    @Override
    public VoucherEntity updateVoucher(UUID organisationId, UUID voucherId, UpdateVoucherRequest request, ActionType action)
            throws ItemNotFoundException, AccessDeniedException {

        AccountEntity currentUser = getAuthenticatedAccount();
        OrganisationEntity organisation = organisationRepo.findById(organisationId)
                .orElseThrow(() -> new ItemNotFoundException("Organisation not found"));

        VoucherEntity existingVoucher = validateVoucherExists(voucherId, organisation);
        OrganisationMember member = validateProjectAndOrganisationAccess(currentUser, existingVoucher.getProject(), organisation);
        permissionChecker.checkMemberPermission(member, "VOUCHERS","updateVoucher");

        validateVoucherCanBeUpdated(existingVoucher);
        updateVoucherBasicFields(existingVoucher, request);

        if (request.getAccountId() != null) {
            ChartOfAccounts newAccount = validateVoucherAccount(request.getAccountId(), organisationId);
            existingVoucher.setAccount(newAccount);
        }

        if (request.getBeneficiaries() != null && !request.getBeneficiaries().isEmpty()) {
            updateVoucherBeneficiaries(existingVoucher, request.getBeneficiaries(), organisationId);

            // SOFT BALANCE CHECK after updating beneficiaries
            if (existingVoucher.getAccount() != null) {
                performSoftBalanceCheck(existingVoucher.getAccount().getId(), existingVoucher.getTotalAmount());
            }
        }

        if (request.getAttachments() != null) {
            updateVoucherAttachments(existingVoucher, request.getAttachments(), organisation);
        }

        VoucherEntity savedVoucher =  voucherRepo.save(existingVoucher);

        // Handle approval workflow
        if (action == ActionType.SAVE_AND_APPROVAL) {
            // 1. Update document status via integration service
            approvalIntegrationService.submitForApproval(
                    ServiceType.VOUCHER,
                    savedVoucher.getId(),
                    organisationId,
                    savedVoucher.getProject().getProjectId()
            );

            // 2. Start the workflow directly
            approvalWorkflowService.startApprovalWorkflow(
                    ServiceType.VOUCHER,
                    savedVoucher.getId(),
                    organisationId,
                    savedVoucher.getProject().getProjectId()
            );
        }

        return savedVoucher;
    }


    @Override
    public VoucherEntity getVoucherById(UUID organisationId, UUID voucherId) throws ItemNotFoundException, AccessDeniedException {

        AccountEntity currentUser = getAuthenticatedAccount();

        OrganisationEntity organisation = organisationRepo.findById(organisationId).orElseThrow(() -> new ItemNotFoundException("Organisation not found"));

        VoucherEntity existingVoucher = validateVoucherExists(voucherId, organisation);

        OrganisationMember member = validateProjectAndOrganisationAccess(currentUser, existingVoucher.getProject(), organisation);

        permissionChecker.checkMemberPermission(member, "VOUCHERS","viewVouchers");

        return validateVoucherExists(voucherId, organisation);


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


    private void validateProject(ProjectEntity project, OrganisationEntity organisation) throws ItemNotFoundException {
        if (!project.getOrganisation().getOrganisationId().equals(organisation.getOrganisationId())) {
            throw new ItemNotFoundException("Project does not belong to this organisation");
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


    private void updateVoucherBeneficiaries(VoucherEntity voucher, List<VoucherBeneficiaryRequest> newBeneficiaries,
                                            UUID organisationId) throws ItemNotFoundException {

        BigDecimal newTotalAmount = newBeneficiaries.stream()
                .map(VoucherBeneficiaryRequest::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

     //   validateBudgetAvailability(voucher.getDetailAllocation(), newTotalAmount);

        // First, explicitly delete existing beneficiaries from database
        // This ensures cascading deletion of deductions as well
        if (!voucher.getBeneficiaries().isEmpty()) {
            List<VoucherBeneficiaryEntity> existingBeneficiaries = new ArrayList<>(voucher.getBeneficiaries());
            voucherBeneficiaryRepo.deleteAll(existingBeneficiaries);

            voucher.getBeneficiaries().clear();

            voucherBeneficiaryRepo.flush();
        }

        List<VoucherBeneficiaryEntity> beneficiaryEntities = new ArrayList<>();
        BigDecimal totalDeductions = BigDecimal.ZERO;

        for (VoucherBeneficiaryRequest beneficiaryRequest : newBeneficiaries) {
            VendorEntity vendor = vendorsRepo.findById(beneficiaryRequest.getVendorId())
                    .orElseThrow(() -> new ItemNotFoundException("Vendor not found: " + beneficiaryRequest.getVendorId()));

            if (!vendor.getOrganisation().getOrganisationId().equals(organisationId)) {
                throw new ItemNotFoundException("Vendor does not belong to this organisation");
            }

            List<DeductsEntity> validDeducts = validateAndFetchDeducts(
                    beneficiaryRequest.getDeductions(), organisationId);

            VoucherBeneficiaryEntity beneficiaryEntity = new VoucherBeneficiaryEntity();
            beneficiaryEntity.setVoucher(voucher);
            beneficiaryEntity.setVendor(vendor);
            beneficiaryEntity.setDescription(beneficiaryRequest.getDescription());
            beneficiaryEntity.setAmount(beneficiaryRequest.getAmount());

            // Create deductions based on saved deduct entities
            List<VoucherDeductionEntity> deductionEntities = new ArrayList<>();
            BigDecimal beneficiaryDeductionTotal = BigDecimal.ZERO;

            for (DeductsEntity deductEntity : validDeducts) {
                VoucherDeductionEntity deductionEntity = new VoucherDeductionEntity();
                deductionEntity.setBeneficiary(beneficiaryEntity);
                deductionEntity.setPercentage(deductEntity.getDeductPercent());
                deductionEntity.setDeductId(deductEntity.getDeductId());
                deductionEntity.setDeductName(deductEntity.getDeductName());

                // Calculate deduction amount
                BigDecimal deductionAmount = beneficiaryRequest.getAmount()
                        .multiply(deductEntity.getDeductPercent())
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

    private List<DeductsEntity> validateAndFetchDeducts(List<UUID> deductIds, UUID organisationId)
            throws ItemNotFoundException {

        if (deductIds == null || deductIds.isEmpty()) {
            return new ArrayList<>(); // No deductions applied
        }

        // Check for duplicate deduct IDs
        Set<UUID> uniqueDeductIds = new HashSet<>(deductIds);
        if (uniqueDeductIds.size() != deductIds.size()) {
            throw new ItemNotFoundException("Duplicate deduct IDs found in the request");
        }

        // Fetch deducts from a database
        List<DeductsEntity> deducts = deductRepo.findByDeductIdInAndOrganisation_OrganisationId(
                deductIds, organisationId);

        // Verify all requested deducting were found
        if (deducts.size() != deductIds.size()) {
            Set<UUID> foundDeductIds = deducts.stream()
                    .map(DeductsEntity::getDeductId)
                    .collect(Collectors.toSet());

            List<UUID> missingDeductIds = deductIds.stream()
                    .filter(id -> !foundDeductIds.contains(id))
                    .toList();

            throw new ItemNotFoundException("The following deduct IDs do not belong to this organisation or do not exist: "
                    + missingDeductIds);
        }

        // Check if all deducting are active
        List<DeductsEntity> inactiveDeducts = deducts.stream()
                .filter(deduct -> !deduct.getIsActive())
                .toList();

        if (!inactiveDeducts.isEmpty()) {
            List<String> inactiveDeductNames = inactiveDeducts.stream()
                    .map(DeductsEntity::getDeductName)
                    .toList();

            throw new ItemNotFoundException("The following deducts are inactive and cannot be used: "
                    + inactiveDeductNames);
        }

        return deducts;
    }

    private void updateVoucherAttachments(VoucherEntity voucher, List<UUID> newAttachmentIds, OrganisationEntity organisation)
            throws ItemNotFoundException, AccessDeniedException {

        voucher.getAttachments().clear();
        voucherRepo.save(voucher);
        voucherRepo.flush();
        log.info("Cleared attachments for voucher {}", voucher.getVoucherNumber());

        if (!newAttachmentIds.isEmpty()) {
            validateVoucherAttachments(newAttachmentIds, organisation);

            voucher.setAttachments(new ArrayList<>(newAttachmentIds));
            voucherRepo.save(voucher);

            log.info("Added {} new attachments to voucher {}",
                    newAttachmentIds.size(), voucher.getVoucherNumber());
        }
    }
    private void validateVoucherAttachments(List<UUID> attachmentIds, OrganisationEntity organisation)
            throws ItemNotFoundException, AccessDeniedException {

        for (UUID fileId : attachmentIds) {
            OrgFileEntity file = orgFileRepo.findById(fileId)
                    .orElseThrow(() -> new ItemNotFoundException("Attachment file not found: " + fileId));

            if (!file.getOrganisation().getOrganisationId().equals(organisation.getOrganisationId())) {
                throw new AccessDeniedException("File does not belong to this organisation: " + fileId);
            }

            if (Boolean.TRUE.equals(file.getIsDeleted())) {
                throw new ItemNotFoundException("Cannot attach deleted file: " + fileId);
            }
        }
    }



    private OrganisationMember validateProjectAndOrganisationAccess(AccountEntity account, ProjectEntity project, OrganisationEntity organisation) throws ItemNotFoundException {
        if (!project.getOrganisation().getOrganisationId().equals(organisation.getOrganisationId())) {
            throw new ItemNotFoundException("Project does not belong to this organisation");
        }

        OrganisationMember organisationMember = organisationMemberRepo.findByAccountAndOrganisation(account, project.getOrganisation())
                .orElseThrow(() -> new ItemNotFoundException("Member is not belong to this organisation"));

        if (organisationMember.getStatus() != MemberStatus.ACTIVE) {
            throw new ItemNotFoundException("Member is not active");
        }

        projectTeamMemberRepo.findByOrganisationMemberAndProject(organisationMember, project)
                .orElseThrow(() -> new ItemNotFoundException("Project team member not found"));

        return organisationMember;
    }

    private OrganisationMember validateOrganisationMemberAccess(AccountEntity account, OrganisationEntity organisation) throws ItemNotFoundException {
        OrganisationMember member = organisationMemberRepo.findByAccountAndOrganisation(account, organisation)
                .orElseThrow(() -> new ItemNotFoundException("Member is not belong to this organisation"));

        if (member.getStatus() != MemberStatus.ACTIVE) {
            throw new ItemNotFoundException("Member is not active");
        }

        return member;
    }

    public VoucherEntity approveVoucher(UUID organisationId, UUID voucherId)
            throws ItemNotFoundException, AccessDeniedException {

        AccountEntity currentUser = getAuthenticatedAccount();
        OrganisationEntity organisation = organisationRepo.findById(organisationId)
                .orElseThrow(() -> new ItemNotFoundException("Organisation not found"));

        VoucherEntity voucher = validateVoucherExists(voucherId, organisation);
        OrganisationMember member = validateProjectAndOrganisationAccess(currentUser, voucher.getProject(), organisation);
        permissionChecker.checkMemberPermission(member, "VOUCHERS", "approveVoucher");

        if (voucher.getStatus() != VoucherStatus.PENDING_APPROVAL) {
            throw new ItemNotFoundException("Only vouchers in PENDING_APPROVAL status can be approved");
        }

        // HARD BALANCE CHECK - block approval if insufficient balance
        performHardBalanceCheck(voucher.getAccount().getId(), voucher.getTotalAmount());

        voucher.setStatus(VoucherStatus.APPROVED);
        voucher.setApprovedAt(LocalDateTime.now());

        return voucherRepo.save(voucher);
    }

    // NEW METHOD: Add voucher spending method
    public VoucherEntity markVoucherAsSpent(UUID organisationId, UUID voucherId)
            throws ItemNotFoundException, AccessDeniedException {

        AccountEntity currentUser = getAuthenticatedAccount();
        OrganisationEntity organisation = organisationRepo.findById(organisationId)
                .orElseThrow(() -> new ItemNotFoundException("Organisation not found"));

        VoucherEntity voucher = validateVoucherExists(voucherId, organisation);
        OrganisationMember member = validateProjectAndOrganisationAccess(currentUser, voucher.getProject(), organisation);
        permissionChecker.checkMemberPermission(member, "VOUCHERS", "processPayment");

        if (voucher.getStatus() != VoucherStatus.APPROVED) {
            throw new ItemNotFoundException("Only approved vouchers can be marked as spent");
        }

        // Record spending in budget
        budgetSpendingService.recordSpendingFromVoucher(organisationId, voucher);

        voucher.setStatus(VoucherStatus.PAID);
        return voucherRepo.save(voucher);
    }

    // Helper methods
    private ChartOfAccounts validateVoucherAccount(UUID accountId, UUID organisationId)
            throws ItemNotFoundException {

        ChartOfAccounts account = chartOfAccountsRepo.findById(accountId)
                .orElseThrow(() -> new ItemNotFoundException("Account not found"));

        if (!account.getOrganisation().getOrganisationId().equals(organisationId)) {
            throw new ItemNotFoundException("Account does not belong to this organisation");
        }

        if (!account.getIsActive()) {
            throw new ItemNotFoundException("Account is not active");
        }

        if (account.getIsHeader()) {
            throw new ItemNotFoundException("Cannot create voucher for header account. Please select a detail account.");
        }

        return account;
    }

    private void performSoftBalanceCheck(UUID accountId, BigDecimal amount) {
        if (!budgetSpendingService.canSpendFromAccount(accountId, amount)) {
            BigDecimal available = budgetSpendingService.getAccountAvailableBalance(accountId);
            log.warn("Voucher created with insufficient balance. Account: {}, Available: {}, Requested: {}",
                    accountId, available, amount);
            // Could add notification or flag here
        }
    }

    private void performHardBalanceCheck(UUID accountId, BigDecimal amount)
            throws ItemNotFoundException {

        if (!budgetSpendingService.canSpendFromAccount(accountId, amount)) {
            BigDecimal available = budgetSpendingService.getAccountAvailableBalance(accountId);
            throw new ItemNotFoundException(String.format(
                    "Insufficient balance for approval. Available: TSh %s, Required: TSh %s",
                    available, amount));
        }
    }

}
