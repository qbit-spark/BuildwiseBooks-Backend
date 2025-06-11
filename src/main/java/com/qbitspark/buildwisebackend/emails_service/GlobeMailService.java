package com.qbitspark.buildwisebackend.emails_service;

import java.util.UUID;

public interface GlobeMailService {
    boolean sendOTPEmail(String email, String otp, String userName, String header, String instructions) throws Exception;
    boolean sendOrganisationInvitationEmail(String email, String organisationName, String inviterName,
                                            String role, String acceptLink, String declineLink) throws Exception;
    void sendProjectTeamMemberAddedEmail(String email, String userName, String projectName, String role, String projectLink) throws Exception;

    void sendSubcontractorAssignmentEmail(String email, String companyName, String name, UUID organisationId, UUID projectId) throws Exception;
}
