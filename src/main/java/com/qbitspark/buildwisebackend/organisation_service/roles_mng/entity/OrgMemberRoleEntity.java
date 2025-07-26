package com.qbitspark.buildwisebackend.organisation_service.roles_mng.entity;

import com.qbitspark.buildwisebackend.organisation_service.organisation_mng.entity.OrganisationEntity;
import com.qbitspark.buildwisebackend.organisation_service.roles_mng.utils.PermissionJsonbConverter;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;


import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "org_member_roles")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class OrgMemberRoleEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID roleId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organisation_id", nullable = false)
    private OrganisationEntity organisation;

    @Column(name = "role_name", nullable = false)
    private String roleName;

    @Column(name = "is_default_role")
    private Boolean isDefaultRole = true;

    @Column(name = "is_active")
    private Boolean isActive = true;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(columnDefinition = "jsonb")
    @Convert(converter = PermissionJsonbConverter.class)
    private Map<String, Map<String, Boolean>> permissions;

    @Column(name = "created_by")
    private UUID createdBy;

    @CreationTimestamp
    @Column(name = "created_date", updatable = false)
    private LocalDateTime createdDate;

    @UpdateTimestamp
    @Column(name = "updated_date")
    private LocalDateTime updatedDate;

    public boolean hasPermission(String resource, String permission) {
        if (permissions == null) return false;
        Map<String, Boolean> resourcePermissions = permissions.get(resource);
        if (resourcePermissions == null) return false;
        return resourcePermissions.getOrDefault(permission, false);
    }

    public boolean hasAnyPermissionInResource(String resource) {
        if (permissions == null) return false;
        Map<String, Boolean> resourcePermissions = permissions.get(resource);
        if (resourcePermissions == null) return false;
        return resourcePermissions.values().stream().anyMatch(Boolean::booleanValue);
    }
}
