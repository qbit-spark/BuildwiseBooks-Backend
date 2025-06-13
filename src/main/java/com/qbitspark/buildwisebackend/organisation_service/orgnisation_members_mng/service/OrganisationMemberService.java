package com.qbitspark.buildwisebackend.organisation_service.orgnisation_members_mng.service;

import com.qbitspark.buildwisebackend.globeadvice.exceptions.*;
import com.qbitspark.buildwisebackend.authentication_service.entity.AccountEntity;
import com.qbitspark.buildwisebackend.organisation_service.organisation_mng.entity.OrganisationEntity;
import com.qbitspark.buildwisebackend.organisation_service.orgnisation_members_mng.payloads.*;

import java.util.List;
import java.util.UUID;

public interface OrganisationMemberService {

    boolean inviteMember(UUID organisationId, String email, String role) throws ItemNotFoundException, AccessDeniedException;

    AcceptInvitationResponse acceptInvitation(String token) throws ItemNotFoundException, InvitationAlreadyProcessedException, InvitationExpiredException, RandomExceptions, AccessDeniedException;

    boolean declineInvitation(String token) throws ItemNotFoundException, InvitationAlreadyProcessedException, InvitationExpiredException, RandomExceptions, AccessDeniedException;

    InvitationInfoResponse getInvitationInfo(String token) throws ItemNotFoundException;
    //This method is used
    // to add the owner of the organization as a member automatically when the organization is created.
    void addOwnerAsMember(OrganisationEntity organisation, AccountEntity owner);

    OrganisationMembersOverviewResponse getAllMembersAndInvitations(UUID organisationId) throws ItemNotFoundException, AccessDeniedException;

    List<OrganisationMemberResponse> getActiveMembers(UUID organisationId) throws ItemNotFoundException, AccessDeniedException;

    List<PendingInvitationResponse> getPendingInvitations(UUID organisationId) throws ItemNotFoundException, AccessDeniedException;

    UserOrganisationsOverviewResponse getMyOrganisations() throws ItemNotFoundException;

    boolean removeMember(UUID organisationId, UUID memberId) throws ItemNotFoundException, AccessDeniedException;

}