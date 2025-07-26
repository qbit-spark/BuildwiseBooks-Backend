package com.qbitspark.buildwisebackend.accounting_service.bank_mng.payload;

import com.qbitspark.buildwisebackend.accounting_service.bank_mng.enums.AccountType;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class UpdateBankAccountRequest {

    private String accountName;
    private String bankName;
    private String branchName;
    private String swiftCode;
    private String routingNumber;
    private AccountType accountType;
    private BigDecimal currentBalance;
    private Boolean isDefault;
    private String description;
}
