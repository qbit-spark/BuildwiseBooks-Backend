package com.qbitspark.buildwisebackend.accounting_service.coa.utils;

import com.qbitspark.buildwisebackend.accounting_service.coa.entity.ChartOfAccounts;
import com.qbitspark.buildwisebackend.accounting_service.coa.payload.ChartOfAccountsResponse;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class ChartOfAccountsMapper {

    /**
     * Convert ChartOfAccounts entity to Response
     */
    public ChartOfAccountsResponse toResponse(ChartOfAccounts entity) {
        if (entity == null) {
            return null;
        }

        return ChartOfAccountsResponse.builder()
                .id(entity.getId())
                .accountCode(entity.getAccountCode())
                .name(entity.getName())
                .description(entity.getDescription())
                .accountType(entity.getAccountType())
                .isActive(entity.getIsActive())
                .createdDate(entity.getCreatedDate())
                .modifiedDate(entity.getModifiedDate())
                .createdBy(entity.getCreatedBy())
                .organisationId(entity.getOrganisation() != null ? entity.getOrganisation().getOrganisationId() : null)
                .organisationName(entity.getOrganisation() != null ? entity.getOrganisation().getOrganisationName() : null)
                // ADD THESE 3 LINES:
                .parentAccountId(entity.getParentAccountId())
                .isHeader(entity.getIsHeader())
                .isPostable(entity.getIsPostable())
                .build();
    }


    /**
     * Convert list of ChartOfAccounts entities to Response list
     */
    public List<ChartOfAccountsResponse> toResponseList(List<ChartOfAccounts> entities) {
        if (entities == null) {
            return null;
        }

        return entities.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

}