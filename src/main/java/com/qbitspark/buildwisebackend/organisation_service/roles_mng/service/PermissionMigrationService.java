package com.qbitspark.buildwisebackend.organisation_service.roles_mng.service;

public interface PermissionMigrationService {

    void fixMissingPermissionsAsync();

    boolean isMigrationInProgress();

    String getMigrationStatus();
}
