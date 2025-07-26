package com.qbitspark.buildwisebackend.accounting_service.coa.payload;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HierarchicalAccountResponse {
    private UUID id;
    private String accountCode;
    private String name;
    private String description;
    private boolean isActive;
    private LocalDateTime createdDate;
    private LocalDateTime modifiedDate;
    private String createdBy;
    private UUID parentAccountId;
    private boolean isHeader;
    private boolean isPostable;

    // Children accounts within the same account type
    @Builder.Default
    private List<HierarchicalAccountResponse> children = new ArrayList<>();
}
