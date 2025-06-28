package com.qbitspark.buildwisebackend.vendormng_service.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.qbitspark.buildwisebackend.organisation_service.organisation_mng.entity.OrganisationEntity;
import com.qbitspark.buildwisebackend.projectmng_service.entity.ProjectEntity;
import com.qbitspark.buildwisebackend.vendormng_service.enums.VendorStatus;
import com.qbitspark.buildwisebackend.vendormng_service.enums.VendorType;
import com.qbitspark.buildwisebackend.vendormng_service.payloads.BankDetails;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
@Entity
@Table(name = "vendors_tb")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class VendorEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID vendorId;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "address", columnDefinition = "TEXT")
    private String address;

    @Column(name = "office_phone", length = 20)
    private String officePhone;

    @Column(name = "tin", length = 50)
    private String tin;

    @Column(name = "email", length = 100)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(name = "vendor_type", nullable = false)
    private VendorType vendorType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private VendorStatus status = VendorStatus.ACTIVE;

    @Embedded
    private BankDetails bankDetails;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organisation_id", nullable = false)
    private OrganisationEntity organisation;


    @ElementCollection
    @CollectionTable(name = "vendor_attachments_tb", joinColumns = @JoinColumn(name = "vendor_id"))
    @Column(name = "attachment_id")
    private List<UUID> attachmentIds = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Helper methods
    public boolean isActive() {
        return this.status == VendorStatus.ACTIVE;
    }

}