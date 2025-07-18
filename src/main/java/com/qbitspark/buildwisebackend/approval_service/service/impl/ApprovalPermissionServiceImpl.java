package com.qbitspark.buildwisebackend.approval_service.service.impl;

import com.qbitspark.buildwisebackend.approval_service.entities.ApprovalStepInstance;
import com.qbitspark.buildwisebackend.approval_service.enums.ScopeType;
import com.qbitspark.buildwisebackend.approval_service.service.ApprovalPermissionService;
import com.qbitspark.buildwisebackend.authentication_service.entity.AccountEntity;
import com.qbitspark.buildwisebackend.organisation_service.orgnisation_members_mng.entities.OrganisationMember;
import com.qbitspark.buildwisebackend.organisation_service.orgnisation_members_mng.repo.OrganisationMemberRepo;
import com.qbitspark.buildwisebackend.organisation_service.roles_mng.entity.OrgMemberRoleEntity;
import com.qbitspark.buildwisebackend.organisation_service.roles_mng.repo.MemberRoleRepo;
import com.qbitspark.buildwisebackend.projectmng_service.entity.ProjectTeamMemberEntity;
import com.qbitspark.buildwisebackend.projectmng_service.entity.ProjectTeamRoleEntity;
import com.qbitspark.buildwisebackend.projectmng_service.repo.ProjectTeamMemberRepo;
import com.qbitspark.buildwisebackend.projectmng_service.repo.ProjectTeamRoleRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

import static org.bouncycastle.asn1.x500.style.RFC4519Style.member;

@Service
@RequiredArgsConstructor
public class ApprovalPermissionServiceImpl implements ApprovalPermissionService {

    private final OrganisationMemberRepo organisationMemberRepo;
    private final MemberRoleRepo memberRoleRepo;
    private final ProjectTeamMemberRepo projectTeamMemberRepo;
    private final ProjectTeamRoleRepo projectTeamRoleRepo;

    @Override
    public boolean canUserApprove(AccountEntity user, ApprovalStepInstance stepInstance) {
        UUID organisationId = stepInstance.getApprovalInstance().getOrganisation().getOrganisationId();
        UUID projectId = stepInstance.getApprovalInstance().getContextProjectId();
        UUID requiredRoleId = stepInstance.getRoleId();
        ScopeType scopeType = stepInstance.getScopeType();

        if (scopeType == ScopeType.ORGANIZATION) {
            return hasOrganizationRole(user, organisationId, requiredRoleId);
        } else if (scopeType == ScopeType.PROJECT) {
            return hasProjectRole(user, projectId, requiredRoleId);
        }

        return false;
    }

    private boolean hasOrganizationRole(AccountEntity user, UUID organisationId, UUID roleId) {
        OrganisationMember member = organisationMemberRepo
                .findByAccount_IdAndOrganisation_OrganisationId(user.getId(), organisationId);

        if (member == null) {
            return false;
        }

        return member.getMemberRole().getRoleId().equals(roleId) &&
                member.getMemberRole().getIsActive();
    }
    private boolean hasProjectRole(AccountEntity user, UUID projectId, UUID roleId) {
        // Find an organization member first (needed for project team membership)
        OrganisationMember orgMember = organisationMemberRepo.findByAccount_Id(user.getId());
        if (orgMember == null) {
            return false;
        }

        // Find a project team member
        ProjectTeamMemberEntity teamMember = projectTeamMemberRepo
                .findByProject_ProjectIdAndOrganisationMember_MemberId(projectId, orgMember.getMemberId());

        if (teamMember == null) {
            return false;
        }

        // Check if a team member has the required role and it's active
        return teamMember.getProjectTeamRole().getRoleId().equals(roleId) &&
                teamMember.getProjectTeamRole().getIsActive();
    }
}