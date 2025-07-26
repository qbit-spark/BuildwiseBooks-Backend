package com.qbitspark.buildwisebackend.accounting_service.deducts_mng.entity;


import com.qbitspark.buildwisebackend.organisation_service.organisation_mng.entity.OrganisationEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "deducts_tb")
public class DeductsEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID deductId;
    private String deductName;
    private BigDecimal deductPercent;
    @Column(name = "deduct_description", columnDefinition = "TEXT")
    private String deductDescription;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organisation_id")
    private OrganisationEntity organisation;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive;

    private LocalDateTime createdDate;
    private LocalDateTime modifiedDate;
    private String createdBy;
}
