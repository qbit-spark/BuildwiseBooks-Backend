package com.qbitspark.buildwisebackend.accounting_service.coa.payload;

import com.qbitspark.buildwisebackend.accounting_service.coa.enums.AccountType;
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

    // Organisation info
    private UUID organisationId;
    private String organisationName;

    // NEW HIERARCHY FIELDS
    private UUID parentAccountId;
    private boolean isHeader;
    private boolean isPostable;
}