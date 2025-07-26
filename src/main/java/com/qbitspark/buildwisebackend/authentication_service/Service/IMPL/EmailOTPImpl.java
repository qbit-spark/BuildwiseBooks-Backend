package com.qbitspark.buildwisebackend.authentication_service.Service.IMPL;

import com.qbitspark.buildwisebackend.globeadvice.exceptions.ItemNotFoundException;
import com.qbitspark.buildwisebackend.globeadvice.exceptions.RandomExceptions;
import com.qbitspark.buildwisebackend.globeadvice.exceptions.VerificationException;
import com.qbitspark.buildwisebackend.authentication_service.Repository.PasswordResetOTPRepo;
import com.qbitspark.buildwisebackend.authentication_service.entity.AccountEntity;
import com.qbitspark.buildwisebackend.authentication_service.entity.PasswordResetOTPEntity;
import com.qbitspark.buildwisebackend.authentication_service.entity.UserOTP;
import com.qbitspark.buildwisebackend.authentication_service.payloads.LoginResponse;
import com.qbitspark.buildwisebackend.authentication_service.Repository.AccountRepo;
import com.qbitspark.buildwisebackend.authentication_service.Repository.UserOTPRepository;
import com.qbitspark.buildwisebackend.authentication_service.Service.EmailOTPService;
import com.qbitspark.buildwisebackend.globesecurity.JWTProvider;
import com.qbitspark.buildwisebackend.globevalidationutils.CustomValidationUtils;
import com.qbitspark.buildwisebackend.emails_service.GlobeMailService;
import com.qbitspark.buildwisebackend.globeresponsebody.GlobeSuccessResponseBuilder;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;


@RequiredArgsConstructor
@Service
public class EmailOTPImpl implements EmailOTPService {


    private final UserOTPRepository otpRepository;
    private final AccountRepo accountRepo;
    private final UserOTPRepository userOTPRepository;
    private final CustomValidationUtils validationUtils;
    private final JWTProvider tokenProvider;
    private final GlobeMailService globeMailService;
    private final PasswordResetOTPRepo passwordResetOTPRepo;

    @Value("${otp.expire_time.minutes}")
    private String OTP_EXPIRE_TIME;


    @Override
    public void generateAndSendEmailOTP(AccountEntity userAuthEntity, String emailHeader, String instructionText) throws RandomExceptions, ItemNotFoundException {

    }

    @Override
    public void sendRegistrationOTP(String email, String otpCode, String firstName, String emailHeader, String instructionText) throws RandomExceptions, ItemNotFoundException {

        try {
            globeMailService.sendOTPEmail(
                    email,
                    otpCode,
                    firstName,
                    emailHeader,
                    instructionText);
        } catch (Exception ex) {
            throw new RandomExceptions("Failed to send verification email to account: " + email + ". " + ex.getMessage());
        }

    }

    @Override
    public void generateAndSendPasswordResetEmail(AccountEntity userAuthEntity, String emailHeader, String instructionText) throws RandomExceptions, ItemNotFoundException {

        AccountEntity account = accountRepo.findByEmail(userAuthEntity.getEmail())
                .orElseThrow(() -> new ItemNotFoundException("No such account with the given email"));

        if (!account.getIsEmailVerified() || !account.getIsVerified() ){
            throw new RandomExceptions("You need to verify your account first before reset password");
        }

        // Check if there's an existing OTP
        PasswordResetOTPEntity existingOTP = passwordResetOTPRepo.findPasswordResetOTPEntitiesByAccount(account);

        // Generate a new OTP code
        String newOtpCode = "generateOtpCode()";

        if (existingOTP == null) {
            // Create a new OTP entry if none exists for the account
            existingOTP = new PasswordResetOTPEntity();
            existingOTP.setAccount(account);
            existingOTP.setSentTime(LocalDateTime.now());
        }
        // Update OTP details
        existingOTP.setOtpCode(newOtpCode);
        existingOTP.setSentTime(LocalDateTime.now());

        passwordResetOTPRepo.save(existingOTP);

        // Send the OTP via centralized email service - SIMPLE!
        try {
            globeMailService.sendOTPEmail(
                    account.getEmail(),
                    newOtpCode,
                    account.getUserName(),
                    emailHeader,
                    instructionText);
        } catch (Exception ex) {
            throw new RandomExceptions("Failed to send verification email to account: " + account.getEmail() + ". " + ex.getMessage());
        }

    }
}
