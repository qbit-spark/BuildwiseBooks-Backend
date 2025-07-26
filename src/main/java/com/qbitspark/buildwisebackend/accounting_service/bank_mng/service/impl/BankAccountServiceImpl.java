package com.qbitspark.buildwisebackend.accounting_service.bank_mng.service.impl;

import com.qbitspark.buildwisebackend.accounting_service.bank_mng.entity.BankAccountEntity;
import com.qbitspark.buildwisebackend.accounting_service.bank_mng.payload.CreateBankAccountRequest;
import com.qbitspark.buildwisebackend.accounting_service.bank_mng.payload.UpdateBankAccountRequest;
import com.qbitspark.buildwisebackend.accounting_service.bank_mng.repo.BankAccountRepo;
import com.qbitspark.buildwisebackend.accounting_service.bank_mng.service.BankAccountService;
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

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class BankAccountServiceImpl implements BankAccountService {

    private final BankAccountRepo bankAccountRepo;
    private final OrganisationRepo organisationRepo;
    private final OrganisationMemberRepo organisationMemberRepo;
    private final AccountRepo accountRepo;
    private final PermissionCheckerService permissionChecker;

    @Override
    public BankAccountEntity createBankAccount(UUID organisationId, CreateBankAccountRequest request)
            throws ItemNotFoundException, AccessDeniedException {

        AccountEntity currentUser = getAuthenticatedAccount();

        OrganisationEntity organisation = organisationRepo.findById(organisationId).orElseThrow(() -> new ItemNotFoundException("Organisation not found"));

        OrganisationMember member = validateOrganisationMemberAccess(currentUser, organisation);

        permissionChecker.checkMemberPermission(member, "BANK_ACCOUNTS","createBankAccounts");

        if (bankAccountRepo.existsByAccountNumberAndOrganisation(request.getAccountNumber(), organisation)) {
            throw new ItemNotFoundException("Account number already exists in this organisation");
        }

        BankAccountEntity bankAccount = new BankAccountEntity();
        bankAccount.setOrganisation(organisation);
        bankAccount.setAccountName(request.getAccountName());
        bankAccount.setAccountNumber(request.getAccountNumber());
        bankAccount.setBankName(request.getBankName());
        bankAccount.setBranchName(request.getBranchName());
        bankAccount.setSwiftCode(request.getSwiftCode());
        bankAccount.setRoutingNumber(request.getRoutingNumber());
        bankAccount.setAccountType(request.getAccountType());
        bankAccount.setCurrentBalance(request.getCurrentBalance() != null ? request.getCurrentBalance() : BigDecimal.ZERO);
        bankAccount.setDescription(request.getDescription());
        bankAccount.setCreatedBy(currentUser.getAccountId());

        if (Boolean.TRUE.equals(request.getIsDefault())) {
            clearExistingDefaultAccount(organisation);
            bankAccount.setIsDefault(true);
        }

        return bankAccountRepo.save(bankAccount);
    }

    @Override
    public List<BankAccountEntity> getOrganisationBankAccounts(UUID organisationId)
            throws ItemNotFoundException, AccessDeniedException {

        AccountEntity currentUser = getAuthenticatedAccount();

        OrganisationEntity organisation = organisationRepo.findById(organisationId).orElseThrow(() -> new ItemNotFoundException("Organisation not found"));

        OrganisationMember member = validateOrganisationMemberAccess(currentUser, organisation);

        permissionChecker.checkMemberPermission(member, "BANK_ACCOUNTS","viewBankAccounts");

        return bankAccountRepo.findByOrganisationAndIsActiveTrue(organisation);
    }

    @Override
    public BankAccountEntity getBankAccountById(UUID organisationId, UUID bankAccountId)
            throws ItemNotFoundException, AccessDeniedException {

        AccountEntity currentUser = getAuthenticatedAccount();

        OrganisationEntity organisation = organisationRepo.findById(organisationId).orElseThrow(() -> new ItemNotFoundException("Organisation not found"));

        OrganisationMember member = validateOrganisationMemberAccess(currentUser, organisation);

        permissionChecker.checkMemberPermission(member, "BANK_ACCOUNTS","viewBankAccounts");

        return bankAccountRepo.findByBankAccountIdAndOrganisation(bankAccountId, organisation)
                .orElseThrow(() -> new ItemNotFoundException("Bank account not found"));
    }

    @Override
    public BankAccountEntity updateBankAccount(UUID organisationId, UUID bankAccountId, UpdateBankAccountRequest request)
            throws ItemNotFoundException, AccessDeniedException {

        AccountEntity currentUser = getAuthenticatedAccount();

        OrganisationEntity organisation = organisationRepo.findById(organisationId).orElseThrow(() -> new ItemNotFoundException("Organisation not found"));

        OrganisationMember member = validateOrganisationMemberAccess(currentUser, organisation);

        permissionChecker.checkMemberPermission(member, "BANK_ACCOUNTS","updateBankAccounts");

        BankAccountEntity bankAccount = bankAccountRepo.findByBankAccountIdAndOrganisation(bankAccountId, organisation)
                .orElseThrow(() -> new ItemNotFoundException("Bank account not found"));

        if (request.getAccountName() != null) {
            bankAccount.setAccountName(request.getAccountName());
        }
        if (request.getBankName() != null) {
            bankAccount.setBankName(request.getBankName());
        }
        if (request.getBranchName() != null) {
            bankAccount.setBranchName(request.getBranchName());
        }
        if (request.getSwiftCode() != null) {
            bankAccount.setSwiftCode(request.getSwiftCode());
        }
        if (request.getRoutingNumber() != null) {
            bankAccount.setRoutingNumber(request.getRoutingNumber());
        }
        if (request.getAccountType() != null) {
            bankAccount.setAccountType(request.getAccountType());
        }
        if (request.getCurrentBalance() != null) {
            bankAccount.setCurrentBalance(request.getCurrentBalance());
        }
        if (request.getDescription() != null) {
            bankAccount.setDescription(request.getDescription());
        }

        if (Boolean.TRUE.equals(request.getIsDefault())) {
            clearExistingDefaultAccount(organisation);
            bankAccount.setIsDefault(true);
        }

        bankAccount.setUpdatedBy(currentUser.getAccountId());

        return bankAccountRepo.save(bankAccount);
    }

    @Override
    public void setDefaultBankAccount(UUID organisationId, UUID bankAccountId)
            throws ItemNotFoundException, AccessDeniedException {

        AccountEntity currentUser = getAuthenticatedAccount();

        OrganisationEntity organisation = organisationRepo.findById(organisationId).orElseThrow(() -> new ItemNotFoundException("Organisation not found"));

        OrganisationMember member = validateOrganisationMemberAccess(currentUser, organisation);

        permissionChecker.checkMemberPermission(member, "BANK_ACCOUNTS","setDefaultBankAccounts");

        BankAccountEntity bankAccount = bankAccountRepo.findByBankAccountIdAndOrganisation(bankAccountId, organisation)
                .orElseThrow(() -> new ItemNotFoundException("Bank account not found"));

        clearExistingDefaultAccount(organisation);

        bankAccount.setIsDefault(true);
        bankAccount.setUpdatedBy(currentUser.getAccountId());
        bankAccountRepo.save(bankAccount);
    }

    @Override
    public void deactivateBankAccount(UUID organisationId, UUID bankAccountId)
            throws ItemNotFoundException, AccessDeniedException {

        AccountEntity currentUser = getAuthenticatedAccount();

        OrganisationEntity organisation = organisationRepo.findById(organisationId).orElseThrow(() -> new ItemNotFoundException("Organisation not found"));

        OrganisationMember member = validateOrganisationMemberAccess(currentUser, organisation);

        permissionChecker.checkMemberPermission(member, "BANK_ACCOUNTS","deactivateBankAccounts");

        BankAccountEntity bankAccount = bankAccountRepo.findByBankAccountIdAndOrganisation(bankAccountId, organisation)
                .orElseThrow(() -> new ItemNotFoundException("Bank account not found"));

        bankAccount.setIsActive(false);
        bankAccount.setIsDefault(false);
        bankAccount.setUpdatedBy(currentUser.getAccountId());
        bankAccountRepo.save(bankAccount);
    }


    private void clearExistingDefaultAccount(OrganisationEntity organisation) {
        bankAccountRepo.findByOrganisationAndIsDefaultTrue(organisation)
                .ifPresent(existingDefault -> {
                    existingDefault.setIsDefault(false);
                    bankAccountRepo.save(existingDefault);
                });
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
        if (authentication != null && authentication.isAuthenticated()) {
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            String userName = userDetails.getUsername();
            return accountRepo.findByUserName(userName)
                    .orElseThrow(() -> new ItemNotFoundException("User not found"));
        }
        throw new ItemNotFoundException("User not authenticated");
    }

}