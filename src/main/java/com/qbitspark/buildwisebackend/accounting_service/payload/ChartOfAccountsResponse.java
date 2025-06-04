package com.qbitspark.buildwisebackend.accounting_service.payload;

import com.qbitspark.buildwisebackend.accounting_service.enums.AccountType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChartOfAccountsResponse {

    private UUID id;
    private String accountCode;
    private String name;
    private String description;
    private AccountType accountType;
    private boolean isActive;
    private LocalDateTime createdDate;
    private LocalDateTime modifiedDate;
    private String createdBy;

    // Organisation info - just basic details to avoid recursion
    private UUID organisationId;
    private String organisationName;
}