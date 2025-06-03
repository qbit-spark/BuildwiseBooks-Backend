package com.qbitspark.buildwisebackend.authentication_service.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.qbitspark.buildwisebackend.globeadvice.exceptions.ItemNotFoundException;
import com.qbitspark.buildwisebackend.globeadvice.exceptions.ItemReadyExistException;
import com.qbitspark.buildwisebackend.globeadvice.exceptions.RandomExceptions;
import com.qbitspark.buildwisebackend.globeadvice.exceptions.VerificationException;
import com.qbitspark.buildwisebackend.authentication_service.payloads.PswResetAndOTPRequestBody;
import com.qbitspark.buildwisebackend.authentication_service.payloads.RequestSMSOTPBody;
import com.qbitspark.buildwisebackend.authentication_service.Service.PasswordResetOTPService;
import com.qbitspark.buildwisebackend.globeresponsebody.GlobeSuccessResponseBuilder;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("api/v1/auth")
public class PasswordResetController {

    private final PasswordResetOTPService passwordResetOTPService;

    @PostMapping("/psw-request-otp")
    public ResponseEntity<GlobeSuccessResponseBuilder> requestOTP(@Valid @RequestBody RequestSMSOTPBody requestOTPBody) throws RandomExceptions, JsonProcessingException, ItemReadyExistException, ItemNotFoundException {

        String email = passwordResetOTPService.generateAndSendPSWDResetOTP(requestOTPBody.getEmail());

        GlobeSuccessResponseBuilder response = GlobeSuccessResponseBuilder.success(
                "OTP for password reset was generated and sent to: " + email
        );

        return ResponseEntity.ok(response);
    }

    @PostMapping("/verify-otp-and-reset")
    public ResponseEntity<GlobeSuccessResponseBuilder> verifyOTP(@Valid @RequestBody PswResetAndOTPRequestBody pswResetAndOTPRequestBody) throws RandomExceptions, ItemReadyExistException, VerificationException, ItemNotFoundException {

        Object result = passwordResetOTPService.verifyOTPAndResetPassword(
                pswResetAndOTPRequestBody.getEmail(),
                pswResetAndOTPRequestBody.getCode(),
                pswResetAndOTPRequestBody.getNewPassword()
        );

        GlobeSuccessResponseBuilder response = GlobeSuccessResponseBuilder.success(
                "Password reset successfully",
                result
        );

        return ResponseEntity.ok(response);
    }
}