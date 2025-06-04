package com.qbitspark.buildwisebackend.accounting_service.service.impl;

import com.qbitspark.buildwisebackend.accounting_service.entity.ChartOfAccounts;
import com.qbitspark.buildwisebackend.accounting_service.payload.ChartOfAccountsResponse;
import com.qbitspark.buildwisebackend.accounting_service.payload.GroupedChartOfAccountsResponse;
import com.qbitspark.buildwisebackend.accounting_service.repo.ChartOfAccountsRepo;
import com.qbitspark.buildwisebackend.accounting_service.service.ChartOfAccountService;
import com.qbitspark.buildwisebackend.accounting_service.utils.ChartOfAccountsMapper;
import com.qbitspark.buildwisebackend.accounting_service.utils.DefaultChartOfAccountsUtils;
import com.qbitspark.buildwisebackend.accounting_service.utils.HierarchicalChartOfAccountsMapper;
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

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChartOfAccountServiceIMPL implements ChartOfAccountService {

    private final ChartOfAccountsRepo chartOfAccountsRepository;
    private final OrganisationRepo organisationRepository;
    private final AccountRepo accountRepo;
    private final OrganisationMemberRepo organisationMemberRepo;
    private final ChartOfAccountsMapper chartOfAccountsMapper; // ADD THIS
    private final HierarchicalChartOfAccountsMapper hierarchicalMapper; // ADD THIS


//    @Override
//    @Transactional
//    public void createDefaultChartOfAccounts(OrganisationEntity organisation) throws ItemNotFoundException {
//        List<ChartOfAccounts> defaultAccounts = DefaultChartOfAccountsUtils.createConstructionChart(organisation);
//        chartOfAccountsRepository.saveAll(defaultAccounts);
//    }
//
    @Override
    public List<ChartOfAccountsResponse> getChartOfAccountsByOrganisationId(UUID organisationId) throws ItemNotFoundException {
        AccountEntity account = getAuthenticatedAccount();
        validateUserPermission(account, organisationId, false);

        List<ChartOfAccounts> accounts = chartOfAccountsRepository.findByOrganisation_OrganisationId(organisationId);
        return chartOfAccountsMapper.toResponseList(accounts); // ADD THIS MAPPING
    }

    @Override
    @Transactional
    public void createDefaultChartOfAccounts(OrganisationEntity organisation) throws ItemNotFoundException {
        // PASS 1: Create and save all accounts without parent relationships
        List<ChartOfAccounts> defaultAccounts = DefaultChartOfAccountsUtils.createConstructionChart(organisation);
        List<ChartOfAccounts> savedAccounts = chartOfAccountsRepository.saveAll(defaultAccounts);

        // PASS 2: Update parent relationships using saved IDs
        updateParentRelationships(savedAccounts);
        chartOfAccountsRepository.saveAll(savedAccounts);
    }

    // ADD THIS NEW METHOD for the hierarchical response
    @Override
    @Transactional
    public GroupedChartOfAccountsResponse createDefaultChartOfAccountsAndReturnHierarchical(OrganisationEntity organisation) {

        // PASS 1: Create and save all accounts without parent relationships
        List<ChartOfAccounts> defaultAccounts = DefaultChartOfAccountsUtils.createConstructionChart(organisation);
        List<ChartOfAccounts> savedAccounts = chartOfAccountsRepository.saveAll(defaultAccounts);

        // PASS 2: Update parent relationships using saved IDs
        updateParentRelationships(savedAccounts);
        List<ChartOfAccounts> updatedAccounts = chartOfAccountsRepository.saveAll(savedAccounts);

        // Convert to hierarchical structure and return
        return hierarchicalMapper.toGroupedHierarchicalResponse(updatedAccounts);
    }



    // ADD THIS HELPER METHOD
    private void updateParentRelationships(List<ChartOfAccounts> accounts) {
        // Create a map of account code -> account for easy lookup
        Map<String, ChartOfAccounts> accountMap = accounts.stream()
                .collect(Collectors.toMap(ChartOfAccounts::getAccountCode, account -> account));

        // Define parent-child relationships
        setParentRelationship(accountMap, "1000", Arrays.asList("1010", "1020", "1100", "1110", "1200", "1250", "1300"));
        setParentRelationship(accountMap, "1500", Arrays.asList("1510", "1520", "1530", "1600", "1700", "1800"));
        setParentRelationship(accountMap, "2000", Arrays.asList("2010", "2020", "2100", "2200", "2300", "2400"));
        setParentRelationship(accountMap, "2500", Arrays.asList("2510", "2600"));
        setParentRelationship(accountMap, "5000", Arrays.asList("5010", "5100", "5200", "5300", "5400"));
        setParentRelationship(accountMap, "5500", Arrays.asList("5510", "5600", "5700", "5800", "5900"));
    }

    // ADD THIS HELPER METHOD
    private void setParentRelationship(Map<String, ChartOfAccounts> accountMap, String parentCode, List<String> childCodes) {
        ChartOfAccounts parent = accountMap.get(parentCode);
        if (parent != null) {
            for (String childCode : childCodes) {
                ChartOfAccounts child = accountMap.get(childCode);
                if (child != null) {
                    child.setParentAccountId(parent.getId());
                }
            }
        }
    }


    @Override
    public GroupedChartOfAccountsResponse getGroupedHierarchicalChartOfAccounts(UUID organisationId)
            throws ItemNotFoundException {

        AccountEntity account = getAuthenticatedAccount();
        validateUserPermission(account, organisationId, false);

        List<ChartOfAccounts> allAccounts = chartOfAccountsRepository
                .findByOrganisation_OrganisationId(organisationId);

        if (allAccounts.isEmpty()) {
            throw new ItemNotFoundException("No accounts found for organisation");
        }

        return hierarchicalMapper.toGroupedHierarchicalResponse(allAccounts);
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