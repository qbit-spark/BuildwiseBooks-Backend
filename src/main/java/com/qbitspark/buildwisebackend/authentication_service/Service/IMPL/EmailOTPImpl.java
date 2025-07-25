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

    @Override
    public GlobeSuccessResponseBuilder verifyEmailOTP(String email, String otpCode) throws RandomExceptions, VerificationException, ItemNotFoundException {
        // Find the user by email
        AccountEntity user = accountRepo.findByEmail(email)
                .orElseThrow(() -> new ItemNotFoundException("No such user with the given email"));

        // Find the OTP associated with the user
        UserOTP existingOTP = userOTPRepository.findUserOTPByUser(user);

        // Check if OTP exists and has not expired
        if (existingOTP != null) {
            LocalDateTime createdTime = existingOTP.getSentTime();
            if (validationUtils.isOTPExpired(createdTime)) {
                throw new RandomExceptions("OTP expired");
            }

            // Check if the provided OTP code matches the stored OTP
            if (existingOTP.getOtpCode().equals(otpCode)) {
                var currentTime = LocalDateTime.now();
                var expirationTime = existingOTP.getSentTime().plusMinutes(Long.parseLong(OTP_EXPIRE_TIME));

                // Make the OTP expire after a successful password reset
                LocalDateTime expiration = existingOTP.getSentTime().minusHours(2);
                existingOTP.setSentTime(expiration);

                userOTPRepository.save(existingOTP);

                // Validate OTP expiration and return response
                GlobeSuccessResponseBuilder response = buildSuccessResponse(user, currentTime, expirationTime, accountRepo, tokenProvider);
                if (response != null) return response;
            }
        }

        // If OTP is invalid or doesn't match
        throw new VerificationException("OTP or email provided is incorrect");
    }


    static GlobeSuccessResponseBuilder buildSuccessResponse(AccountEntity user, LocalDateTime currentTime, LocalDateTime expirationTime, AccountRepo accountRepo, JWTProvider tokenProvider) {
        if (currentTime.isBefore(expirationTime)) {
            // Mark the user as verified
            user.setIsVerified(true);
            accountRepo.save(user);

            // Generate access and refresh tokens
            Authentication authentication = new UsernamePasswordAuthenticationToken(user.getUserName(), null);
            String accessToken = tokenProvider.generateAccessToken(authentication);
            String refreshToken = tokenProvider.generateRefreshToken(authentication);

            // Construct the response
            LoginResponse loginResponse = new LoginResponse();
            loginResponse.setAccessToken(accessToken);
            loginResponse.setRefreshToken(refreshToken);
            loginResponse.setUserData(user);

            return GlobeSuccessResponseBuilder.success(
                    "OTP validation successful",
                    loginResponse
            );
        }
        return null;
    }



}
