package com.qbitspark.buildwisebackend.subcontractor_service.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.qbitspark.buildwisebackend.projectmng_service.entity.ProjectEntity;
import com.qbitspark.buildwisebackend.organisation_service.organisation_mng.entity.OrganisationEntity;
import com.qbitspark.buildwisebackend.projectmng_service.entity.ProjectSubcontractorEntity;
import com.qbitspark.buildwisebackend.subcontractor_service.enums.SpecializationType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "subcontractor",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"company_name", "organisation_id"}),
                @UniqueConstraint(columnNames = {"email"}),
                @UniqueConstraint(columnNames = {"tin"}),
                @UniqueConstraint(columnNames = {"registration_number"})
        },
        indexes = {
                @Index(name = "idx_subcontractor_organisation", columnList = "organisation_id"),
                @Index(name = "idx_subcontractor_company_name", columnList = "company_name"),
                @Index(name = "idx_subcontractor_email", columnList = "email"),
                @Index(name = "idx_subcontractor_created_at", columnList = "createdAt")
        })
public class SubcontractorEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "subcontractor_id", nullable = false, updatable = false)
    private UUID subcontractorId;

    @Column(name = "company_name", nullable = false)
    private String companyName;

    @Column(name = "email", nullable = false, length = 100)
    private String email;

    @Column(name = "phone_number", nullable = false, length = 20)
    private String phoneNumber;

    @Column(name = "tin", nullable = false, length = 50)
    private String tin;

    @Column(name = "address", columnDefinition = "TEXT")
    private String address;

    @Column(name = "registration_number", length = 50, nullable = false)
    private String registrationNumber;

    @ElementCollection(targetClass = SpecializationType.class)
    @CollectionTable(name = "subcontractor_specializations",
            joinColumns = @JoinColumn(name = "subcontractor_id"))
    @Column(name = "specialization")
    @Enumerated(EnumType.STRING)
    private List<SpecializationType> specializations;

    @OneToMany(mappedBy = "subcontractor", fetch = FetchType.LAZY, cascade = {CascadeType.PERSIST, CascadeType.MERGE}, orphanRemoval = true)
    private Set<ProjectSubcontractorEntity> projectSubcontractors = new HashSet<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organisation_id", nullable = false)
    @JsonIgnore
    private OrganisationEntity organisation;

    @CreationTimestamp
    @Column(name = "createdAt", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updatedAt")
    private LocalDateTime updatedAt;

    public boolean isAssignedToProject(ProjectEntity project) {
        return projectSubcontractors.stream().anyMatch(ps -> ps.getProject().equals(project));
    }

    public int getProjectsCount() {
        return this.projectSubcontractors != null ? this.projectSubcontractors.size() : 0;
    }

    public int getSpecializationsCount() {
        return this.specializations != null ? this.specializations.size() : 0;
    }

    public String getOrganisationName() {
        return this.organisation != null ? this.organisation.getOrganisationName() : null;
    }

    public UUID getOrganisationId() {
        return this.organisation != null ? this.organisation.getOrganisationId() : null;
    }


}