package com.qbitspark.buildwisebackend.projectmng_service.utils;

import com.qbitspark.buildwisebackend.emails_service.GlobeMailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AsyncEmailService {

    private final GlobeMailService globeMailService;

    @Value("${frontend.base-url}")
    private String frontendUrl;

    @Async
    public void sendProjectTeamMemberAddedEmailAsync(String email, String userName,
                                                     String projectName, String role, UUID organisationId,
                                                     UUID projectId) {
        try {

            globeMailService.sendProjectTeamMemberAddedEmail(email, userName, projectName, role, frontendUrl + "?organisation="+organisationId+"&project=" + projectId);
            log.info("Async email sent successfully to: {}", email);
        } catch (Exception e) {
            log.error("Failed to send async email to {}: {}", email, e.getMessage());
        }
    }

    public void sendSubcontractorAssignmentEmailAsync(String email, String companyName, String name, UUID organisationId, UUID projectId) {
        try {
            globeMailService.sendSubcontractorAssignmentEmail(email, companyName,name,organisationId,projectId);
            log.info("Async email sent successfully to: {}", email);
        } catch (Exception e) {
            log.error("Failed to send async email to {}: {}", email, e.getMessage());
        }
    }
}