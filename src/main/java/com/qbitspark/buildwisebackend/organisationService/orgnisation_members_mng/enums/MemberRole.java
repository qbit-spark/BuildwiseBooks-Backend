package com.qbitspark.buildwisebackend.organisationService.orgnisation_members_mng.enums;

public enum MemberRole {
    OWNER,   // Can do everything, delete org
    ADMIN,   // Can invite/manage members, can't delete org
    MEMBER   // Basic access, can't manage members
}