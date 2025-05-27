package com.qbitspark.buildwisebackend.organisationService.entity;

import com.qbitspark.buildwisebackend.globeauthentication.entity.AccountEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "organisation_table")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class OrganisationEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID organisationId;

    private String organisationName;

    private String organisationAddress;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private AccountEntity owner;
}
