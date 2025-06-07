package com.qbitspark.buildwisebackend.clientsmng_service.entity;
import com.qbitspark.buildwisebackend.projectmngService.entity.ProjectEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "clients",
        indexes = {
                @Index(name = "idx_client_name", columnList = "name"),
                @Index(name = "idx_client_tin", columnList = "tin"),
                @Index(name = "idx_client_email", columnList = "email"),
                @Index(name = "idx_client_created_at", columnList = "createdAt"),
                @Index(name = "idx_client_status", columnList = "is_active")
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

    @Column(name = "tin", length = 50, unique = true)
    private String tin;

    @Column(name = "email", length = 100, unique = true)
    private String email;

    @Column(name = "is_active")
    private Boolean isActive = true;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Relationship with projects
    @OneToMany(mappedBy = "client", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<ProjectEntity> projects;

    // Helper method to get project count
    public int getProjectsCount() {
        return this.projects != null ? this.projects.size() : 0;
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (updatedAt == null) {
            updatedAt = LocalDateTime.now();
        }
    }

    protected void onUpdate() {
        if (updatedAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (updatedAt == null) {
            updatedAt = LocalDateTime.now();
        }
    }

    // Helper method to add project (maintains bidirectional relationship)
    public void addProject(ProjectEntity project) {
        if (project != null) {
            this.projects.add(project);
            project.setClient(this);
        }
    }

    // Helper method to remove project (maintains bidirectional relationship)
    public void removeProject(ProjectEntity project) {
        if (project != null) {
            this.projects.remove(project);
            project.setClient(null);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ClientEntity)) return false;
        ClientEntity that = (ClientEntity) o;
        return clientId != null && clientId.equals(that.clientId);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}