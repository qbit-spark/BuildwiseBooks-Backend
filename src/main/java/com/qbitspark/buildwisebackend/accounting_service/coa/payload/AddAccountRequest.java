package com.qbitspark.buildwisebackend.accounting_service.coa.payload;

import com.qbitspark.buildwisebackend.accounting_service.coa.enums.AccountType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AddAccountRequest {

    @NotBlank(message = "Account name is required")
    private String name;

    private String description;

    @NotNull(message = "Account type is required")
    private AccountType accountType;

    private UUID parentAccountId; // Optional - for creating child accounts

    private Boolean isHeader = false; // Default to detail account
}
