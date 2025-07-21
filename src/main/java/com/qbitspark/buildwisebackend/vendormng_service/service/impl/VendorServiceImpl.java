package com.qbitspark.buildwisebackend.vendormng_service.service.impl;

import com.qbitspark.buildwisebackend.accounting_service.documentflow.invoice.enums.ActionType;
import com.qbitspark.buildwisebackend.approval_service.enums.ServiceType;
import com.qbitspark.buildwisebackend.approval_service.service.ApprovalIntegrationService;
import com.qbitspark.buildwisebackend.approval_service.service.ApprovalWorkflowService;
import com.qbitspark.buildwisebackend.authentication_service.Repository.AccountRepo;
import com.qbitspark.buildwisebackend.authentication_service.entity.AccountEntity;
import com.qbitspark.buildwisebackend.globeadvice.exceptions.AccessDeniedException;
import com.qbitspark.buildwisebackend.organisation_service.roles_mng.service.PermissionCheckerService;
import com.qbitspark.buildwisebackend.vendormng_service.entity.VendorEntity;
import com.qbitspark.buildwisebackend.vendormng_service.enums.VendorStatus;
import com.qbitspark.buildwisebackend.vendormng_service.enums.VendorType;
import com.qbitspark.buildwisebackend.vendormng_service.payloads.*;
import com.qbitspark.buildwisebackend.vendormng_service.repo.VendorsRepo;
import com.qbitspark.buildwisebackend.vendormng_service.service.VendorService;
import com.qbitspark.buildwisebackend.globeadvice.exceptions.ItemNotFoundException;
import com.qbitspark.buildwisebackend.organisation_service.organisation_mng.entity.OrganisationEntity;
import com.qbitspark.buildwisebackend.organisation_service.organisation_mng.repo.OrganisationRepo;
import com.qbitspark.buildwisebackend.organisation_service.orgnisation_members_mng.entities.OrganisationMember;
import com.qbitspark.buildwisebackend.organisation_service.orgnisation_members_mng.enums.MemberStatus;
import com.qbitspark.buildwisebackend.organisation_service.orgnisation_members_mng.repo.OrganisationMemberRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class VendorServiceImpl implements VendorService {

    private final VendorsRepo vendorsRepo;
    private final OrganisationRepo organisationRepo;
    private final OrganisationMemberRepo organisationMemberRepo;
    private final AccountRepo accountRepo;
    private final PermissionCheckerService permissionChecker;
    private final ApprovalIntegrationService approvalIntegrationService;
    private final ApprovalWorkflowService approvalWorkflowService;

    @Override
    public VendorEntity createVendor(UUID organisationId, CreateVendorRequest request, ActionType action)
            throws ItemNotFoundException, AccessDeniedException {

        AccountEntity currentUser = getAuthenticatedAccount();

        OrganisationEntity organisation = organisationRepo.findById(organisationId)
                .orElseThrow(() -> new ItemNotFoundException("Organisation not found"));

        OrganisationMember member = validateOrganisationMemberAccess(currentUser, organisation);

        permissionChecker.checkMemberPermission(member, "VENDORS","createVendor");

        if (vendorsRepo.existsByNameIgnoreCaseAndOrganisation(
                request.getName(), organisation)) {
            throw new ItemNotFoundException("Vendor with this name already exists");
        }
        if (vendorsRepo.existsByNameIgnoreCaseAndOrganisation(
                request.getEmail(), organisation)) {
            throw new ItemNotFoundException("Vendor with this email already exists");
        }
        if (vendorsRepo.existsByNameIgnoreCaseAndOrganisation(
                request.getTin(), organisation)) {
            throw new ItemNotFoundException("Vendor with this TIN already exists");
        }


        VendorEntity vendor = new VendorEntity();
        vendor.setName(request.getName());
        vendor.setDescription(request.getDescription());
        vendor.setAddress(request.getAddress());
        vendor.setOfficePhone(request.getOfficePhone());
        vendor.setTin(request.getTin());
        vendor.setEmail(request.getEmail());
        vendor.setVendorType(request.getVendorType());
        vendor.setStatus(VendorStatus.DRAFT);
        vendor.setOrganisation(organisation);
        vendor.setBankDetails(request.getBankDetails());
        vendor.setAttachmentIds(request.getAttachmentIds() != null ?
                request.getAttachmentIds() : new ArrayList<>());

        VendorEntity savedVendor = vendorsRepo.save(vendor);

        // Handle approval workflow
        if (action == ActionType.SAVE_AND_APPROVAL) {
            // 1. Update document status via integration service
            approvalIntegrationService.submitForApproval(
                    ServiceType.VENDORS,
                    savedVendor.getVendorId(),
                    organisationId,
                    null
            );

            // 2. Start the workflow directly
            approvalWorkflowService.startApprovalWorkflow(
                    ServiceType.VENDORS,
                    savedVendor.getVendorId(),
                    organisationId,
                    null
            );
        }

        return savedVendor;
    }

    @Override
    public VendorEntity getVendorById(UUID organisationId, UUID vendorId)
            throws ItemNotFoundException, AccessDeniedException {


        AccountEntity currentUser = getAuthenticatedAccount();


        OrganisationEntity organisation = organisationRepo.findById(organisationId)
                .orElseThrow(() -> new ItemNotFoundException("Organisation not found"));

        OrganisationMember member = validateOrganisationMemberAccess(currentUser, organisation);

        permissionChecker.checkMemberPermission(member, "VENDORS","viewVendors");

        return vendorsRepo.findByVendorIdAndOrganisation(vendorId, organisation)
                .orElseThrow(() -> new ItemNotFoundException("Vendor not found"));
    }

    @Override
    public Page<VendorEntity> getAllVendors(UUID organisationId, VendorStatus status, Pageable pageable)
            throws ItemNotFoundException, AccessDeniedException {

        AccountEntity currentUser = getAuthenticatedAccount();

        OrganisationEntity organisation = organisationRepo.findById(organisationId)
                .orElseThrow(() -> new ItemNotFoundException("Organisation not found"));

        OrganisationMember member = validateOrganisationMemberAccess(currentUser, organisation);

        permissionChecker.checkMemberPermission(member, "VENDORS","viewVendors");

        if (status != null) {
            return vendorsRepo.findAllByOrganisationAndStatus(organisation, status, pageable);
        } else {
            return vendorsRepo.findAllByOrganisation(organisation, pageable);
        }
    }

    @Override
    public List<VendorEntity> getVendorSummaries(UUID organisationId, VendorType vendorType)
            throws ItemNotFoundException, AccessDeniedException {

        AccountEntity currentUser = getAuthenticatedAccount();

        OrganisationEntity organisation = organisationRepo.findById(organisationId)
                .orElseThrow(() -> new ItemNotFoundException("Organisation not found"));


        OrganisationMember member = validateOrganisationMemberAccess(currentUser, organisation);

        permissionChecker.checkMemberPermission(member, "VENDORS","viewVendors");

        if (vendorType != null) {
            return vendorsRepo.findAllByOrganisationAndStatusAndVendorTypeOrderByName(
                    organisation, VendorStatus.APPROVED, vendorType);
        } else {
            return vendorsRepo.findAllByOrganisationAndStatusOrderByName(
                    organisation, VendorStatus.APPROVED);
        }
    }

    @Override
    public VendorEntity updateVendor(UUID organisationId, UUID vendorId, UpdateVendorRequest request, ActionType action)
            throws ItemNotFoundException, AccessDeniedException {

        AccountEntity currentUser = getAuthenticatedAccount();
        OrganisationEntity organisation = validateOrganisationExists(organisationId);

        OrganisationMember member = validateOrganisationMemberAccess(currentUser, organisation);

        permissionChecker.checkMemberPermission(member, "VENDORS","updateVendor");

        VendorEntity vendor = validateVendorExists(vendorId, organisation);

        updateVendorFields(vendor, request, organisation, vendorId);

        VendorEntity savedVendor =  vendorsRepo.save(vendor);

        // Handle approval workflow
        if (action == ActionType.SAVE_AND_APPROVAL) {
            // 1. Update document status via integration service
            approvalIntegrationService.submitForApproval(
                    ServiceType.VENDORS,
                    savedVendor.getVendorId(),
                    organisationId,
                    null
            );

            // 2. Start the workflow directly
            approvalWorkflowService.startApprovalWorkflow(
                    ServiceType.VENDORS,
                    savedVendor.getVendorId(),
                    organisationId,
                    null
            );
        }

        return savedVendor;
    }


    @Override
    public void deleteVendor(UUID organisationId, UUID vendorId)
            throws ItemNotFoundException, AccessDeniedException {

        AccountEntity currentUser = getAuthenticatedAccount();

        OrganisationEntity organisation = organisationRepo.findById(organisationId)
                .orElseThrow(() -> new ItemNotFoundException("Organisation not found"));

        OrganisationMember member = validateOrganisationMemberAccess(currentUser, organisation);

        permissionChecker.checkMemberPermission(member, "VENDORS","deleteVendor");

        VendorEntity vendor = vendorsRepo.findByVendorIdAndOrganisation(vendorId, organisation)
                .orElseThrow(() -> new ItemNotFoundException("Vendor not found"));

        vendor.setStatus(VendorStatus.BLACKLISTED);
        vendorsRepo.save(vendor);

    }


    // ====================================================================
    // HELPER METHODS
    // ====================================================================

    private void updateVendorFields(VendorEntity vendor, UpdateVendorRequest request,
                                   OrganisationEntity organisation, UUID vendorId)
            throws ItemNotFoundException {

        updateVendorName(vendor, request.getName(), organisation, vendorId);
        updateVendorBasicFields(vendor, request);
        updateVendorTin(vendor, request.getTin(), organisation, vendorId);
        updateVendorEmail(vendor, request.getEmail(), organisation, vendorId);
        updateVendorEnumFields(vendor, request);
        updateVendorBankDetails(vendor, request.getBankDetails());
        updateVendorAttachments(vendor, request.getAttachmentIds());
        updateVendorStatus(vendor);
    }

    private void updateVendorStatus(VendorEntity vendor) {
            vendor.setStatus(VendorStatus.DRAFT);
    }

    private void updateVendorName(VendorEntity vendor, String newName,
                                  OrganisationEntity organisation, UUID vendorId)
            throws ItemNotFoundException {

        if (newName != null && !newName.trim().isEmpty()) {
            validateUniqueField("name", newName.trim(), organisation, vendorId);
            vendor.setName(newName.trim());
        }
    }

    private void updateVendorBasicFields(VendorEntity vendor, UpdateVendorRequest request) {
        if (request.getDescription() != null) {
            vendor.setDescription(request.getDescription());
        }
        if (request.getAddress() != null) {
            vendor.setAddress(request.getAddress());
        }
        if (request.getOfficePhone() != null) {
            vendor.setOfficePhone(request.getOfficePhone());
        }
    }

    private void updateVendorTin(VendorEntity vendor, String newTin,
                                 OrganisationEntity organisation, UUID vendorId)
            throws ItemNotFoundException {

        if (newTin != null && !newTin.trim().isEmpty()) {
            validateUniqueField("tin", newTin.trim(), organisation, vendorId);
            vendor.setTin(newTin.trim());
        }
    }

    private void updateVendorEmail(VendorEntity vendor, String newEmail,
                                   OrganisationEntity organisation, UUID vendorId)
            throws ItemNotFoundException {

        if (newEmail != null && !newEmail.trim().isEmpty()) {
            validateUniqueField("email", newEmail.trim(), organisation, vendorId);
            vendor.setEmail(newEmail.trim());
        }
    }

    private void updateVendorEnumFields(VendorEntity vendor, UpdateVendorRequest request) {
        if (request.getVendorType() != null) {
            vendor.setVendorType(request.getVendorType());
        }
        if (request.getStatus() != null) {
            vendor.setStatus(request.getStatus());
        }
    }

    private void updateVendorBankDetails(VendorEntity vendor, BankDetails bankDetails) {
        if (bankDetails != null) {
            vendor.setBankDetails(bankDetails);
        }
    }

    private void updateVendorAttachments(VendorEntity vendor, List<UUID> attachmentIds) {
        if (attachmentIds != null) {
            vendor.setAttachmentIds(attachmentIds);
        }
    }


    private AccountEntity getAuthenticatedAccount() throws ItemNotFoundException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return extractAccount(authentication);
    }

    private AccountEntity extractAccount(Authentication authentication) throws ItemNotFoundException {
        if (authentication != null && authentication.isAuthenticated()) {
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            String userName = userDetails.getUsername();
            return accountRepo.findByUserName(userName)
                    .orElseThrow(() -> new ItemNotFoundException("User given username does not exist"));
        }
        throw new ItemNotFoundException("User is not authenticated");
    }


    private OrganisationEntity validateOrganisationExists(UUID organisationId)
            throws ItemNotFoundException {
        return organisationRepo.findById(organisationId)
                .orElseThrow(() -> new ItemNotFoundException("Organisation not found"));
    }

    private VendorEntity validateVendorExists(UUID vendorId, OrganisationEntity organisation)
            throws ItemNotFoundException {
        return vendorsRepo.findByVendorIdAndOrganisation(vendorId, organisation)
                .orElseThrow(() -> new ItemNotFoundException("Vendor not found"));
    }

    private void validateUniqueField(String fieldName, String value,
                                     OrganisationEntity organisation, UUID excludeVendorId)
            throws ItemNotFoundException {

        boolean exists = switch (fieldName.toLowerCase()) {
            case "name" -> vendorsRepo.existsByNameIgnoreCaseAndOrganisationAndVendorIdNot(
                    value, organisation, excludeVendorId);
            case "email" -> vendorsRepo.existsByNameIgnoreCaseAndOrganisationAndVendorIdNot(
                    value, organisation,  excludeVendorId);
            case "tin" -> vendorsRepo.existsByNameIgnoreCaseAndOrganisationAndVendorIdNot(
                    value, organisation, excludeVendorId);
            default -> false;
        };

        if (exists) {
            throw new ItemNotFoundException("Vendor with this " + fieldName + " already exists");
        }
    }

    private OrganisationMember validateOrganisationMemberAccess(AccountEntity account, OrganisationEntity organisation) throws ItemNotFoundException {
        OrganisationMember member = organisationMemberRepo.findByAccountAndOrganisation(account, organisation)
                .orElseThrow(() -> new ItemNotFoundException("Member is not belong to this organisation"));

        if (member.getStatus() != MemberStatus.ACTIVE) {
            throw new ItemNotFoundException("Member is not active");
        }

        return member;
    }
}