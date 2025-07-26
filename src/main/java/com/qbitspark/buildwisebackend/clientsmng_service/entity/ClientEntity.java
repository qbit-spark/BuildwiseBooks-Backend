package com.qbitspark.buildwisebackend.clientsmng_service.entity;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.qbitspark.buildwisebackend.organisation_service.organisation_mng.entity.OrganisationEntity;
import com.qbitspark.buildwisebackend.projectmng_service.entity.ProjectEntity;
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
@Table(name = "clients",
        indexes = {
                // Composite indexes for validation queries (most important)
                @Index(name = "idx_client_name_org_active", columnList = "name, organisation_id, is_active"),
                @Index(name = "idx_client_email_org_active", columnList = "email, organisation_id, is_active"),
                @Index(name = "idx_client_tin_org_active", columnList = "tin, organisation_id, is_active"),
                @Index(name = "idx_client_address_org_active", columnList = "address, organisation_id, is_active"),

                // For getAllClientsWithinOrganisation query
                @Index(name = "idx_client_org_active", columnList = "organisation_id, is_active"),

                // For finding by client ID (already covered by a primary key, but explicit)
                @Index(name = "idx_client_id", columnList = "client_id"),

                // For date-based queries and sorting
                @Index(name = "idx_client_created_at", columnList = "createdAt"),
                @Index(name = "idx_client_updated_at", columnList = "updatedAt")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ClientEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "client_id", nullable = false, updatable = false)
    private UUID clientId;

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

    @Column(name = "is_active")
    private Boolean isActive = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organisation_id", nullable = false)
    private OrganisationEntity organisation;

    @JsonIgnore
    @OneToMany(mappedBy = "client", cascade = CascadeType.ALL)
    private List<ProjectEntity> projects = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}