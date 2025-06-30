package com.qbitspark.buildwisebackend.accounting_service.deducts_mng.service.impl;

import com.qbitspark.buildwisebackend.accounting_service.deducts_mng.entity.DeductsEntity;
import com.qbitspark.buildwisebackend.accounting_service.deducts_mng.payload.CreateDeductRequest;
import com.qbitspark.buildwisebackend.accounting_service.deducts_mng.payload.UpdateDeductRequest;
import com.qbitspark.buildwisebackend.accounting_service.deducts_mng.payload.DeductResponse;
import com.qbitspark.buildwisebackend.accounting_service.deducts_mng.repo.DeductRepo;
import com.qbitspark.buildwisebackend.accounting_service.deducts_mng.service.DeductService;
import com.qbitspark.buildwisebackend.authentication_service.Repository.AccountRepo;
import com.qbitspark.buildwisebackend.authentication_service.entity.AccountEntity;
import com.qbitspark.buildwisebackend.globeadvice.exceptions.ItemNotFoundException;
import com.qbitspark.buildwisebackend.organisation_service.organisation_mng.entity.OrganisationEntity;
import com.qbitspark.buildwisebackend.organisation_service.organisation_mng.repo.OrganisationRepo;
import com.qbitspark.buildwisebackend.organisation_service.orgnisation_members_mng.entities.OrganisationMember;
import com.qbitspark.buildwisebackend.organisation_service.orgnisation_members_mng.enums.MemberRole;
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
public class DeductServiceIMPL implements DeductService {

    private final DeductRepo deductRepository;
    private final OrganisationRepo organisationRepository;
    private final AccountRepo accountRepo;
    private final OrganisationMemberRepo organisationMemberRepo;

    @Override
    @Transactional
    public DeductResponse createDeduct(UUID organisationId, CreateDeductRequest request) throws ItemNotFoundException {

        AccountEntity account = getAuthenticatedAccount();
        validateUserPermission(account, organisationId, true); // Admin only

        // Get organisation
        OrganisationEntity organisation = organisationRepository.findById(organisationId)
                .orElseThrow(() -> new ItemNotFoundException("Organisation not found"));

        // Check for duplicate deduct name
        validateNoDuplicateDeductName(request.getDeductName(), organisationId, null);

        // Create deduct entity
        DeductsEntity deductEntity = createDeductFromRequest(request, organisation, account.getUserName());

        // Save deduct
        DeductsEntity savedDeduct = deductRepository.save(deductEntity);

        return toDeductResponse(savedDeduct);
    }

    @Override
    public List<DeductResponse> getAllDeductsByOrganisation(UUID organisationId) throws ItemNotFoundException {

        AccountEntity account = getAuthenticatedAccount();
        validateUserPermission(account, organisationId, false); // Any member can view

        List<DeductsEntity> deducts = deductRepository.findByOrganisation_OrganisationId(organisationId);

        return deducts.stream()
                .map(this::toDeductResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<DeductResponse> getActiveDeductsByOrganisation(UUID organisationId) throws ItemNotFoundException {

        AccountEntity account = getAuthenticatedAccount();
        validateUserPermission(account, organisationId, false); // Any member can view

        List<DeductsEntity> activeDeducts = deductRepository.findByOrganisation_OrganisationIdAndIsActiveTrue(organisationId);

        return activeDeducts.stream()
                .map(this::toDeductResponse)
                .collect(Collectors.toList());
    }

    @Override
    public DeductResponse getDeductById(UUID organisationId, UUID deductId) throws ItemNotFoundException {

        AccountEntity account = getAuthenticatedAccount();
        validateUserPermission(account, organisationId, false); // Any member can view

        DeductsEntity deduct = deductRepository.findByDeductIdAndOrganisation_OrganisationId(deductId, organisationId)
                .orElseThrow(() -> new ItemNotFoundException("Deduct not found or does not belong to this organisation"));

        return toDeductResponse(deduct);
    }

    @Override
    @Transactional
    public DeductResponse updateDeduct(UUID organisationId, UUID deductId, UpdateDeductRequest request) throws ItemNotFoundException {

        AccountEntity account = getAuthenticatedAccount();
        validateUserPermission(account, organisationId, true);

        // Get existing deduct
        DeductsEntity existingDeduct = deductRepository.findByDeductIdAndOrganisation_OrganisationId(deductId, organisationId)
                .orElseThrow(() -> new ItemNotFoundException("Deduct not found or does not belong to this organisation"));

        // Check for duplicate deduct name (excluding current deduct)
        validateNoDuplicateDeductName(request.getDeductName(), organisationId, deductId);

        // Update deduct
        existingDeduct.setDeductName(request.getDeductName());
        existingDeduct.setDeductPercent(request.getDeductPercent());
        existingDeduct.setDeductDescription(request.getDeductDescription());
        existingDeduct.setIsActive(request.getIsActive());
        existingDeduct.setModifiedDate(LocalDateTime.now());

        DeductsEntity savedDeduct = deductRepository.save(existingDeduct);

        return toDeductResponse(savedDeduct);
    }

    @Override
    @Transactional
    public void deleteDeduct(UUID organisationId, UUID deductId) throws ItemNotFoundException {

        AccountEntity account = getAuthenticatedAccount();
        validateUserPermission(account, organisationId, true);

        // Get existing deduct
        DeductsEntity existingDeduct = deductRepository.findByDeductIdAndOrganisation_OrganisationId(deductId, organisationId)
                .orElseThrow(() -> new ItemNotFoundException("Deduct not found or does not belong to this organisation"));

        // Hard delete - completely remove from database
        deductRepository.delete(existingDeduct);
    }

    /**
     * Validate no duplicate deduct names within the same organisation (case-insensitive)
     */
    private void validateNoDuplicateDeductName(String deductName, UUID organisationId, UUID excludeDeductId)
            throws ItemNotFoundException {

        List<DeductsEntity> allDeducts = deductRepository.findByOrganisation_OrganisationId(organisationId);

        boolean duplicateExists = allDeducts.stream()
                .filter(deduct -> !deduct.getDeductId().equals(excludeDeductId))
                .anyMatch(deduct -> deduct.getDeductName().equalsIgnoreCase(deductName));

        if (duplicateExists) {
            throw new ItemNotFoundException("Deduct with name '" + deductName + "' already exists in this organisation (case-insensitive).");
        }
    }

    /**
     * Create a deduct entity from request
     */
    private DeductsEntity createDeductFromRequest(CreateDeductRequest request, OrganisationEntity organisation, String createdBy) {
        DeductsEntity deductEntity = new DeductsEntity();
        deductEntity.setDeductName(request.getDeductName());
        deductEntity.setDeductPercent(request.getDeductPercent());
        deductEntity.setDeductDescription(request.getDeductDescription());
        deductEntity.setOrganisation(organisation);
        deductEntity.setIsActive(request.getIsActive());
        deductEntity.setCreatedDate(LocalDateTime.now());
        deductEntity.setCreatedBy(createdBy);
        return deductEntity;
    }

    /**
     * Convert deduct entity to response DTO
     */
    private DeductResponse toDeductResponse(DeductsEntity deductEntity) {
        return DeductResponse.builder()
                .deductId(deductEntity.getDeductId())
                .deductName(deductEntity.getDeductName())
                .deductPercent(deductEntity.getDeductPercent())
                .deductDescription(deductEntity.getDeductDescription())
                .isActive(deductEntity.getIsActive())
                .createdDate(deductEntity.getCreatedDate())
                .modifiedDate(deductEntity.getModifiedDate())
                .createdBy(deductEntity.getCreatedBy())
                .organisationId(deductEntity.getOrganisation().getOrganisationId())
                .organisationName(deductEntity.getOrganisation().getOrganisationName())
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
