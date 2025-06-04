package com.qbitspark.buildwisebackend.accounting_service.service.impl;

import com.qbitspark.buildwisebackend.accounting_service.entity.ChartOfAccounts;
import com.qbitspark.buildwisebackend.accounting_service.payload.ChartOfAccountsResponse;
import com.qbitspark.buildwisebackend.accounting_service.repo.ChartOfAccountsRepo;
import com.qbitspark.buildwisebackend.accounting_service.service.ChartOfAccountService;
import com.qbitspark.buildwisebackend.accounting_service.utils.ChartOfAccountsMapper;
import com.qbitspark.buildwisebackend.accounting_service.utils.DefaultChartOfAccountsUtils;
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

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ChartOfAccountServiceIMPL implements ChartOfAccountService {

    private final ChartOfAccountsRepo chartOfAccountsRepository;
    private final OrganisationRepo organisationRepository;
    private final AccountRepo accountRepo;
    private final OrganisationMemberRepo organisationMemberRepo;
    private final ChartOfAccountsMapper chartOfAccountsMapper; // ADD THIS


    @Override
    @Transactional
    public void createDefaultChartOfAccounts(OrganisationEntity organisation) throws ItemNotFoundException {
        List<ChartOfAccounts> defaultAccounts = DefaultChartOfAccountsUtils.createConstructionChart(organisation);
        chartOfAccountsRepository.saveAll(defaultAccounts);
    }

    @Override
    public List<ChartOfAccountsResponse> getChartOfAccountsByOrganisationId(UUID organisationId) throws ItemNotFoundException {
        AccountEntity account = getAuthenticatedAccount();
        validateUserPermission(account, organisationId, false);

        List<ChartOfAccounts> accounts = chartOfAccountsRepository.findByOrganisation_OrganisationId(organisationId);
        return chartOfAccountsMapper.toResponseList(accounts); // ADD THIS MAPPING
    }

    /**
     * Validates if the authenticated user has permission to access the organisation
     * @param account The authenticated user account
     * @param organisationId The organisation ID
     * @param adminOnly If true, only OWNER/ADMIN roles are allowed. If false, any active member is allowed.
     * @throws AccessDeniedException if user doesn't have permission
     * @throws ItemNotFoundException if organisation is not found
     */
    private void validateUserPermission(AccountEntity account, UUID organisationId, boolean adminOnly)
            throws ItemNotFoundException, AccessDeniedException {

        // Find the organisation
        OrganisationEntity organisation = organisationRepository.findById(organisationId)
                .orElseThrow(() -> new ItemNotFoundException("Organisation not found"));

        // First check if user is the owner of the organisation
        if (organisation.getOwner().equals(account)) {
            return; // Owner always has access
        }

        // Check if user is a member of the organisation
        Optional<OrganisationMember> memberOptional = organisationMemberRepo.findByAccountAndOrganisation(account, organisation);

        if (memberOptional.isEmpty()) {
            throw new AccessDeniedException("User is not a member of this organisation");
        }

        OrganisationMember member = memberOptional.get();

        // Check if member is active
        if (member.getStatus() != MemberStatus.ACTIVE) {
            throw new AccessDeniedException("User membership is not active");
        }

        // If admin-only operation, check role
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