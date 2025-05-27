package com.qbitspark.buildwisebackend.GlobeAuthentication.Service.IMPL;

import com.qbitspark.buildwisebackend.GlobeAdvice.Exceptions.ItemNotFoundException;
import com.qbitspark.buildwisebackend.GlobeAdvice.Exceptions.RandomExceptions;
import com.qbitspark.buildwisebackend.GlobeAdvice.Exceptions.VerificationException;
import com.qbitspark.buildwisebackend.GlobeAuthentication.entity.AccountEntity;
import com.qbitspark.buildwisebackend.GlobeAuthentication.entity.UserOTP;
import com.qbitspark.buildwisebackend.GlobeAuthentication.payloads.LoginResponse;
import com.qbitspark.buildwisebackend.GlobeAuthentication.Repository.GlobeAccountRepository;
import com.qbitspark.buildwisebackend.GlobeAuthentication.Repository.UserOTPRepository;
import com.qbitspark.buildwisebackend.GlobeAuthentication.Service.EmailOTPService;
import com.qbitspark.buildwisebackend.GlobeResponseBody.GlobalJsonResponseBody;
import com.qbitspark.buildwisebackend.GlobeSecurity.JWTProvider;
import com.qbitspark.buildwisebackend.GlobeValidationUtils.CustomValidationUtils;
import com.qbitspark.buildwisebackend.emails.service.GlobeMailService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.Random;


@RequiredArgsConstructor
@Service
public class EmailOTPIMPL implements EmailOTPService {

    private final JavaMailSender mailSender;

    private final TemplateEngine templateEngine;
    private final UserOTPRepository otpRepository;
    private final GlobeAccountRepository globeAccountRepository;
    private final UserOTPRepository userOTPRepository;
    private final CustomValidationUtils validationUtils;
    private final JWTProvider tokenProvider;
    private final GlobeMailService globeMailService;


    @Value("${otp.expire_time.minutes}")
    private String OTP_EXPIRE_TIME;

    @Override
    public void generateAndSendEmailOTP(AccountEntity userAuthEntity, String emailHeader, String instructionText) throws RandomExceptions, ItemNotFoundException {
        // Find the account by email
        AccountEntity account = globeAccountRepository.findByEmail(userAuthEntity.getEmail())
                .orElseThrow(() -> new ItemNotFoundException("No such account with the given email"));

        // Check if there's an existing OTP
        UserOTP existingOTP = otpRepository.findUserOTPByUser(account);

        // Generate a new OTP code
        String newOtpCode = generateOtpCode();

        if (existingOTP == null) {
            // Create a new OTP entry if none exists for the account
            existingOTP = new UserOTP();
            existingOTP.setUser(account);
            existingOTP.setSentTime(LocalDateTime.now());
        }
        // Update OTP details
        existingOTP.setOtpCode(newOtpCode);
        existingOTP.setSentTime(LocalDateTime.now());

        // Save the OTP to the repository
        otpRepository.save(existingOTP);

        //Update account verification status
        account.setIsEmailVerified(true);
        globeAccountRepository.save(account);

        // Send the OTP via centralized email service - SIMPLE!
        try {
            globeMailService.sendOTPEmail(
                    account.getEmail(),
                    newOtpCode,
                    "Customer"  // You can use account.getFirstName() if available
            );
        } catch (Exception ex) {
            throw new RandomExceptions("Failed to send verification email to account: " + account.getEmail() + ". " + ex.getMessage());
        }
    }

    @Override
    public GlobalJsonResponseBody verifyEmailOTP(String email, String otpCode) throws RandomExceptions, VerificationException, ItemNotFoundException {
        // Find the user by email
        AccountEntity user = globeAccountRepository.findByEmail(email)
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

                // Make the OTP expire after successful password reset
                LocalDateTime expiration = existingOTP.getSentTime().minusHours(2);
                existingOTP.setSentTime(expiration);

                userOTPRepository.save(existingOTP);

                // Validate OTP expiration
                GlobalJsonResponseBody globalJsonResponseBody = getGlobalJsonResponseBody(user, currentTime, expirationTime, globeAccountRepository, tokenProvider);
                if (globalJsonResponseBody != null) return globalJsonResponseBody;
            }
        }

        // If OTP is invalid or doesn't match
        throw new VerificationException("OTP or email provided is incorrect");
    }

    static GlobalJsonResponseBody getGlobalJsonResponseBody(AccountEntity user, LocalDateTime currentTime, LocalDateTime expirationTime, GlobeAccountRepository globeAccountRepository, JWTProvider tokenProvider) {
        if (currentTime.isBefore(expirationTime)) {
            // Mark the user as verified
            user.setIsVerified(true);
            globeAccountRepository.save(user);

            // Generate access and refresh tokens
            Authentication authentication = new UsernamePasswordAuthenticationToken(user.getUserName(), null);
            String accessToken = tokenProvider.generateAccessToken(authentication);
            String refreshToken = tokenProvider.generateRefreshToken(authentication);

            // Construct the response
            LoginResponse loginResponse = new LoginResponse();
            loginResponse.setAccessToken(accessToken);
            loginResponse.setRefreshToken(refreshToken);
            loginResponse.setUserData(user);

            GlobalJsonResponseBody globalJsonResponseBody = new GlobalJsonResponseBody();
            globalJsonResponseBody.setMessage("OTP validation successful");
            globalJsonResponseBody.setData(loginResponse);
            globalJsonResponseBody.setSuccess(true);
            globalJsonResponseBody.setAction_time(new Date());
            globalJsonResponseBody.setHttpStatus(HttpStatus.OK);

            return globalJsonResponseBody;
        }
        return null;
    }

    private String generateOtpCode() {
        // Generate a random OTP code of 6 digits
        Random random = new Random();
        int otp = random.nextInt(900000) + 100000;
        return String.valueOf(otp);
    }
}
