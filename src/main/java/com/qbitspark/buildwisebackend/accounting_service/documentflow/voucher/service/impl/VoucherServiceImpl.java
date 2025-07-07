package com.qbitspark.buildwisebackend.accounting_service.documentflow.voucher.service.impl;

import com.qbitspark.buildwisebackend.accounting_service.budget_mng.org_budget.entity.OrgBudgetDetailAllocationEntity;
import com.qbitspark.buildwisebackend.accounting_service.budget_mng.org_budget.enums.OrgBudgetStatus;
import com.qbitspark.buildwisebackend.accounting_service.budget_mng.org_budget.repo.OrgBudgetDetailAllocationRepo;
import com.qbitspark.buildwisebackend.accounting_service.budget_mng.project_budget.entity.ProjectBudgetLineItemEntity;
import com.qbitspark.buildwisebackend.accounting_service.budget_mng.project_budget.enums.ProjectBudgetStatus;
import com.qbitspark.buildwisebackend.accounting_service.budget_mng.project_budget.repo.ProjectBudgetLineItemRepo;
import com.qbitspark.buildwisebackend.accounting_service.deducts_mng.entity.DeductsEntity;
import com.qbitspark.buildwisebackend.accounting_service.deducts_mng.repo.DeductRepo;
import com.qbitspark.buildwisebackend.accounting_service.documentflow.voucher.entity.VoucherBeneficiaryEntity;
import com.qbitspark.buildwisebackend.accounting_service.documentflow.voucher.entity.VoucherDeductionEntity;
import com.qbitspark.buildwisebackend.accounting_service.documentflow.voucher.entity.VoucherEntity;
import com.qbitspark.buildwisebackend.accounting_service.documentflow.voucher.enums.VoucherStatus;
import com.qbitspark.buildwisebackend.accounting_service.documentflow.voucher.paylaod.*;
import com.qbitspark.buildwisebackend.accounting_service.documentflow.voucher.repo.VoucherBeneficiaryRepo;
import com.qbitspark.buildwisebackend.accounting_service.documentflow.voucher.repo.VoucherRepo;
import com.qbitspark.buildwisebackend.accounting_service.documentflow.voucher.service.VoucherNumberService;
import com.qbitspark.buildwisebackend.accounting_service.documentflow.voucher.service.VoucherService;
import com.qbitspark.buildwisebackend.authentication_service.Repository.AccountRepo;
import com.qbitspark.buildwisebackend.authentication_service.entity.AccountEntity;
import com.qbitspark.buildwisebackend.drive_mng.entity.OrgFileEntity;
import com.qbitspark.buildwisebackend.drive_mng.repo.OrgFileRepo;
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
    private final ProjectBudgetLineItemRepo projectBudgetLineItemRepo;
    private final DeductRepo deductRepo;
    private final VoucherBeneficiaryRepo voucherBeneficiaryRepo;
    private final OrgFileRepo orgFileRepo;
    private final OrgBudgetDetailAllocationRepo allocationRepo;

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

        OrgBudgetDetailAllocationEntity detailAllocation = validateAndGetDetailAllocation(
                request.getDetailAllocationId(), organisationId);

        String voucherNumber = voucherNumberService.generateVoucherNumber(project, organisation);

        List<VoucherBeneficiaryEntity> beneficiaryEntities = new ArrayList<>();
        BigDecimal totalAmount = BigDecimal.ZERO;
        BigDecimal totalDeductions = BigDecimal.ZERO;

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
            BigDecimal beneficiaryDeductionTotal = BigDecimal.ZERO;

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

                beneficiaryDeductionTotal = beneficiaryDeductionTotal.add(deductionAmount);
            }

            beneficiaryEntity.setDeductions(deductionEntities);
            beneficiaryEntities.add(beneficiaryEntity);

            totalAmount = totalAmount.add(beneficiaryRequest.getAmount());
            totalDeductions = totalDeductions.add(beneficiaryDeductionTotal);
        }

        validateBudgetAvailability(detailAllocation, totalAmount);

        VoucherEntity voucher = new VoucherEntity();
        voucher.setVoucherNumber(voucherNumber);
        voucher.setVoucherDate(LocalDateTime.now());
        voucher.setOverallDescription(request.getGeneralDescription());
        voucher.setCreatedBy(organisationMember);
        voucher.setOrganisation(organisation);
        voucher.setProject(project);
        voucher.setDetailAllocation(detailAllocation);
        voucher.setStatus(VoucherStatus.DRAFT);
        voucher.setCurrency("TSh");
        voucher.setAttachments(request.getAttachments());

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

        VoucherEntity existingVoucher = validateVoucherExists(voucherId, organisation);

        validateVoucherCanBeUpdated(existingVoucher);

        validateProjectMemberPermissions(currentUser, existingVoucher.getProject(),
                List.of(TeamMemberRole.ACCOUNTANT, TeamMemberRole.OWNER, TeamMemberRole.PROJECT_MANAGER));

        updateVoucherBasicFields(existingVoucher, request);

        if (request.getDetailAllocationId() != null) {
            updateVoucherDetailAllocation(existingVoucher, request.getDetailAllocationId(), organisationId);
        }

        if (request.getBeneficiaries() != null && !request.getBeneficiaries().isEmpty()) {
            updateVoucherBeneficiaries(existingVoucher, request.getBeneficiaries(), organisationId);
        }

        if (request.getAttachments() != null) {
            updateVoucherAttachments(existingVoucher, request.getAttachments(), organisation);
        }

        VoucherEntity updatedVoucher = voucherRepo.save(existingVoucher);

        log.info("Voucher {} updated by user {}",
                updatedVoucher.getVoucherNumber(), organisationMember.getAccount().getUserName());

        return updatedVoucher;
    }



    @Override
    public VoucherEntity getVoucherById(UUID organisationId, UUID voucherId) throws ItemNotFoundException, AccessDeniedException {
        AccountEntity currentUser = getAuthenticatedAccount();
        OrganisationEntity organisation = validateOrganisationExists(organisationId);
        OrganisationMember organisationMember = validateOrganisationAccess(currentUser, organisation,
                List.of(MemberRole.OWNER, MemberRole.ADMIN, MemberRole.MEMBER));

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

    private void validateProjectMemberPermissions(AccountEntity account, ProjectEntity project, List<TeamMemberRole> allowedRoles) throws ItemNotFoundException, AccessDeniedException {

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

    private void validateBudgetAvailability(OrgBudgetDetailAllocationEntity allocation,
                                            BigDecimal requestedAmount) throws ItemNotFoundException {

        BigDecimal availableBudget = allocation.getRemainingAmount();

        if (availableBudget.compareTo(requestedAmount) < 0) {
            throw new ItemNotFoundException(String.format(
                    "Insufficient budget for %s - %s. Available: TSh %s, Requested: TSh %s",
                    allocation.getDetailAccountCode(),
                    allocation.getDetailAccountName(),
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

    private void updateVoucherDetailAllocation(VoucherEntity voucher, UUID newDetailAllocationId, UUID organisationId)
            throws ItemNotFoundException {

        OrgBudgetDetailAllocationEntity newDetailAllocation = validateAndGetDetailAllocation(
                newDetailAllocationId, organisationId);

        voucher.setDetailAllocation(newDetailAllocation);
    }

    private void updateVoucherBeneficiaries(VoucherEntity voucher, List<VoucherBeneficiaryRequest> newBeneficiaries,
                                            UUID organisationId) throws ItemNotFoundException {

        BigDecimal newTotalAmount = newBeneficiaries.stream()
                .map(VoucherBeneficiaryRequest::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        validateBudgetAvailability(voucher.getDetailAllocation(), newTotalAmount);

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

    private OrgBudgetDetailAllocationEntity validateAndGetDetailAllocation(
            UUID detailAllocationId, UUID organisationId) throws ItemNotFoundException {

        OrgBudgetDetailAllocationEntity allocation = allocationRepo.findById(detailAllocationId)
                .orElseThrow(() -> new ItemNotFoundException("Detail allocation not found"));

        if (!allocation.getHeaderLineItem().getOrgBudget().getOrganisation()
                .getOrganisationId().equals(organisationId)) {
            throw new ItemNotFoundException("Detail allocation does not belong to this organisation");
        }

        if (!allocation.hasAllocation()) {
            throw new ItemNotFoundException("No budget allocated to this detail account");
        }

        if (allocation.getHeaderLineItem().getOrgBudget().getStatus() != OrgBudgetStatus.ACTIVE) {
            throw new ItemNotFoundException("Organisation budget is not active");
        }

        return allocation;
    }
}
