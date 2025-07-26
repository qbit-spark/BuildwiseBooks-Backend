package com.qbitspark.buildwisebackend.accounting_service.bank_mng.payload;

import com.qbitspark.buildwisebackend.accounting_service.bank_mng.enums.AccountType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CreateBankAccountRequest {

    @NotBlank(message = "Account name is required")
    private String accountName;

    @NotBlank(message = "Account number is required")
    private String accountNumber;

    @NotBlank(message = "Bank name is required")
    private String bankName;

    private String branchName;
    private String swiftCode;
    private String routingNumber;

    @NotNull(message = "Account type is required")
    private AccountType accountType;

    private BigDecimal currentBalance;
    private Boolean isDefault = false;
    private String description;
}

