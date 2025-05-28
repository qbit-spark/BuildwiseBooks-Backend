package com.qbitspark.buildwisebackend.organisationService.orgnisation_members_mng.service;

import com.qbitspark.buildwisebackend.globeadvice.exceptions.AccessDeniedException;
import com.qbitspark.buildwisebackend.globeadvice.exceptions.ItemNotFoundException;

import java.util.UUID;

public interface OrganisationMemberService {

    boolean inviteMember(UUID organisationId, String email, String role) throws ItemNotFoundException, AccessDeniedException;

    boolean acceptInvitation(String token);

}