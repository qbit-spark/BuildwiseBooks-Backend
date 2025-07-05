package com.qbitspark.buildwisebackend.accounting_service.bank_mng.service;

import com.qbitspark.buildwisebackend.accounting_service.bank_mng.entity.BankAccountEntity;
import com.qbitspark.buildwisebackend.accounting_service.bank_mng.payload.CreateBankAccountRequest;
import com.qbitspark.buildwisebackend.accounting_service.bank_mng.payload.UpdateBankAccountRequest;
import com.qbitspark.buildwisebackend.globeadvice.exceptions.AccessDeniedException;
import com.qbitspark.buildwisebackend.globeadvice.exceptions.ItemNotFoundException;

import java.util.List;
import java.util.UUID;

public interface BankAccountService {

    BankAccountEntity createBankAccount(UUID organisationId, CreateBankAccountRequest request)
            throws ItemNotFoundException, AccessDeniedException;

    List<BankAccountEntity> getOrganisationBankAccounts(UUID organisationId)
            throws ItemNotFoundException, AccessDeniedException;

    BankAccountEntity getBankAccountById(UUID organisationId, UUID bankAccountId)
            throws ItemNotFoundException, AccessDeniedException;

    BankAccountEntity updateBankAccount(UUID organisationId, UUID bankAccountId, UpdateBankAccountRequest request)
            throws ItemNotFoundException, AccessDeniedException;

    void setDefaultBankAccount(UUID organisationId, UUID bankAccountId)
            throws ItemNotFoundException, AccessDeniedException;

    void deactivateBankAccount(UUID organisationId, UUID bankAccountId)
            throws ItemNotFoundException, AccessDeniedException;
}