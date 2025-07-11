package com.qbitspark.buildwisebackend.organisation_service.roles_mng.controller;

import com.qbitspark.buildwisebackend.globeresponsebody.GlobeSuccessResponseBuilder;
import com.qbitspark.buildwisebackend.organisation_service.roles_mng.service.PermissionMigrationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/system/permissions")
@RequiredArgsConstructor
@Slf4j
public class PermissionMigrationController {

    private final PermissionMigrationService migrationService;
    @PostMapping("/migrate")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<GlobeSuccessResponseBuilder> migratePermissions() {

        if (migrationService.isMigrationInProgress()) {
            return ResponseEntity.ok(
                    GlobeSuccessResponseBuilder.success("Migration is already in progress")
            );
        }

         migrationService.fixMissingPermissionsAsync();


        return ResponseEntity.ok(
                GlobeSuccessResponseBuilder.success("Permission migration started in background. Check logs for progress.")
        );
    }

    @GetMapping("/migration-status")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<GlobeSuccessResponseBuilder> getMigrationStatus() {

        boolean inProgress = migrationService.isMigrationInProgress();
        String status = inProgress ? "Migration in progress" : "No migration running";

        return ResponseEntity.ok(
                GlobeSuccessResponseBuilder.success(status, Map.of("inProgress", inProgress))
        );
    }
}