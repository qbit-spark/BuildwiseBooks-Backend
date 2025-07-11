package com.qbitspark.buildwisebackend.accounting_service.tax_mng.service.impl;

import com.qbitspark.buildwisebackend.accounting_service.tax_mng.entity.TaxEntity;
import com.qbitspark.buildwisebackend.accounting_service.tax_mng.payload.CreateTaxRequest;
import com.qbitspark.buildwisebackend.accounting_service.tax_mng.payload.UpdateTaxRequest;
import com.qbitspark.buildwisebackend.accounting_service.tax_mng.payload.TaxResponse;
import com.qbitspark.buildwisebackend.accounting_service.tax_mng.repo.TaxRepo;
import com.qbitspark.buildwisebackend.accounting_service.tax_mng.service.TaxService;
import com.qbitspark.buildwisebackend.authentication_service.Repository.AccountRepo;
import com.qbitspark.buildwisebackend.authentication_service.entity.AccountEntity;
import com.qbitspark.buildwisebackend.globeadvice.exceptions.ItemNotFoundException;
import com.qbitspark.buildwisebackend.organisation_service.organisation_mng.entity.OrganisationEntity;
import com.qbitspark.buildwisebackend.organisation_service.organisation_mng.repo.OrganisationRepo;
import com.qbitspark.buildwisebackend.organisation_service.orgnisation_members_mng.entities.OrganisationMember;
import com.qbitspark.buildwisebackend.organisation_service.orgnisation_members_mng.enums.MemberStatus;
import com.qbitspark.buildwisebackend.organisation_service.orgnisation_members_mng.repo.OrganisationMemberRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TaxServiceIMPL implements TaxService {

    private final TaxRepo taxRepository;
    private final OrganisationRepo organisationRepository;
    private final AccountRepo accountRepo;
    private final OrganisationMemberRepo organisationMemberRepo;

    @Override
    @Transactional
    public TaxResponse createTax(UUID organisationId, CreateTaxRequest request) throws ItemNotFoundException {

        AccountEntity account = getAuthenticatedAccount();
        validateUserPermission(account, organisationId, true); // Admin only

        // Get organisation
        OrganisationEntity organisation = organisationRepository.findById(organisationId)
                .orElseThrow(() -> new ItemNotFoundException("Organisation not found"));

        // Check for duplicate tax name
        validateNoDuplicateTaxName(request.getTaxName(), organisationId, null);

        // Create tax entity
        TaxEntity taxEntity = createTaxFromRequest(request, organisation, account.getUserName());

        // Save tax
        TaxEntity savedTax = taxRepository.save(taxEntity);

        return toTaxResponse(savedTax);
    }

    @Override
    public List<TaxResponse> getAllTaxesByOrganisation(UUID organisationId) throws ItemNotFoundException {

        AccountEntity account = getAuthenticatedAccount();

        validateUserPermission(account, organisationId, false); // Any member can view

        List<TaxEntity> taxes = taxRepository.findByOrganisation_OrganisationId(organisationId);

        return taxes.stream()
                .map(this::toTaxResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<TaxResponse> getActiveTaxesByOrganisation(UUID organisationId) throws ItemNotFoundException {

        AccountEntity account = getAuthenticatedAccount();
        validateUserPermission(account, organisationId, false); // Any member can view

        List<TaxEntity> activeTaxes = taxRepository.findByOrganisation_OrganisationIdAndIsActiveTrue(organisationId);

        return activeTaxes.stream()
                .map(this::toTaxResponse)
                .collect(Collectors.toList());
    }

    @Override
    public TaxResponse getTaxById(UUID organisationId, UUID taxId) throws ItemNotFoundException {

        AccountEntity account = getAuthenticatedAccount();
        validateUserPermission(account, organisationId, false); // Any member can view

        TaxEntity tax = taxRepository.findByTaxIdAndOrganisation_OrganisationId(taxId, organisationId)
                .orElseThrow(() -> new ItemNotFoundException("Tax not found or does not belong to this organisation"));

        return toTaxResponse(tax);
    }

    @Override
    @Transactional
    public TaxResponse updateTax(UUID organisationId, UUID taxId, UpdateTaxRequest request) throws ItemNotFoundException {

        AccountEntity account = getAuthenticatedAccount();
        validateUserPermission(account, organisationId, true);

        // Get existing tax
        TaxEntity existingTax = taxRepository.findByTaxIdAndOrganisation_OrganisationId(taxId, organisationId)
                .orElseThrow(() -> new ItemNotFoundException("Tax not found or does not belong to this organisation"));

        // Check for duplicate tax name (excluding current tax)
        validateNoDuplicateTaxName(request.getTaxName(), organisationId, taxId);

        // Update tax
        existingTax.setTaxName(request.getTaxName());
        existingTax.setTaxPercent(request.getTaxPercent());
        existingTax.setTaxDescription(request.getTaxDescription());
        existingTax.setIsActive(request.getIsActive());
        existingTax.setModifiedDate(LocalDateTime.now());

        TaxEntity savedTax = taxRepository.save(existingTax);

        return toTaxResponse(savedTax);
    }

    @Override
    @Transactional
    public void deleteTax(UUID organisationId, UUID taxId) throws ItemNotFoundException {

        AccountEntity account = getAuthenticatedAccount();
        validateUserPermission(account, organisationId, true);

        // Get existing tax
        TaxEntity existingTax = taxRepository.findByTaxIdAndOrganisation_OrganisationId(taxId, organisationId)
                .orElseThrow(() -> new ItemNotFoundException("Tax not found or does not belong to this organisation"));

        // Hard delete - completely remove from database
        taxRepository.delete(existingTax);
    }

    /**
     * Validate no duplicate tax names within the same organisation (case-sensitive)
     */
    private void validateNoDuplicateTaxName(String taxName, UUID organisationId, UUID excludeTaxId)
            throws ItemNotFoundException {

        List<TaxEntity> allTaxes = taxRepository.findByOrganisation_OrganisationId(organisationId);

        boolean duplicateExists = allTaxes.stream()
                .filter(tax -> !tax.getTaxId().equals(excludeTaxId))
                .anyMatch(tax -> tax.getTaxName().equalsIgnoreCase(taxName));

        if (duplicateExists) {
            throw new ItemNotFoundException("Tax with name '" + taxName + "' already exists in this organisation (case-insensitive).");
        }
    }


    /**
     * Create a tax entity from request
     */
    private TaxEntity createTaxFromRequest(CreateTaxRequest request, OrganisationEntity organisation, String createdBy) {
        TaxEntity taxEntity = new TaxEntity();
        taxEntity.setTaxName(request.getTaxName());
        taxEntity.setTaxPercent(request.getTaxPercent());
        taxEntity.setTaxDescription(request.getTaxDescription());
        taxEntity.setOrganisation(organisation);
        taxEntity.setIsActive(request.getIsActive());
        taxEntity.setCreatedDate(LocalDateTime.now());
        taxEntity.setCreatedBy(createdBy);
        return taxEntity;
    }

    /**
     * Convert tax entity to response DTO
     */
    private TaxResponse toTaxResponse(TaxEntity taxEntity) {
        return TaxResponse.builder()
                .taxId(taxEntity.getTaxId())
                .taxName(taxEntity.getTaxName())
                .taxPercent(taxEntity.getTaxPercent())
                .taxDescription(taxEntity.getTaxDescription())
                .isActive(taxEntity.getIsActive())
                .createdDate(taxEntity.getCreatedDate())
                .modifiedDate(taxEntity.getModifiedDate())
                .createdBy(taxEntity.getCreatedBy())
                .organisationId(taxEntity.getOrganisation().getOrganisationId())
                .organisationName(taxEntity.getOrganisation().getOrganisationName())
                .build();
    }

    /**
     * Validate user permissions
     */
    private void validateUserPermission(AccountEntity account, UUID organisationId, boolean adminOnly)
            throws ItemNotFoundException, AccessDeniedException {

        OrganisationEntity organisation = organisationRepository.findById(organisationId)
                .orElseThrow(() -> new ItemNotFoundException("Organisation not found"));

        if (organisation.getOwner().equals(account)) {
            return; // Owner always has access
        }

        Optional<OrganisationMember> memberOptional = organisationMemberRepo.findByAccountAndOrganisation(account, organisation);

        if (memberOptional.isEmpty()) {
            throw new AccessDeniedException("User is not a member of this organisation");
        }

        OrganisationMember member = memberOptional.get();

        if (member.getStatus() != MemberStatus.ACTIVE) {
            throw new AccessDeniedException("User membership is not active");
        }

        if (adminOnly) {
            MemberRole role = member.getRole();
            if (role != MemberRole.OWNER && role != MemberRole.ADMIN) {
                throw new AccessDeniedException("User does not have sufficient permissions for this operation");
            }
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

            Optional<AccountEntity> userOptional = accountRepo.findByUserName(userName);
            if (userOptional.isPresent()) {
                return userOptional.get();
            } else {
                throw new ItemNotFoundException("User with given userName does not exist");
            }
        } else {
            throw new ItemNotFoundException("User is not authenticated");
        }
    }
}