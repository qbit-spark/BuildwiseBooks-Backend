package com.qbitspark.buildwisebackend.emails.service.IMPL;

import com.qbitspark.buildwisebackend.emails.service.GlobeMailService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;


@RequiredArgsConstructor
@Service
@Slf4j
public class GlobeMailIMPL implements GlobeMailService {

    private final EmailsHelperMethodsIMPL emailsHelperMethodsIMPL;

    @Override
    public boolean sendOTPEmail(String email, String otp, String userName, String textHeader, String instructions) throws Exception {
        try {
            log.info("Sending OTP email to: {} for user: {}", email, userName);

            Map<String, Object> templateVariables = new HashMap<>();
            templateVariables.put("emailHeader", textHeader);
            templateVariables.put("userName", userName != null ? userName : "User");
            templateVariables.put("instructionText", instructions);
            templateVariables.put("otpCode", otp);

            // Send email using template
            String subject = "Account Verification - Your OTP Code";
            emailsHelperMethodsIMPL.sendTemplateEmail(
                    email,
                    subject,
                    "verification_email",
                    templateVariables
            );

            log.info("OTP email sent successfully to: {}", email);
            return true;

        } catch (Exception e) {
            log.error("Failed to send OTP email to: {}", email, e);
            throw new Exception("Failed to send OTP email: " + e.getMessage(), e);
        }
    }
}
