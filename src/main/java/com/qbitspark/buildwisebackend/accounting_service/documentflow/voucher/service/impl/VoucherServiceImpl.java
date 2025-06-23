package com.qbitspark.buildwisebackend.accounting_service.documentflow.voucher.service.impl;

import com.qbitspark.buildwisebackend.accounting_service.coa.entity.JournalEntry;
import com.qbitspark.buildwisebackend.accounting_service.documentflow.voucher.adapter.VoucherToAccountingAdapter;
import com.qbitspark.buildwisebackend.accounting_service.documentflow.voucher.entity.VoucherAttachmentEntity;
import com.qbitspark.buildwisebackend.accounting_service.documentflow.voucher.entity.VoucherEntity;
import com.qbitspark.buildwisebackend.accounting_service.documentflow.voucher.entity.VoucherPayeeEntity;
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
    private final VoucherToAccountingAdapter voucherToAccountingAdapter;


    @Override
    public VoucherResponse createVoucher(UUID organisationId, CreateVoucherRequest request)
            throws ItemNotFoundException, AccessDeniedException {

        AccountEntity currentUser = getAuthenticatedAccount();

        OrganisationEntity organisation = organisationRepo.findById(organisationId)
                .orElseThrow(() -> new ItemNotFoundException("Organisation not found"));

        ProjectEntity project = projectRepo.findById(request.getProjectId())
                .orElseThrow(() -> new ItemNotFoundException("Project not found"));

        // Validate user permissions
        OrganisationMember organisationMember = validateOrganisationAccess(currentUser, organisation,
                List.of(MemberRole.OWNER, MemberRole.ADMIN, MemberRole.MEMBER));

        // Validate project belongs to this organisation
        validateProject(project, organisation);

        // Validate user is an active member of this project and have roles to do this?
        validateProjectMemberPermissions(currentUser, project,
                List.of(TeamMemberRole.ACCOUNTANT, TeamMemberRole.OWNER, TeamMemberRole.PROJECT_MANAGER));

        // Generate voucher number
        String voucherNumber = generateVoucherNumber(project, organisation);

        // Create voucher
        VoucherEntity voucher = new VoucherEntity();
        voucher.setVoucherNumber(voucherNumber);
        voucher.setVoucherDate(request.getVoucherDate().atStartOfDay());
        voucher.setVoucherType(request.getVoucherType());
        voucher.setPaymentMode(request.getPaymentMode());
        voucher.setOverallDescription(request.getOverallDescription());
        voucher.setCreatedBy(organisationMember);
        voucher.setOrganisation(organisation);
        voucher.setProject(project);
        voucher.setStatus(VoucherStatus.DRAFT);
        voucher.setCurrency("TSh");
        voucher.setCreatedAt(LocalDateTime.now());
        voucher.setUpdatedAt(LocalDateTime.now());

        // Create payees
        List<VoucherPayeeEntity> payeeEntities = new ArrayList<>();
        BigDecimal totalAmount = BigDecimal.ZERO;

        for (VoucherPayeeRequest payeeRequest : request.getPayees()) {
            VendorEntity vendor = vendorsRepo.findById(payeeRequest.getVendorId())
                    .orElseThrow(() -> new ItemNotFoundException("Vendor not found: " + payeeRequest.getVendorId()));

            // Validate vendor belongs to organisation
            if (!vendor.getOrganisation().getOrganisationId().equals(organisationId)) {
                throw new ItemNotFoundException("One of vendors does not belong to this organisation");
            }

            VoucherPayeeEntity payeeEntity = new VoucherPayeeEntity();
            payeeEntity.setVoucher(voucher);
            payeeEntity.setVendor(vendor);
            payeeEntity.setAmount(payeeRequest.getAmount());
            payeeEntity.setDescription(payeeRequest.getDescription());
            payeeEntity.setPaymentStatus(PaymentStatus.PENDING);

            payeeEntities.add(payeeEntity);
            totalAmount = totalAmount.add(payeeRequest.getAmount());
        }

        voucher.setPayees(payeeEntities);
        voucher.setTotalAmount(totalAmount);

        // Create attachments if provided
        if (request.getAttachments() != null && !request.getAttachments().isEmpty()) {
            List<VoucherAttachmentEntity> attachmentEntities = createVoucherAttachments(voucher, request.getAttachments());
            voucher.setAttachments(attachmentEntities);
        }

        VoucherEntity savedVoucher = voucherRepo.save(voucher);

        log.info("Voucher {} created for organisation {} with {} attachments",
                savedVoucher.getVoucherNumber(),
                organisation.getOrganisationName(),
                savedVoucher.getAttachments().size());

        return mapToVoucherResponse(savedVoucher);
    }

    @Override
    public VoucherResponse getVoucherById(UUID organisationId, UUID voucherId)
            throws ItemNotFoundException, AccessDeniedException {

        AccountEntity currentUser = getAuthenticatedAccount();

        OrganisationEntity organisation = organisationRepo.findById(organisationId)
                .orElseThrow(() -> new ItemNotFoundException("Organisation not found"));

        validateOrganisationAccess(currentUser, organisation,
                List.of(MemberRole.OWNER, MemberRole.ADMIN, MemberRole.MEMBER));

        VoucherEntity voucher = voucherRepo.findByIdAndOrganisation(voucherId, organisation)
                .orElseThrow(() -> new ItemNotFoundException("Voucher not found"));

        return mapToVoucherResponse(voucher);
    }

    @Override
    public VoucherResponse getVoucherByNumber(UUID organisationId, String voucherNumber)
            throws ItemNotFoundException, AccessDeniedException {

        AccountEntity currentUser = getAuthenticatedAccount();

        OrganisationEntity organisation = organisationRepo.findById(organisationId)
                .orElseThrow(() -> new ItemNotFoundException("Organisation not found"));

        validateOrganisationAccess(currentUser, organisation,
                List.of(MemberRole.OWNER, MemberRole.ADMIN, MemberRole.MEMBER));

        VoucherEntity voucher = voucherRepo.findByVoucherNumberAndOrganisation(voucherNumber, organisation)
                .orElseThrow(() -> new ItemNotFoundException("Voucher not found"));

        return mapToVoucherResponse(voucher);
    }

    @Override
    public List<VoucherSummaryResponse> getProjectVouchers(UUID organisationId, UUID projectId)
            throws ItemNotFoundException, AccessDeniedException {

        AccountEntity currentUser = getAuthenticatedAccount();

        OrganisationEntity organisation = organisationRepo.findById(organisationId)
                .orElseThrow(() -> new ItemNotFoundException("Organisation not found"));

        validateOrganisationAccess(currentUser, organisation,
                List.of(MemberRole.OWNER, MemberRole.ADMIN, MemberRole.MEMBER));

        ProjectEntity project = projectRepo.findById(projectId)
                .orElseThrow(() -> new ItemNotFoundException("Project not found"));

        if (!project.getOrganisation().getOrganisationId().equals(organisationId)) {
            throw new ItemNotFoundException("Project does not belong to this organisation");
        }

        List<VoucherEntity> vouchers = voucherRepo.findAllByProject(project);

        return vouchers.stream()
                .map(this::mapToVoucherSummaryResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<VoucherSummaryResponse> getOrganisationVouchers(UUID organisationId)
            throws ItemNotFoundException, AccessDeniedException {

        AccountEntity currentUser = getAuthenticatedAccount();

        OrganisationEntity organisation = organisationRepo.findById(organisationId)
                .orElseThrow(() -> new ItemNotFoundException("Organisation not found"));

        validateOrganisationAccess(currentUser, organisation,
                List.of(MemberRole.OWNER, MemberRole.ADMIN, MemberRole.MEMBER));

        List<VoucherEntity> vouchers = voucherRepo.findAllByOrganisation(organisation);

        return vouchers.stream()
                .map(this::mapToVoucherSummaryResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    @Override
    public VoucherResponse approveVoucher(UUID organisationId, UUID voucherId)
            throws Exception {

        AccountEntity currentUser = getAuthenticatedAccount();

        OrganisationEntity organisation = organisationRepo.findById(organisationId)
                .orElseThrow(() -> new ItemNotFoundException("Organisation not found"));

        // Only admins and owners can approve
        validateOrganisationAccess(currentUser, organisation,
                List.of(MemberRole.OWNER, MemberRole.ADMIN));

        VoucherEntity voucher = voucherRepo.findByIdAndOrganisation(voucherId, organisation)
                .orElseThrow(() -> new ItemNotFoundException("Voucher not found"));

        if (voucher.getStatus() != VoucherStatus.DRAFT && voucher.getStatus() != VoucherStatus.PENDING_APPROVAL) {
            throw new IllegalStateException("Voucher cannot be approved in current status: " + voucher.getStatus());
        }

        voucher.setStatus(VoucherStatus.APPROVED);
        voucher.setApprovedAt(LocalDateTime.now());

        VoucherEntity savedVoucher = voucherRepo.save(voucher);

        // NEW: Create accounting entry automatically
        JournalEntry journalEntry = voucherToAccountingAdapter.createAccountingEntry(savedVoucher);
        log.info("Voucher {} approved and journal entry {} created", savedVoucher.getVoucherNumber(), journalEntry.getId());


        return mapToVoucherResponse(savedVoucher);
    }


    @Override
    public PreviewVoucherNumberResponse previewVoucherNumber(UUID organisationId, PreviewVoucherNumberRequest request)
            throws ItemNotFoundException, AccessDeniedException {

        AccountEntity currentUser = getAuthenticatedAccount();

        OrganisationEntity organisation = organisationRepo.findById(organisationId)
                .orElseThrow(() -> new ItemNotFoundException("Organisation not found"));

        // Validate user permissions
        validateOrganisationAccess(currentUser, organisation,
                List.of(MemberRole.OWNER, MemberRole.ADMIN, MemberRole.MEMBER));

        ProjectEntity project = projectRepo.findById(request.getProjectId())
                .orElseThrow(() -> new ItemNotFoundException("Project not found"));

        // Validate project belongs to organisation
        if (!project.getOrganisation().getOrganisationId().equals(organisationId)) {
            throw new ItemNotFoundException("Project does not belong to this organisation");
        }

        String previewVoucherNumber = voucherNumberService.previewNextVoucherNumber(project, organisation);

        PreviewVoucherNumberResponse response = new PreviewVoucherNumberResponse();
        response.setNextVoucherNumber(previewVoucherNumber);
        response.setOrganisationName(organisation.getOrganisationName());
        response.setProjectName(project.getName());
        response.setProjectCode(project.getProjectCode());

        return response;
    }

    @Override
    public VoucherResponse updateVoucher(UUID organisationId, UUID voucherId, UpdateVoucherRequest request)
            throws ItemNotFoundException, AccessDeniedException {

        AccountEntity currentUser = getAuthenticatedAccount();

        OrganisationEntity organisation = organisationRepo.findById(organisationId)
                .orElseThrow(() -> new ItemNotFoundException("Organisation not found"));

        // Validate user permissions - only allow members+ to update
        OrganisationMember organisationMember = validateOrganisationAccess(currentUser, organisation,
                List.of(MemberRole.OWNER, MemberRole.ADMIN, MemberRole.MEMBER));

        // Get the existing voucher
        VoucherEntity existingVoucher = voucherRepo.findByIdAndOrganisation(voucherId, organisation)
                .orElseThrow(() -> new ItemNotFoundException("Voucher not found"));


        // BUSINESS RULE: Only allow updates for DRAFT vouchers
        if (existingVoucher.getStatus() != VoucherStatus.DRAFT) {
            throw new IllegalStateException("Only vouchers in DRAFT status can be updated. Current status: " + existingVoucher.getStatus());
        }


        validateProjectMemberPermissions(currentUser, existingVoucher.getProject(),
                List.of(TeamMemberRole.ACCOUNTANT, TeamMemberRole.OWNER, TeamMemberRole.PROJECT_MANAGER));


        // Update basic voucher fields (only if provided)
        if (request.getVoucherDate() != null) {
            existingVoucher.setVoucherDate(request.getVoucherDate().atStartOfDay());
        }

        if (request.getVoucherType() != null) {
            existingVoucher.setVoucherType(request.getVoucherType());
        }

        if (request.getPaymentMode() != null) {
            existingVoucher.setPaymentMode(request.getPaymentMode());
        }

        if (request.getOverallDescription() != null) {
            existingVoucher.setOverallDescription(request.getOverallDescription());
        }

        existingVoucher.setUpdatedAt(LocalDateTime.now());

        // Handle payees update only if payees are provided
        if (request.getPayees() != null && !request.getPayees().isEmpty()) {
            updateVoucherPayees(existingVoucher, request.getPayees(), organisationId);

            // Recalculate total amount only when payees are updated
            BigDecimal newTotalAmount = existingVoucher.getPayees().stream()
                    .map(VoucherPayeeEntity::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            existingVoucher.setTotalAmount(newTotalAmount);
        }

        // Handle attachments update if attachment operations are provided
        if (request.getAttachmentIdsToKeep() != null ||
                (request.getNewAttachments() != null && !request.getNewAttachments().isEmpty())) {
            updateVoucherAttachments(existingVoucher, request.getAttachmentIdsToKeep(), request.getNewAttachments());
        }

        VoucherEntity savedVoucher = voucherRepo.save(existingVoucher);

        log.info("Voucher {} updated by user {}", savedVoucher.getVoucherNumber(),
                organisationMember.getAccount().getUserName());

        return mapToVoucherResponse(savedVoucher);
    }


    // ====================================================================
    // HELPER METHODS
    // ====================================================================

    private String generateVoucherNumber(ProjectEntity project, OrganisationEntity organisation) {
        if (project != null) {
            return voucherNumberService.generateVoucherNumber(project, organisation);
        } else {
            // For non-project vouchers, create a simple organisation-based number
            return generateSimpleVoucherNumber(organisation);
        }
    }

    private String generateSimpleVoucherNumber(OrganisationEntity organisation) {
        return "ORG-VCH-" + LocalDateTime.now().getYear() +
                String.format("%02d", LocalDateTime.now().getMonthValue()) +
                String.format("%02d", LocalDateTime.now().getDayOfMonth()) +
                String.format("%03d", (int) (Math.random() * 1000));
    }

    private VoucherResponse mapToVoucherResponse(VoucherEntity voucher) {
        VoucherResponse response = new VoucherResponse();
        response.setId(voucher.getId());
        response.setVoucherNumber(voucher.getVoucherNumber());
        response.setVoucherDate(voucher.getVoucherDate());
        response.setVoucherType(voucher.getVoucherType());
        response.setStatus(voucher.getStatus());
        response.setPaymentMode(voucher.getPaymentMode());
        response.setTotalAmount(voucher.getTotalAmount());
        response.setCurrency(voucher.getCurrency());
        response.setOverallDescription(voucher.getOverallDescription());
        response.setCreatedById(voucher.getCreatedBy().getMemberId());
        response.setCreatedByName(voucher.getCreatedBy().getAccount().getUserName());
        response.setOrganisationId(voucher.getOrganisation().getOrganisationId());
        response.setOrganisationName(voucher.getOrganisation().getOrganisationName());
        response.setCreatedAt(voucher.getCreatedAt());
        response.setUpdatedAt(voucher.getUpdatedAt());

        // Project info
        if (voucher.getProject() != null) {
            response.setProjectId(voucher.getProject().getProjectId());
            response.setProjectName(voucher.getProject().getName());
        }

        // Map payees
        List<VoucherPayeeResponse> payeeResponses = voucher.getPayees().stream()
                .map(this::mapToVoucherPayeeResponse)
                .collect(Collectors.toList());
        response.setPayees(payeeResponses);

        // Map attachments
        List<VoucherAttachmentResponse> attachmentResponses = voucher.getAttachments().stream()
                .map(this::mapToVoucherAttachmentResponse)
                .collect(Collectors.toList());
        response.setAttachments(attachmentResponses);

        return response;
    }

    private VoucherPayeeResponse mapToVoucherPayeeResponse(VoucherPayeeEntity payee) {
        VoucherPayeeResponse response = new VoucherPayeeResponse();
        response.setId(payee.getId());
        response.setVendorId(payee.getVendor().getVendorId());
        response.setVendorName(payee.getVendor().getName());
        response.setVendorType(payee.getVendor().getVendorType());
        response.setAmount(payee.getAmount());
        response.setDescription(payee.getDescription());
        response.setPaymentStatus(payee.getPaymentStatus());
        response.setPaidAt(payee.getPaidAt());
        response.setPaymentReference(payee.getPaymentReference());
        return response;
    }

    private VoucherSummaryResponse mapToVoucherSummaryResponse(VoucherEntity voucher) {
        VoucherSummaryResponse response = new VoucherSummaryResponse();
        response.setVoucherId(voucher.getId());
        response.setVoucherNumber(voucher.getVoucherNumber());
        response.setStatus(voucher.getStatus());
        response.setTotalAmount(voucher.getTotalAmount());
        response.setPayeeCount(voucher.getPayees().size());
        response.setVoucherDate(voucher.getVoucherDate());
        response.setPreparedBy(voucher.getCreatedBy().getAccount().getUserName());

        if (voucher.getProject() != null) {
            response.setProjectName(voucher.getProject().getName());
        }

        return response;
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

    // Helper method to update voucher payees
    private void updateVoucherPayees(VoucherEntity voucher, List<VoucherPayeeRequest> newPayees, UUID organisationId)
            throws ItemNotFoundException {

        // Clear existing payees (cascade will handle deletion)
        voucher.getPayees().clear();

        // Add new payees
        List<VoucherPayeeEntity> payeeEntities = new ArrayList<>();

        for (VoucherPayeeRequest payeeRequest : newPayees) {
            VendorEntity vendor = vendorsRepo.findById(payeeRequest.getVendorId())
                    .orElseThrow(() -> new ItemNotFoundException("Vendor not found: " + payeeRequest.getVendorId()));

            // Validate vendor belongs to organisation
            if (!vendor.getOrganisation().getOrganisationId().equals(organisationId)) {
                throw new ItemNotFoundException("Vendor does not belong to this organisation");
            }

            VoucherPayeeEntity payeeEntity = new VoucherPayeeEntity();
            payeeEntity.setVoucher(voucher);
            payeeEntity.setVendor(vendor);
            payeeEntity.setAmount(payeeRequest.getAmount());
            payeeEntity.setDescription(payeeRequest.getDescription());
            payeeEntity.setPaymentStatus(PaymentStatus.PENDING); // Reset payment status for updates

            payeeEntities.add(payeeEntity);
        }

        voucher.setPayees(payeeEntities);
    }

    // Helper method to update voucher attachments
    private void updateVoucherAttachments(VoucherEntity voucher, List<UUID> attachmentIdsToKeep,
                                          List<VoucherAttachmentRequest> newAttachments) {

        // Handle removal of attachments not in the keep list
        if (attachmentIdsToKeep != null) {
            // Remove attachments that are not in the keep list
            voucher.getAttachments().removeIf(attachment ->
                    !attachmentIdsToKeep.contains(attachment.getId()));
        }

        // Handle adding new attachments
        if (newAttachments != null && !newAttachments.isEmpty()) {
            List<VoucherAttachmentEntity> attachmentEntities = new ArrayList<>();

            for (VoucherAttachmentRequest attachmentRequest : newAttachments) {
                VoucherAttachmentEntity attachmentEntity = new VoucherAttachmentEntity();
                attachmentEntity.setVoucher(voucher);
                attachmentEntity.setFilename(attachmentRequest.getFilename());
                attachmentEntity.setFileExtension(attachmentRequest.getFileExtension());
                attachmentEntity.setFileHash(attachmentRequest.getFileHash());
                attachmentEntity.setSystemDirectory(attachmentRequest.getSystemDirectory());
                attachmentEntity.setOriginalFilename(attachmentRequest.getOriginalFilename());
                attachmentEntity.setFileSize(attachmentRequest.getFileSize());
                attachmentEntity.setFilePathUrl(attachmentRequest.getFilePathUrl());

                attachmentEntities.add(attachmentEntity);
            }

            voucher.getAttachments().addAll(attachmentEntities);
        }
    }


    private List<VoucherAttachmentEntity> createVoucherAttachments(VoucherEntity voucher,
                                                                   List<VoucherAttachmentRequest> attachmentRequests) {
        List<VoucherAttachmentEntity> attachmentEntities = new ArrayList<>();

        for (VoucherAttachmentRequest attachmentRequest : attachmentRequests) {
            VoucherAttachmentEntity attachmentEntity = new VoucherAttachmentEntity();
            attachmentEntity.setVoucher(voucher);
            attachmentEntity.setFilename(attachmentRequest.getFilename());
            attachmentEntity.setFileExtension(attachmentRequest.getFileExtension());
            attachmentEntity.setFileHash(attachmentRequest.getFileHash());
            attachmentEntity.setSystemDirectory(attachmentRequest.getSystemDirectory());
            attachmentEntity.setOriginalFilename(attachmentRequest.getOriginalFilename());
            attachmentEntity.setFileSize(attachmentRequest.getFileSize());
            attachmentEntity.setFilePathUrl(attachmentRequest.getFilePathUrl());
            // uploadedAt will be set by @PrePersist

            attachmentEntities.add(attachmentEntity);
        }

        return attachmentEntities;
    }

    private VoucherAttachmentResponse mapToVoucherAttachmentResponse(VoucherAttachmentEntity attachment) {
        VoucherAttachmentResponse response = new VoucherAttachmentResponse();
        response.setId(attachment.getId());
        response.setFilename(attachment.getFilename());
        response.setFileExtension(attachment.getFileExtension());
        response.setFileHash(attachment.getFileHash());
        response.setSystemDirectory(attachment.getSystemDirectory());
        response.setOriginalFilename(attachment.getOriginalFilename());
        response.setFileSize(attachment.getFileSize());
        response.setFilePathUrl(attachment.getFilePathUrl());
        response.setUploadedAt(attachment.getUploadedAt());
        return response;
    }
}