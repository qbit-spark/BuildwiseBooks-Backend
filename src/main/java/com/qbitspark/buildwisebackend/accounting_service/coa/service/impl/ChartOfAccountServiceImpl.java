package com.qbitspark.buildwisebackend.accounting_service.coa.service.impl;

import com.qbitspark.buildwisebackend.accounting_service.coa.entity.ChartOfAccounts;
import com.qbitspark.buildwisebackend.accounting_service.coa.enums.AccountType;
import com.qbitspark.buildwisebackend.accounting_service.coa.payload.AddAccountRequest;
import com.qbitspark.buildwisebackend.accounting_service.coa.payload.ChartOfAccountsResponse;
import com.qbitspark.buildwisebackend.accounting_service.coa.payload.GroupedChartOfAccountsResponse;
import com.qbitspark.buildwisebackend.accounting_service.coa.repo.ChartOfAccountsRepo;
import com.qbitspark.buildwisebackend.accounting_service.coa.service.ChartOfAccountService;
import com.qbitspark.buildwisebackend.accounting_service.coa.utils.AccountCodeGenerator;
import com.qbitspark.buildwisebackend.accounting_service.coa.utils.ChartOfAccountsMapper;
import com.qbitspark.buildwisebackend.accounting_service.coa.utils.DefaultChartOfAccountsUtils;
import com.qbitspark.buildwisebackend.accounting_service.coa.utils.HierarchicalChartOfAccountsMapper;
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
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChartOfAccountServiceImpl implements ChartOfAccountService {

    private final ChartOfAccountsRepo chartOfAccountsRepository;
    private final OrganisationRepo organisationRepo;
    private final AccountRepo accountRepo;
    private final OrganisationMemberRepo organisationMemberRepo;
    private final ChartOfAccountsMapper chartOfAccountsMapper;
    private final HierarchicalChartOfAccountsMapper hierarchicalMapper;
    private final AccountCodeGenerator accountCodeGenerator;
    private final PermissionCheckerService permissionChecker;

    @Override
    @Transactional
    public void createDefaultChartOfAccountsAndReturnHierarchical(OrganisationEntity organisation) {

        // Check if organization already has chart of accounts
        List<ChartOfAccounts> existingAccounts = chartOfAccountsRepository
                .findByOrganisation_OrganisationId(organisation.getOrganisationId());

        if (!existingAccounts.isEmpty()) {
            // Organization already has a chart of accounts - do nothing
            return;
        }

        // Create a default chart of accounts only if none exist
        List<ChartOfAccounts> defaultAccounts = DefaultChartOfAccountsUtils.createConstructionChart(organisation);
        List<ChartOfAccounts> savedAccounts = chartOfAccountsRepository.saveAll(defaultAccounts);
        updateParentRelationships(savedAccounts);
        chartOfAccountsRepository.saveAll(savedAccounts);
    }

    @Override
    @Transactional
    public GroupedChartOfAccountsResponse initializeDefaultChartOfAccounts(UUID organisationId) throws ItemNotFoundException, AccessDeniedException {

        OrganisationEntity organisation = organisationRepo.findById(organisationId).orElseThrow(() -> new ItemNotFoundException("Organisation not found"));

        List<ChartOfAccounts> existingAccounts = chartOfAccountsRepository
                .findByOrganisation_OrganisationId(organisationId);

        if (!existingAccounts.isEmpty()) {
            throw new ItemNotFoundException("Organisation already has chart of accounts. Cannot initialize again.");
        }

        // Create a default chart of accounts
        createDefaultChartOfAccountsAndReturnHierarchical(organisation);

        // Return the created chart of accounts
        return getGroupedHierarchicalChartOfAccounts(organisationId);
    }

    @Override
    public GroupedChartOfAccountsResponse getGroupedHierarchicalChartOfAccounts(UUID organisationId)
            throws ItemNotFoundException, AccessDeniedException {

        AccountEntity currentUser = getAuthenticatedAccount();

        OrganisationEntity organisation = organisationRepo.findById(organisationId).orElseThrow(() -> new ItemNotFoundException("Organisation not found"));

        OrganisationMember member = validateOrganisationMemberAccess(currentUser, organisation);

        permissionChecker.checkMemberPermission(member, "CHART_OF_ACCOUNTS","viewChartOfAccounts");

        // Check if organization has chart of accounts
        List<ChartOfAccounts> allAccounts = chartOfAccountsRepository
                .findByOrganisation_OrganisationId(organisationId);

        if (allAccounts.isEmpty()) {
            throw new ItemNotFoundException("No chart of accounts found for this organisation. Please create default chart of accounts first.");
        }

        return hierarchicalMapper.toGroupedHierarchicalResponse(allAccounts);
    }

    @Override
    @Transactional
    public ChartOfAccountsResponse addNewAccount(UUID organisationId, AddAccountRequest request) throws ItemNotFoundException, AccessDeniedException {

        AccountEntity currentUser = getAuthenticatedAccount();

        OrganisationEntity organisation = organisationRepo.findById(organisationId).orElseThrow(() -> new ItemNotFoundException("Organisation not found"));

        OrganisationMember member = validateOrganisationMemberAccess(currentUser, organisation);

        permissionChecker.checkMemberPermission(member, "CHART_OF_ACCOUNTS","viewChartOfAccounts");

        validateOrganisationHasChartOfAccounts(organisationId);

        validateParentAccount(request, organisationId);

        // Check for duplicate names within the same parent group
        validateNoDuplicateName(request.getName(), organisationId,
                request.getParentAccountId(), request.getAccountType(), null);

        // Generate account code
        String accountCode = accountCodeGenerator.generateNextAccountCode(
                request.getAccountType(),
                organisationId,
                request.getParentAccountId()
        );

        // Create a new account
        ChartOfAccounts newAccount = createAccountFromRequest(request, organisation, accountCode, currentUser.getUserName());

        // Save account
        ChartOfAccounts savedAccount = chartOfAccountsRepository.save(newAccount);

        return chartOfAccountsMapper.toResponse(savedAccount);
    }

    @Override
    @Transactional
    public ChartOfAccountsResponse updateAccount(UUID organisationId, UUID accountId, AddAccountRequest request) throws ItemNotFoundException, AccessDeniedException {

        AccountEntity currentUser = getAuthenticatedAccount();

        OrganisationEntity organisation = organisationRepo.findById(organisationId).orElseThrow(() -> new ItemNotFoundException("Organisation not found"));

        OrganisationMember member = validateOrganisationMemberAccess(currentUser, organisation);

        permissionChecker.checkMemberPermission(member, "CHART_OF_ACCOUNTS","updateAccounts");

        // Get an existing account
        ChartOfAccounts existingAccount = chartOfAccountsRepository.findById(accountId)
                .orElseThrow(() -> new ItemNotFoundException("Account not found"));

        // Validate organisation ownership
        if (!existingAccount.getOrganisation().getOrganisationId().equals(organisationId)) {
            throw new ItemNotFoundException("Account belongs to different organisation");
        }

        // Check for duplicate names (excluding the current account)
        validateNoDuplicateName(request.getName(), organisationId,
                existingAccount.getParentAccountId(), existingAccount.getAccountType(), accountId);

        // Update account
        existingAccount.setName(request.getName());
        existingAccount.setDescription(request.getDescription());
        existingAccount.setModifiedDate(LocalDateTime.now());

        ChartOfAccounts savedAccount = chartOfAccountsRepository.save(existingAccount);
        return chartOfAccountsMapper.toResponse(savedAccount);
    }


    private void validateOrganisationHasChartOfAccounts(UUID organisationId) throws ItemNotFoundException {
        List<ChartOfAccounts> existingAccounts = chartOfAccountsRepository
                .findByOrganisation_OrganisationId(organisationId);

        if (existingAccounts.isEmpty()) {
            throw new ItemNotFoundException("Organisation does not have chart of accounts setup. Please create default chart of accounts first.");
        }
    }

    private void validateParentAccount(AddAccountRequest request, UUID organisationId) throws ItemNotFoundException {
        if (request.getParentAccountId() == null) {
            return;
        }

        ChartOfAccounts parentAccount = chartOfAccountsRepository.findById(request.getParentAccountId())
                .orElseThrow(() -> new ItemNotFoundException("Parent account not found"));

        // Check the same organisation
        if (!parentAccount.getOrganisation().getOrganisationId().equals(organisationId)) {
            throw new ItemNotFoundException("Parent account belongs to different organisation");
        }

        // Check the same account type
        if (!parentAccount.getAccountType().equals(request.getAccountType())) {
            throw new ItemNotFoundException("Parent account must belong to same account type: " + request.getAccountType());
        }

        // Check parent is a header (only headers can have children)
        if (!parentAccount.getIsHeader()) {
            throw new ItemNotFoundException("Parent account must be a header account (isHeader: true)");
        }

        // Prevent creating header under header (max 2 levels: header -> detail)
        if (request.getIsHeader() && parentAccount.getParentAccountId() != null) {
            throw new ItemNotFoundException("Cannot create header account under another header. Maximum 2 levels allowed.");
        }
    }


    private void validateNoDuplicateName(String accountName, UUID organisationId,
                                         UUID parentAccountId, AccountType accountType, UUID excludeAccountId)
            throws ItemNotFoundException {

        List<ChartOfAccounts> allAccounts = chartOfAccountsRepository
                .findByOrganisation_OrganisationId(organisationId);

        // Filter accounts in the same parent group
        List<ChartOfAccounts> sameGroupAccounts = allAccounts.stream()
                .filter(account -> {
                    // Same account type
                    if (!account.getAccountType().equals(accountType)) {
                        return false;
                    }

                    // Same parent (both null or both same UUID)
                    if (parentAccountId == null) {
                        return account.getParentAccountId() == null;
                    } else {
                        return parentAccountId.equals(account.getParentAccountId());
                    }
                })
                .filter(account -> !account.getId().equals(excludeAccountId))
                .toList();

        // Check for duplicate names (case-insensitive)
        boolean nameExists = sameGroupAccounts.stream()
                .anyMatch(account -> account.getName().trim().equalsIgnoreCase(accountName.trim()));

        if (nameExists) {
            String groupDescription = parentAccountId != null ? "parent account group" : accountType.name() + " category";
            throw new ItemNotFoundException("Account name '" + accountName + "' already exists in this " + groupDescription);
        }
    }


    private ChartOfAccounts createAccountFromRequest(AddAccountRequest request, OrganisationEntity organisation,
                                                     String accountCode, String createdBy) {
        ChartOfAccounts newAccount = new ChartOfAccounts();
        newAccount.setOrganisation(organisation);
        newAccount.setName(request.getName());
        newAccount.setDescription(request.getDescription());
        newAccount.setAccountType(request.getAccountType());
        newAccount.setAccountCode(accountCode);
        newAccount.setParentAccountId(request.getParentAccountId());
        newAccount.setIsHeader(request.getIsHeader());
        newAccount.setIsPostable(!request.getIsHeader()); // Headers are not postable, details are postable
        newAccount.setIsActive(true);
        newAccount.setCreatedDate(LocalDateTime.now());
        newAccount.setCreatedBy(createdBy);
        return newAccount;
    }

    /**
     * Update parent relationships for default accounts
     */
    private void updateParentRelationships(List<ChartOfAccounts> accounts) {
        Map<String, ChartOfAccounts> accountMap = accounts.stream()
                .collect(Collectors.toMap(ChartOfAccounts::getAccountCode, account -> account));

        setParentRelationship(accountMap, "1000", Arrays.asList("1010", "1020", "1100", "1110", "1200", "1250", "1300"));
        setParentRelationship(accountMap, "1500", Arrays.asList("1510", "1520", "1530", "1600", "1700", "1800"));
        setParentRelationship(accountMap, "2000", Arrays.asList("2010", "2020", "2100", "2200", "2300", "2400"));
        setParentRelationship(accountMap, "2500", Arrays.asList("2510", "2600"));
        setParentRelationship(accountMap, "5000", Arrays.asList("5010", "5100", "5200", "5300", "5400"));
        setParentRelationship(accountMap, "5500", Arrays.asList("5510", "5600", "5700", "5800", "5900"));
    }

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
