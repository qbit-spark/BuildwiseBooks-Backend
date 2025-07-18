package com.qbitspark.buildwisebackend.organisation_service.roles_mng.service.impl;

import com.qbitspark.buildwisebackend.organisation_service.roles_mng.config.PermissionConfig;
import com.qbitspark.buildwisebackend.organisation_service.roles_mng.entity.OrgMemberRoleEntity;
import com.qbitspark.buildwisebackend.organisation_service.roles_mng.repo.OrgMemberRoleRepo;
import com.qbitspark.buildwisebackend.organisation_service.roles_mng.service.PermissionMigrationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class PermissionMigrationServiceImpl implements PermissionMigrationService {

    private final OrgMemberRoleRepo orgMemberRoleRepo;
    private volatile boolean migrationInProgress = false;
    private volatile String lastMigrationResult = "No migration has been run yet";

    @Override
    @Async("migrationTaskExecutor")
    public void fixMissingPermissionsAsync() {

        if (migrationInProgress) {
            CompletableFuture.completedFuture("Migration already in progress");
            return;
        }

        try {
            migrationInProgress = true;
            log.info("Starting async permission migration...");

            List<OrgMemberRoleEntity> allRoles = orgMemberRoleRepo.findAll();
            Map<String, Map<String, Boolean>> latestPermissions = PermissionConfig.getAllPermissions();

            int totalRoles = allRoles.size();
            int updatedCount = 0;

            for (int i = 0; i < allRoles.size(); i++) {
                OrgMemberRoleEntity role = allRoles.get(i);

                try {
                    boolean wasUpdated = updateRolePermissions(role, latestPermissions);
                    if (wasUpdated) {
                        updatedCount++;
                    }

                    // Log progress every 100 roles
                    if ((i + 1) % 100 == 0) {
                        log.info("Migration progress: {}/{} roles processed", i + 1, totalRoles);
                    }

                } catch (Exception e) {
                    log.error("Failed to update role {}: {}", role.getRoleId(), e.getMessage());
                }
            }

            String result = String.format("Migration completed! Updated %d out of %d roles", updatedCount, totalRoles);
            log.info(result);

            lastMigrationResult = result;
            CompletableFuture.completedFuture(result);

        } catch (Exception e) {
            String errorResult = "Migration failed: " + e.getMessage();
            log.error(errorResult);
            lastMigrationResult = errorResult;
            CompletableFuture.completedFuture(errorResult);
        } finally {
            migrationInProgress = false;
        }
    }

    @Override
    public boolean isMigrationInProgress() {
        return migrationInProgress;
    }

    @Override
    public String getMigrationStatus() {
        if (migrationInProgress) {
            return "Migration is currently in progress";
        }
        return lastMigrationResult;
    }

    @Transactional
    public boolean updateRolePermissions(OrgMemberRoleEntity role, Map<String, Map<String, Boolean>> latestPermissions) {

        Map<String, Map<String, Boolean>> currentPermissions = role.getPermissions();

        if (currentPermissions == null) {
            currentPermissions = new HashMap<>();
        }

        boolean needsUpdate = false;

        // Add missing resources and permissions
        for (String resource : latestPermissions.keySet()) {
            if (!currentPermissions.containsKey(resource)) {
                currentPermissions.put(resource, new HashMap<>());
                needsUpdate = true;
            }

            Map<String, Boolean> resourcePerms = currentPermissions.get(resource);
            for (String permission : latestPermissions.get(resource).keySet()) {
                if (!resourcePerms.containsKey(permission)) {
                    resourcePerms.put(permission, getDefaultValue(role.getRoleName()));
                    needsUpdate = true;
                }
            }
        }

        if (needsUpdate) {
            role.setPermissions(currentPermissions);
            orgMemberRoleRepo.save(role);
        }

        return needsUpdate;
    }

    private boolean getDefaultValue(String roleName) {
        return switch (roleName.toUpperCase()) {
            case "OWNER" -> true;
            case "ADMIN" -> true;
            default -> false;
        };
    }
}