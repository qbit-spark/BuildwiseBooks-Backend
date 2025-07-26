package com.qbitspark.buildwisebackend.organisation_service.organisation_mng.entity;
import com.qbitspark.buildwisebackend.authentication_service.entity.AccountEntity;
import com.qbitspark.buildwisebackend.projectmng_service.entity.ProjectEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
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

    private String organisationDescription;

    @ManyToOne
    @JoinColumn(name = "account_id", nullable = false)
    private AccountEntity owner;

    private boolean isActive;

    private boolean isDeleted;

    private LocalDateTime createdDate;

    private LocalDateTime modifiedDate;

    private LocalDateTime deletedDate;

    @OneToMany(mappedBy = "organisation", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<ProjectEntity> projects;

    public void addProject(ProjectEntity project) {
        if (project != null) {
            if (this.projects == null) {
                this.projects = new ArrayList<>();
            }
            if (!this.projects.contains(project)) {
                this.projects.add(project);
                project.setOrganisation(this); // Maintain bidirectional relationship
            }
        }
    }

}
