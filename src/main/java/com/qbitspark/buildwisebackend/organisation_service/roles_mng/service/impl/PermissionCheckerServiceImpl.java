package com.qbitspark.buildwisebackend.organisation_service.roles_mng.service.impl;

import com.qbitspark.buildwisebackend.globeadvice.exceptions.AccessDeniedException;
import com.qbitspark.buildwisebackend.organisation_service.orgnisation_members_mng.entities.OrganisationMember;
import com.qbitspark.buildwisebackend.organisation_service.orgnisation_members_mng.enums.MemberStatus;
import com.qbitspark.buildwisebackend.organisation_service.roles_mng.service.PermissionCheckerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
@Service
@RequiredArgsConstructor
@Slf4j
public class PermissionCheckerServiceImpl implements PermissionCheckerService {

    @Override
    public void checkMemberPermission(OrganisationMember member, String resource, String permission) throws AccessDeniedException {

        if (member.getStatus() != MemberStatus.ACTIVE) {
            throw new AccessDeniedException("Your account is suspended/not active, contact your organisation owner");
        }

        if (member.getMemberRole() == null) {
            throw new AccessDeniedException("No role assigned to this member");
        }

        boolean hasPermission = member.getMemberRole().hasPermission(resource, permission);

        if (!hasPermission) {
            throw new AccessDeniedException(
                    String.format("Permission denied: You do not have permission to %s for %s",
                            permission, resource));
        }


    }

    @Override
    public Boolean hasAnyPermissionInResource(OrganisationMember member, String resource) {

        if (member.getStatus() != MemberStatus.ACTIVE) {
            return false;
        }

        if (member.getMemberRole() == null) {
            return false;
        }

        return member.getMemberRole().hasAnyPermissionInResource(resource);
    }
}