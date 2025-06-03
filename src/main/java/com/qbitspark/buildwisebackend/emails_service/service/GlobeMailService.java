package com.qbitspark.buildwisebackend.emails_service.service;

public interface GlobeMailService {
    boolean sendOTPEmail(String email, String otp, String userName, String header, String instructions) throws Exception;
    boolean sendOrganisationInvitationEmail(String email, String organisationName, String inviterName,
                                            String role, String acceptLink, String declineLink) throws Exception;
}
