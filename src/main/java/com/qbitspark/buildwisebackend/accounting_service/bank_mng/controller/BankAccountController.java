package com.qbitspark.buildwisebackend.accounting_service.bank_mng.controller;

import com.qbitspark.buildwisebackend.accounting_service.bank_mng.entity.BankAccountEntity;
import com.qbitspark.buildwisebackend.accounting_service.bank_mng.payload.BankAccountResponse;
import com.qbitspark.buildwisebackend.accounting_service.bank_mng.payload.CreateBankAccountRequest;
import com.qbitspark.buildwisebackend.accounting_service.bank_mng.payload.UpdateBankAccountRequest;
import com.qbitspark.buildwisebackend.accounting_service.bank_mng.service.BankAccountService;
import com.qbitspark.buildwisebackend.globeadvice.exceptions.AccessDeniedException;
import com.qbitspark.buildwisebackend.globeadvice.exceptions.ItemNotFoundException;
import com.qbitspark.buildwisebackend.globeresponsebody.GlobeSuccessResponseBuilder;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/accounting/{organisationId}/bank-accounts")
@RequiredArgsConstructor
public class BankAccountController {

    private final BankAccountService bankAccountService;

    @PostMapping
    public ResponseEntity<GlobeSuccessResponseBuilder> createBankAccount(
            @PathVariable UUID organisationId,
            @Valid @RequestBody CreateBankAccountRequest request)
            throws ItemNotFoundException, AccessDeniedException {

        BankAccountEntity bankAccount = bankAccountService.createBankAccount(organisationId, request);
        BankAccountResponse response = mapToResponse(bankAccount);

        return ResponseEntity.ok(
                GlobeSuccessResponseBuilder.success("Bank account created successfully", response)
        );
    }

    @GetMapping
    public ResponseEntity<GlobeSuccessResponseBuilder> getOrganisationBankAccounts(
            @PathVariable UUID organisationId)
            throws ItemNotFoundException, AccessDeniedException {

        List<BankAccountEntity> bankAccounts = bankAccountService.getOrganisationBankAccounts(organisationId);
        List<BankAccountResponse> responses = bankAccounts.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(
                GlobeSuccessResponseBuilder.success("Bank accounts retrieved successfully", responses)
        );
    }

    @GetMapping("/{bankAccountId}")
    public ResponseEntity<GlobeSuccessResponseBuilder> getBankAccountById(
            @PathVariable UUID organisationId,
            @PathVariable UUID bankAccountId)
            throws ItemNotFoundException, AccessDeniedException {

        BankAccountEntity bankAccount = bankAccountService.getBankAccountById(organisationId, bankAccountId);
        BankAccountResponse response = mapToResponse(bankAccount);

        return ResponseEntity.ok(
                GlobeSuccessResponseBuilder.success("Bank account retrieved successfully", response)
        );
    }

    @PutMapping("/{bankAccountId}")
    public ResponseEntity<GlobeSuccessResponseBuilder> updateBankAccount(
            @PathVariable UUID organisationId,
            @PathVariable UUID bankAccountId,
            @Valid @RequestBody UpdateBankAccountRequest request)
            throws ItemNotFoundException, AccessDeniedException {

        BankAccountEntity bankAccount = bankAccountService.updateBankAccount(organisationId, bankAccountId, request);
        BankAccountResponse response = mapToResponse(bankAccount);

        return ResponseEntity.ok(
                GlobeSuccessResponseBuilder.success("Bank account updated successfully", response)
        );
    }

    @PutMapping("/{bankAccountId}/set-default")
    public ResponseEntity<GlobeSuccessResponseBuilder> setDefaultBankAccount(
            @PathVariable UUID organisationId,
            @PathVariable UUID bankAccountId)
            throws ItemNotFoundException, AccessDeniedException {

        bankAccountService.setDefaultBankAccount(organisationId, bankAccountId);

        return ResponseEntity.ok(
                GlobeSuccessResponseBuilder.success("Default bank account set successfully")
        );
    }

    @PutMapping("/{bankAccountId}/deactivate")
    public ResponseEntity<GlobeSuccessResponseBuilder> deactivateBankAccount(
            @PathVariable UUID organisationId,
            @PathVariable UUID bankAccountId)
            throws ItemNotFoundException, AccessDeniedException {

        bankAccountService.deactivateBankAccount(organisationId, bankAccountId);

        return ResponseEntity.ok(
                GlobeSuccessResponseBuilder.success("Bank account deactivated successfully")
        );
    }

    private BankAccountResponse mapToResponse(BankAccountEntity entity) {
        BankAccountResponse response = new BankAccountResponse();
        response.setBankAccountId(entity.getBankAccountId());
        response.setAccountName(entity.getAccountName());
        response.setAccountNumber(entity.getAccountNumber());
        response.setBankName(entity.getBankName());
        response.setBranchName(entity.getBranchName());
        response.setSwiftCode(entity.getSwiftCode());
        response.setRoutingNumber(entity.getRoutingNumber());
        response.setAccountType(entity.getAccountType());
        response.setCurrentBalance(entity.getCurrentBalance());
        response.setIsActive(entity.getIsActive());
        response.setIsDefault(entity.getIsDefault());
        response.setDescription(entity.getDescription());
        response.setCreatedAt(entity.getCreatedAt());
        response.setUpdatedAt(entity.getUpdatedAt());
        return response;
    }
}