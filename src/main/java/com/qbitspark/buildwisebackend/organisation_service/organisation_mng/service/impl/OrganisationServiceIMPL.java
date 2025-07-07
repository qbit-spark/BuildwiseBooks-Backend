package com.qbitspark.buildwisebackend.organisation_service.organisation_mng.service.impl;

import com.qbitspark.buildwisebackend.accounting_service.coa.service.ChartOfAccountService;
import com.qbitspark.buildwisebackend.drive_mng.service.OrgDriveService;
import com.qbitspark.buildwisebackend.globeadvice.exceptions.AccessDeniedException;
import com.qbitspark.buildwisebackend.globeadvice.exceptions.ItemNotFoundException;
import com.qbitspark.buildwisebackend.authentication_service.Repository.AccountRepo;
import com.qbitspark.buildwisebackend.authentication_service.entity.AccountEntity;
import com.qbitspark.buildwisebackend.organisation_service.organisation_mng.entity.OrganisationEntity;
import com.qbitspark.buildwisebackend.organisation_service.organisation_mng.payloads.CreateOrganisationRequest;
import com.qbitspark.buildwisebackend.organisation_service.organisation_mng.payloads.UpdateOrganisationRequest;
import com.qbitspark.buildwisebackend.organisation_service.organisation_mng.repo.OrganisationRepo;
import com.qbitspark.buildwisebackend.organisation_service.organisation_mng.service.OrganisationService;
import com.qbitspark.buildwisebackend.organisation_service.orgnisation_members_mng.service.OrganisationMemberService;
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

@Service
@RequiredArgsConstructor
public class OrganisationServiceIMPL implements OrganisationService {

    private final AccountRepo accountRepo;
    private final OrganisationRepo organisationRepo;
    private final OrganisationMemberService organisationMemberService;
    private final ChartOfAccountService chartOfAccountService;
    private final OrgDriveService orgDriveService;

    @Transactional
    @Override
    public OrganisationEntity createOrganisation(CreateOrganisationRequest createOrganisationRequest) throws ItemNotFoundException, AccessDeniedException {

        AccountEntity authenticatedAccount = getAuthenticatedAccount();

        if (organisationRepo.existsByOrganisationNameAndOwner(
                createOrganisationRequest.getName(),
                authenticatedAccount)) {
            throw new ItemNotFoundException("Organisation with this name already exists for the authenticated account");
        }

        OrganisationEntity organisationEntity = new OrganisationEntity();
        organisationEntity.setOrganisationName(createOrganisationRequest.getName());
        organisationEntity.setOrganisationDescription(createOrganisationRequest.getDescription());
        organisationEntity.setOwner(authenticatedAccount);
        organisationEntity.setCreatedDate(LocalDateTime.now());
        organisationEntity.setModifiedDate(LocalDateTime.now());
        organisationEntity.setActive(true);

        OrganisationEntity savedOrganisation = organisationRepo.save(organisationEntity);

        organisationMemberService.addOwnerAsMember(savedOrganisation, authenticatedAccount);

        chartOfAccountService.createDefaultChartOfAccountsAndReturnHierarchical(savedOrganisation);

        orgDriveService.initializeOrganisationDrive(savedOrganisation);

        return savedOrganisation;

    }

    @Override
    public OrganisationEntity getOrganisationById(UUID id) throws ItemNotFoundException {
        return organisationRepo.findById(id)
                .orElseThrow(() -> new ItemNotFoundException("Organisation with the given ID does not exist"));
    }

    @Override
    public OrganisationEntity getMyOrganisationById(UUID id) throws ItemNotFoundException {

        AccountEntity authenticatedAccount = getAuthenticatedAccount();

        return organisationRepo.findByOrganisationIdAndOwner(id, authenticatedAccount)
                .orElseThrow(() -> new ItemNotFoundException("Organisation with the given ID does not exist or you are not the owner"));

    }

    @Override
    public List<OrganisationEntity> getAllOrganisations() {
        return organisationRepo.findAll();
    }

    @Override
    public List<OrganisationEntity> getAllMyOrganisations() throws ItemNotFoundException {
        return organisationRepo.findAllByOwner(getAuthenticatedAccount());
    }

    @Transactional
    @Override
    public OrganisationEntity updateOrganisation(UUID id, UpdateOrganisationRequest updateOrganisationRequest) throws ItemNotFoundException {

        OrganisationEntity existingOrganisation = organisationRepo.findByOrganisationIdAndOwner(id, getAuthenticatedAccount())
                .orElseThrow(() -> new ItemNotFoundException("This organisation does not exist or you are not the owner"));

        if (updateOrganisationRequest.getName() != null && !updateOrganisationRequest.getName().trim().isEmpty()) {
            existingOrganisation.setOrganisationName(updateOrganisationRequest.getName().trim());
        }

        if (updateOrganisationRequest.getDescription() != null && !updateOrganisationRequest.getDescription().trim().isEmpty()) {
            existingOrganisation.setOrganisationDescription(updateOrganisationRequest.getDescription().trim());
        }


        return organisationRepo.save(existingOrganisation);
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
