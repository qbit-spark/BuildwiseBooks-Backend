package com.qbitspark.buildwisebackend.accounting_service.entity;

import com.qbitspark.buildwisebackend.accounting_service.enums.AccountType;
import com.qbitspark.buildwisebackend.organisation_service.organisation_mng.entity.OrganisationEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "chart_of_accounts")
public class ChartOfAccounts {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    /**
     * Many-to-One relationship with OrganisationEntity
     * Each organization can have multiple chart of accounts
     * Each chart of an account belongs to one organization
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organisation_id", nullable = false)
    private OrganisationEntity organisation;

    @Column(name = "account_name", nullable = false, columnDefinition = "TEXT")
    private String name;

    @Column(name = "account_description", columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_type", nullable = false)
    private AccountType accountType;

    @Column(name = "account_code", nullable = false)
    private String accountCode;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive;

    @Column(name = "created_date", nullable = false)
    private LocalDateTime createdDate;

    @Column(name = "modified_date")
    private LocalDateTime modifiedDate;

    @Column(name = "created_by", nullable = false)
    private String createdBy;

    @Column(name = "parent_account_id")
    private UUID parentAccountId;

    @Column(name = "is_header", nullable = false)
    private Boolean isHeader = false;

    @Column(name = "is_postable", nullable = false)
    private Boolean isPostable = true;

}