package com.qbitspark.buildwisebackend.accounting_service.bank_mng.payload;

import com.qbitspark.buildwisebackend.accounting_service.bank_mng.enums.AccountType;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class BankAccountResponse {

    private UUID bankAccountId;
    private String accountName;
    private String accountNumber;
    private String bankName;
    private String branchName;
    private String swiftCode;
    private String routingNumber;
    private AccountType accountType;
    private BigDecimal currentBalance;
    private Boolean isActive;
    private Boolean isDefault;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String maskedAccountNumber;

    public String getMaskedAccountNumber() {
        if (accountNumber == null || accountNumber.length() < 4) {
            return "****";
        }
        return "****" + accountNumber.substring(accountNumber.length() - 4);
    }
}
