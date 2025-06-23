package com.qbitspark.buildwisebackend.accounting_service.tax_mng.entity;

import com.qbitspark.buildwisebackend.organisation_service.organisation_mng.entity.OrganisationEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "taxes_tb")
public class TaxEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID taxId;
    private String taxName;
    private double taxPercent;
    @Column(name = "tax_description", columnDefinition = "TEXT")
    private String taxDescription;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organisation_id")
    private OrganisationEntity organisation;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive;

    private LocalDateTime createdDate;
    private LocalDateTime modifiedDate;
    private String createdBy;
}