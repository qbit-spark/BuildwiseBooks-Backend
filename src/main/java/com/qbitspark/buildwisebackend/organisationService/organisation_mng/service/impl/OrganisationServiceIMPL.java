package com.qbitspark.buildwisebackend.organisationService.organisation_mng.service.impl;

import com.qbitspark.buildwisebackend.globeadvice.exceptions.ItemNotFoundException;
import com.qbitspark.buildwisebackend.globeauthentication.Repository.AccountRepo;
import com.qbitspark.buildwisebackend.globeauthentication.entity.AccountEntity;
import com.qbitspark.buildwisebackend.organisationService.organisation_mng.entity.OrganisationEntity;
import com.qbitspark.buildwisebackend.organisationService.organisation_mng.payloads.CreateOrganisationRequest;
import com.qbitspark.buildwisebackend.organisationService.organisation_mng.payloads.UpdateOrganisationRequest;
import com.qbitspark.buildwisebackend.organisationService.organisation_mng.repo.OrganisationRepo;
import com.qbitspark.buildwisebackend.organisationService.organisation_mng.service.OrganisationService;
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

    @Transactional
    @Override
    public OrganisationEntity createOrganisation(CreateOrganisationRequest createOrganisationRequest) throws ItemNotFoundException {

        AccountEntity authenticatedAccount = getAuthenticatedAccount();

        //Check if Organisation name already exists for the authenticated account
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


        return organisationRepo.save(organisationEntity);

    }

    @Override
    public OrganisationEntity getOrganisationById(UUID id) throws ItemNotFoundException {
        return organisationRepo.findById(id)
                .orElseThrow(() -> new ItemNotFoundException("Organisation with the given ID does not exist"));
    }

    @Override
    public OrganisationEntity getMyOrganisationById(UUID id) throws ItemNotFoundException {

        AccountEntity authenticatedAccount = getAuthenticatedAccount();

        //Make sure the authenticated account is the owner of the organisation
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
