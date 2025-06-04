package com.qbitspark.buildwisebackend.accounting_service.utils;

import com.qbitspark.buildwisebackend.accounting_service.entity.ChartOfAccounts;
import com.qbitspark.buildwisebackend.accounting_service.payload.ChartOfAccountsResponse;
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

    /**
     * Convert ChartOfAccounts entity to DTO
     */
    public ChartOfAccountsResponse toDto(ChartOfAccounts entity) {
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

                .parentAccountId(entity.getParentAccountId())
                .isHeader(entity.getIsHeader())
                .isPostable(entity.getIsPostable())
                .build();
    }

    /**
     * Convert DTO to ChartOfAccounts entity (for create/update operations)
     * Note: Organisation should be set separately in service layer
     */
    public ChartOfAccounts toEntity(ChartOfAccountsResponse response) {
        if (response == null) {
            return null;
        }

        ChartOfAccounts entity = new ChartOfAccounts();
        entity.setId(response.getId());
        entity.setAccountCode(response.getAccountCode());
        entity.setName(response.getName());
        entity.setDescription(response.getDescription());
        entity.setAccountType(response.getAccountType());
        entity.setIsActive(response.isActive());
        entity.setCreatedDate(response.getCreatedDate());
        entity.setModifiedDate(response.getModifiedDate());
        entity.setCreatedBy(response.getCreatedBy());
        // ADD THESE 3 LINES:
        entity.setParentAccountId(response.getParentAccountId());
        entity.setIsHeader(response.isHeader());
        entity.setIsPostable(response.isPostable());

        return entity;
    }
}