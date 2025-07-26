package com.qbitspark.buildwisebackend.organisation_service.roles_mng.service;

import com.qbitspark.buildwisebackend.globeadvice.exceptions.AccessDeniedException;
import com.qbitspark.buildwisebackend.organisation_service.orgnisation_members_mng.entities.OrganisationMember;

public interface PermissionCheckerService {

    void checkMemberPermission(OrganisationMember member, String resource, String permission) throws AccessDeniedException;

    Boolean hasAnyPermissionInResource(OrganisationMember member, String resource);

}