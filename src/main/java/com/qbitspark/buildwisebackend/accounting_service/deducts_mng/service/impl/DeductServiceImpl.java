package com.qbitspark.buildwisebackend.accounting_service.deducts_mng.service.impl;

import com.qbitspark.buildwisebackend.accounting_service.deducts_mng.entity.DeductsEntity;
import com.qbitspark.buildwisebackend.accounting_service.deducts_mng.payload.CreateDeductRequest;
import com.qbitspark.buildwisebackend.accounting_service.deducts_mng.payload.UpdateDeductRequest;
import com.qbitspark.buildwisebackend.accounting_service.deducts_mng.payload.DeductResponse;
import com.qbitspark.buildwisebackend.accounting_service.deducts_mng.repo.DeductRepo;
import com.qbitspark.buildwisebackend.accounting_service.deducts_mng.service.DeductService;
import com.qbitspark.buildwisebackend.authentication_service.repo.AccountRepo;
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

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DeductServiceImpl implements DeductService {

    private final DeductRepo deductRepository;
    private final OrganisationRepo organisationRepo;
    private final AccountRepo accountRepo;
    private final OrganisationMemberRepo organisationMemberRepo;
    private final PermissionCheckerService permissionChecker;

    @Override
    @Transactional
    public DeductResponse createDeduct(UUID organisationId, CreateDeductRequest request) throws ItemNotFoundException, AccessDeniedException {

        AccountEntity currentUser = getAuthenticatedAccount();

        OrganisationEntity organisation = organisationRepo.findById(organisationId).orElseThrow(() -> new ItemNotFoundException("Organisation not found"));

        OrganisationMember member = validateOrganisationMemberAccess(currentUser, organisation);

        permissionChecker.checkMemberPermission(member, "DEDUCTS","createDeducts");

        validateNoDuplicateDeductName(request.getDeductName(), organisationId, null);

        DeductsEntity deductEntity = createDeductFromRequest(request, organisation, currentUser.getUserName());

        DeductsEntity savedDeduct = deductRepository.save(deductEntity);

        return toDeductResponse(savedDeduct);
    }

    @Override
    public List<DeductResponse> getAllDeductsByOrganisation(UUID organisationId) throws ItemNotFoundException, AccessDeniedException {

        AccountEntity currentUser = getAuthenticatedAccount();

        OrganisationEntity organisation = organisationRepo.findById(organisationId).orElseThrow(() -> new ItemNotFoundException("Organisation not found"));

        OrganisationMember member = validateOrganisationMemberAccess(currentUser, organisation);

        permissionChecker.checkMemberPermission(member, "DEDUCTS","viewDeducts");

        List<DeductsEntity> deducts = deductRepository.findByOrganisation_OrganisationId(organisationId);

        return deducts.stream()
                .map(this::toDeductResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<DeductResponse> getActiveDeductsByOrganisation(UUID organisationId) throws ItemNotFoundException, AccessDeniedException {

        AccountEntity currentUser = getAuthenticatedAccount();

        OrganisationEntity organisation = organisationRepo.findById(organisationId).orElseThrow(() -> new ItemNotFoundException("Organisation not found"));

        OrganisationMember member = validateOrganisationMemberAccess(currentUser, organisation);

        permissionChecker.checkMemberPermission(member, "DEDUCTS","viewDeducts");

        List<DeductsEntity> activeDeducts = deductRepository.findByOrganisation_OrganisationIdAndIsActiveTrue(organisationId);

        return activeDeducts.stream()
                .map(this::toDeductResponse)
                .collect(Collectors.toList());
    }

    @Override
    public DeductResponse getDeductById(UUID organisationId, UUID deductId) throws ItemNotFoundException, AccessDeniedException {

        AccountEntity currentUser = getAuthenticatedAccount();

        OrganisationEntity organisation = organisationRepo.findById(organisationId).orElseThrow(() -> new ItemNotFoundException("Organisation not found"));

        OrganisationMember member = validateOrganisationMemberAccess(currentUser, organisation);

        permissionChecker.checkMemberPermission(member, "DEDUCTS","viewDeducts");

        DeductsEntity deduct = deductRepository.findByDeductIdAndOrganisation_OrganisationId(deductId, organisationId)
                .orElseThrow(() -> new ItemNotFoundException("Deduct not found or does not belong to this organisation"));

        return toDeductResponse(deduct);
    }
    @Override
    @Transactional
    public DeductResponse updateDeduct(UUID organisationId, UUID deductId, UpdateDeductRequest request) throws ItemNotFoundException, AccessDeniedException {

        AccountEntity currentUser = getAuthenticatedAccount();

        OrganisationEntity organisation = organisationRepo.findById(organisationId).orElseThrow(() -> new ItemNotFoundException("Organisation not found"));

        OrganisationMember member = validateOrganisationMemberAccess(currentUser, organisation);

        permissionChecker.checkMemberPermission(member, "DEDUCTS","updateDeducts");

        // Get existing deduct
        DeductsEntity existingDeduct = deductRepository.findByDeductIdAndOrganisation_OrganisationId(deductId, organisationId)
                .orElseThrow(() -> new ItemNotFoundException("Deduct not found or does not belong to this organisation"));

        // Only update deduct name if provided and different
        if (request.getDeductName() != null && !request.getDeductName().trim().isEmpty()) {
            // Check for duplicate deduct name only if name is being changed
            if (!existingDeduct.getDeductName().equals(request.getDeductName().trim())) {
                validateNoDuplicateDeductName(request.getDeductName().trim(), organisationId, deductId);
                existingDeduct.setDeductName(request.getDeductName().trim());
            }
        }

        // Only update percentage if provided
        if (request.getDeductPercent() != null) {
            existingDeduct.setDeductPercent(request.getDeductPercent());
        }

        // Update description (allow null/empty to clear description)
        if (request.getDeductDescription() != null) {
            existingDeduct.setDeductDescription(request.getDeductDescription().trim().isEmpty() ? null : request.getDeductDescription().trim());
        }

        // Only update active status if provided
        if (request.getIsActive() != null) {
            existingDeduct.setIsActive(request.getIsActive());
        }

        // Always update modified date when any change is made
        existingDeduct.setModifiedDate(LocalDateTime.now());

        DeductsEntity savedDeduct = deductRepository.save(existingDeduct);

        return toDeductResponse(savedDeduct);
    }

    @Override
    @Transactional
    public void deleteDeduct(UUID organisationId, UUID deductId) throws ItemNotFoundException, AccessDeniedException {

        AccountEntity currentUser = getAuthenticatedAccount();

        OrganisationEntity organisation = organisationRepo.findById(organisationId).orElseThrow(() -> new ItemNotFoundException("Organisation not found"));

        OrganisationMember member = validateOrganisationMemberAccess(currentUser, organisation);

        permissionChecker.checkMemberPermission(member, "DEDUCTS","deleteDeducts");

        DeductsEntity existingDeduct = deductRepository.findByDeductIdAndOrganisation_OrganisationId(deductId, organisationId)
                .orElseThrow(() -> new ItemNotFoundException("Deduct not found or does not belong to this organisation"));

        deductRepository.delete(existingDeduct);
    }


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


    private OrganisationMember validateOrganisationMemberAccess(AccountEntity account, OrganisationEntity organisation) throws ItemNotFoundException {
        OrganisationMember member = organisationMemberRepo.findByAccountAndOrganisation(account, organisation)
                .orElseThrow(() -> new ItemNotFoundException("Member is not belong to this organisation"));

        if (member.getStatus() != MemberStatus.ACTIVE) {
            throw new ItemNotFoundException("Member is not active");
        }

        return member;
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
