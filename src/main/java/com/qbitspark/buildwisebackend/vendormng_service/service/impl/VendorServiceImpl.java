package com.qbitspark.buildwisebackend.vendormng_service.service.impl;

import com.qbitspark.buildwisebackend.authentication_service.Repository.AccountRepo;
import com.qbitspark.buildwisebackend.authentication_service.entity.AccountEntity;
import com.qbitspark.buildwisebackend.vendormng_service.entity.VendorEntity;
import com.qbitspark.buildwisebackend.vendormng_service.payloads.VendorResponse;
import com.qbitspark.buildwisebackend.vendormng_service.payloads.CreateVendorRequest;
import com.qbitspark.buildwisebackend.vendormng_service.payloads.ProjectResponseForVendor;
import com.qbitspark.buildwisebackend.vendormng_service.payloads.UpdateVendorRequest;
import com.qbitspark.buildwisebackend.vendormng_service.repo.VendorsRepo;
import com.qbitspark.buildwisebackend.vendormng_service.service.VendorService;
import com.qbitspark.buildwisebackend.globeadvice.exceptions.ItemNotFoundException;
import com.qbitspark.buildwisebackend.organisation_service.organisation_mng.entity.OrganisationEntity;
import com.qbitspark.buildwisebackend.organisation_service.organisation_mng.repo.OrganisationRepo;
import com.qbitspark.buildwisebackend.organisation_service.orgnisation_members_mng.entities.OrganisationMember;
import com.qbitspark.buildwisebackend.organisation_service.orgnisation_members_mng.enums.MemberRole;
import com.qbitspark.buildwisebackend.organisation_service.orgnisation_members_mng.enums.MemberStatus;
import com.qbitspark.buildwisebackend.organisation_service.orgnisation_members_mng.repo.OrganisationMemberRepo;
import com.qbitspark.buildwisebackend.projectmng_service.entity.ProjectEntity;
import com.qbitspark.buildwisebackend.projectmng_service.repo.ProjectRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class VendorServiceImpl implements VendorService {

    private final VendorsRepo vendorsRepo;
    private final OrganisationRepo organisationRepo;
    private final OrganisationMemberRepo organisationMemberRepo;
    private final AccountRepo accountRepo;
    private final ProjectRepo projectRepo;

    @Override
    public VendorResponse createVendorWithinOrganisation(UUID organisationId, CreateVendorRequest request) throws ItemNotFoundException {

        // Get organisation
        OrganisationEntity organisation = organisationRepo.findById(organisationId)
                .orElseThrow(() -> new ItemNotFoundException("Organisation not found"));

        // Validate user permissions (only OWNER/ADMIN can create vendors)
        AccountEntity currentUser = getAuthenticatedAccount();
        validateMemberPermissions(currentUser, organisation, List.of(MemberRole.OWNER, MemberRole.ADMIN));

        // Check for duplicates
        if (vendorsRepo.existsByNameIgnoreCaseAndOrganisationAndIsActiveTrue(request.getName(), organisation)) {
            throw new ItemNotFoundException("Vendor with this name already exists in the organisation");
        }
        if (vendorsRepo.existsByAddressIgnoreCaseAndOrganisationAndIsActiveTrue(request.getAddress(), organisation)) {
            throw new ItemNotFoundException("Vendor with this address already exists in the organisation");
        }
        if (vendorsRepo.existsByTinIgnoreCaseAndOrganisationAndIsActiveTrue(request.getTin(), organisation)) {
            throw new ItemNotFoundException("Vendor with this TIN already exists in the organisation");
        }
        if (vendorsRepo.existsByEmailIgnoreCaseAndOrganisationAndIsActiveTrue(request.getEmail(), organisation)) {
            throw new ItemNotFoundException("Vendor with this email already exists in the organisation");
        }

        // Create vendor
        VendorEntity vendor = new VendorEntity();
        vendor.setName(request.getName());
        vendor.setDescription(request.getDescription());
        vendor.setAddress(request.getAddress());
        vendor.setOfficePhone(request.getOfficePhone());
        vendor.setTin(request.getTin());
        vendor.setEmail(request.getEmail());
        vendor.setVendorType(request.getVendorType());
        vendor.setOrganisation(organisation);
        vendor.setIsActive(true);

        // Set bank details if provided
        vendor.setBankDetails(request.getBankDetails());

        VendorEntity savedVendor = vendorsRepo.save(vendor);

        log.info("Vendor {} created in organisation {}", savedVendor.getName(), organisation.getOrganisationName());

        return mapToResponse(savedVendor);
    }

    @Override
    public VendorResponse getVendorByIdWithinOrganisation(UUID vendorId) throws ItemNotFoundException {

        AccountEntity currentUser = getAuthenticatedAccount();

        // Get vendor
        VendorEntity vendor = vendorsRepo.findById(vendorId)
                .orElseThrow(() -> new ItemNotFoundException("Vendor not found"));

        // Get organisation
        OrganisationEntity organisation = vendor.getOrganisation();

        // Validate user permissions (only OWNER/ADMIN/MEMBER can view vendors)
        validateMemberPermissions(currentUser, organisation, List.of(MemberRole.OWNER, MemberRole.ADMIN, MemberRole.MEMBER));

        return mapToResponse(vendor);
    }

    @Override
    public List<VendorResponse> getAllVendorsWithinOrganisation(UUID organisationId) throws ItemNotFoundException {

        AccountEntity currentUser = getAuthenticatedAccount();

        // Get organisation
        OrganisationEntity organisation = organisationRepo.findById(organisationId)
                .orElseThrow(() -> new ItemNotFoundException("Organisation not found"));

        validateMemberPermissions(currentUser, organisation, List.of(MemberRole.OWNER, MemberRole.ADMIN, MemberRole.MEMBER));

        // Get all active vendors
        List<VendorEntity> vendors = vendorsRepo.findAll().stream()
                .filter(VendorEntity::getIsActive)
                .toList();

        return vendors.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public VendorResponse updateVendorWithinOrganisation(UUID vendorId, UpdateVendorRequest request) throws ItemNotFoundException {

        AccountEntity currentUser = getAuthenticatedAccount();

        // Get vendor
        VendorEntity vendor = vendorsRepo.findById(vendorId)
                .orElseThrow(() -> new ItemNotFoundException("Vendor not found"));

        // Validate user permissions (only OWNER/ADMIN can update vendors)
        validateMemberPermissions(currentUser, vendor.getOrganisation(), List.of(MemberRole.OWNER, MemberRole.ADMIN));

        // Update fields if provided
        if (request.getName() != null && !request.getName().trim().isEmpty()) {
            vendor.setName(request.getName().trim());
        }
        if (request.getDescription() != null) {
            vendor.setDescription(request.getDescription());
        }
        if (request.getAddress() != null) {
            vendor.setAddress(request.getAddress());
        }
        if (request.getOfficePhone() != null) {
            vendor.setOfficePhone(request.getOfficePhone());
        }
        if (request.getTin() != null) {
            vendor.setTin(request.getTin());
        }
        if (request.getEmail() != null) {
            vendor.setEmail(request.getEmail());
        }
        if (request.getVendorType() != null) {
            vendor.setVendorType(request.getVendorType());
        }
        if (request.getIsActive() != null) {
            vendor.setIsActive(request.getIsActive());
        }

        // Update bank details if provided
        if (request.getBankDetails() != null) {
            vendor.setBankDetails(request.getBankDetails());
        }

        VendorEntity updatedVendor = vendorsRepo.save(vendor);

        log.info("Vendor {} updated", updatedVendor.getName());

        return mapToResponse(updatedVendor);
    }

    @Override
    public void deleteVendorWithinOrganisation(UUID vendorId) throws ItemNotFoundException {

        AccountEntity currentUser = getAuthenticatedAccount();

        // Get vendor
        VendorEntity vendor = vendorsRepo.findById(vendorId)
                .orElseThrow(() -> new ItemNotFoundException("Vendor not found"));

        // Validate user permissions (only OWNER/ADMIN can delete vendors)
        validateMemberPermissions(currentUser, vendor.getOrganisation(), List.of(MemberRole.OWNER, MemberRole.ADMIN));

        // Softly delete - set inactive
        vendor.setIsActive(false);
        vendorsRepo.save(vendor);

        log.info("Vendor {} deleted (set inactive)", vendor.getName());
    }

//    @Override
//    public List<ProjectResponseForVendor> getVendorProjectsWithinOrganisation(UUID vendorId) throws ItemNotFoundException {
//
//        AccountEntity currentUser = getAuthenticatedAccount();
//
//        // Get vendor
//        VendorEntity vendor = vendorsRepo.findById(vendorId)
//                .orElseThrow(() -> new ItemNotFoundException("Vendor not found"));
//
//        // Validate user permissions (only OWNER/ADMIN can view vendor projects)
//        validateMemberPermissions(currentUser, vendor.getOrganisation(), List.of(MemberRole.OWNER, MemberRole.ADMIN));
//
//        // Get all projects for this vendor
//        List<ProjectEntity> projects = projectRepo.findAllByVendorAndOrganisation(vendor, vendor.getOrganisation());
//
//        // Map projects to vendor response format
//        return projects.stream()
//                .map(this::mapToProjectResponseForVendor)
//                .collect(Collectors.toList());
//    }

    // ====================================================================
    // HELPER METHODS
    // ====================================================================

    private VendorResponse mapToResponse(VendorEntity vendor) {
        VendorResponse response = new VendorResponse();
        response.setVendorId(vendor.getVendorId());
        response.setName(vendor.getName());
        response.setDescription(vendor.getDescription());
        response.setAddress(vendor.getAddress());
        response.setOfficePhone(vendor.getOfficePhone());
        response.setTin(vendor.getTin());
        response.setEmail(vendor.getEmail());
        response.setVendorType(vendor.getVendorType());
        response.setIsActive(vendor.getIsActive());
        response.setCreatedAt(vendor.getCreatedAt());
        response.setUpdatedAt(vendor.getUpdatedAt());
        //response.setTotalProjects(vendor.getProjects().size());

        // Map bank details
        response.setBankDetails(vendor.getBankDetails());

        return response;
    }

    private ProjectResponseForVendor mapToProjectResponseForVendor(ProjectEntity project) {
        ProjectResponseForVendor response = new ProjectResponseForVendor();

        response.setProjectId(project.getProjectId());
        response.setName(project.getName());
        response.setDescription(project.getDescription());
        response.setOrganisationName(project.getOrganisation().getOrganisationName());
        response.setOrganisationId(project.getOrganisation().getOrganisationId());
        response.setStatus(project.getStatus().toString());
        response.setContractNumber(project.getContractNumber());
        response.setCreatedAt(project.getCreatedAt());
        response.setUpdatedAt(project.getUpdatedAt());

        return response;
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

    private OrganisationMember validateMemberPermissions(AccountEntity account, OrganisationEntity organisation, List<MemberRole> allowedRoles) throws ItemNotFoundException {

        OrganisationMember member = organisationMemberRepo.findByAccountAndOrganisation(account, organisation)
                .orElseThrow(() -> new ItemNotFoundException("Member is not found in organisation"));

        if (member.getStatus() != MemberStatus.ACTIVE) {
            throw new ItemNotFoundException("Member is not active");
        }

        if (!allowedRoles.contains(member.getRole())) {
            throw new ItemNotFoundException("Member has insufficient permissions");
        }

        return member;
    }
}