package com.qbitspark.buildwisebackend.authentication_service.Service.IMPL;

import com.fasterxml.jackson.core.JsonProcessingException;

import com.qbitspark.buildwisebackend.globeAPIclient.BasicAuthApiClient;
import com.qbitspark.buildwisebackend.globeadvice.exceptions.ItemNotFoundException;
import com.qbitspark.buildwisebackend.globeadvice.exceptions.RandomExceptions;
import com.qbitspark.buildwisebackend.globeadvice.exceptions.VerificationException;
import com.qbitspark.buildwisebackend.authentication_service.entity.AccountEntity;
import com.qbitspark.buildwisebackend.authentication_service.entity.UserOTP;
import com.qbitspark.buildwisebackend.authentication_service.Repository.AccountRepo;
import com.qbitspark.buildwisebackend.authentication_service.Repository.UserOTPRepository;
import com.qbitspark.buildwisebackend.authentication_service.Service.SMSOTPService;
import com.qbitspark.buildwisebackend.globesecurity.JWTProvider;
import com.qbitspark.buildwisebackend.globevalidationutils.CustomValidationUtils;
import com.qbitspark.buildwisebackend.authentication_service.payloads.LoginResponse;
import com.qbitspark.buildwisebackend.globeresponsebody.GlobeSuccessResponseBuilder;
import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Random;

@RequiredArgsConstructor
@Service
public class SMSOTPIMPL implements SMSOTPService {

    private final UserOTPRepository userOTPRepository;
    private final AccountRepo userManagerRepository;

    private final JWTProvider tokenProvider;
    private final BasicAuthApiClient basicAuthApiClient;

    private final CustomValidationUtils validationUtils;

    @Value("${api.sms-url}")
    private  String API_URL ;
    @Value("${api.sms-username}")
    private  String USERNAME;
    @Value("${api.sms-password}")
    private  String PASSWORD;
    @Value("${api.sms-sender}")
    private  String SENDER_ADDRESS;



    @Override
    public String generateAndSendSMSOTP(String phoneNumber) throws RandomExceptions, JsonProcessingException, ItemNotFoundException {

        AccountEntity user = userManagerRepository.findAccountEntitiesByPhoneNumber(phoneNumber).orElseThrow(()-> new ItemNotFoundException("No such user with given phone number"));


        UserOTP existingOTP = userOTPRepository.findUserOTPByUser(user);


        String newOtpCode = generateOtpCode();

        if (existingOTP == null) {
            // Create a new OTP if none exists for the user
            existingOTP = new UserOTP();
            existingOTP.setUser(userManagerRepository.findAccountEntitiesByPhoneNumber(phoneNumber).orElseThrow(() -> new ItemNotFoundException("Phone number is wrong")));
            existingOTP.setSentTime(LocalDateTime.now());
        }
        existingOTP.setOtpCode(newOtpCode);
        existingOTP.setSentTime(LocalDateTime.now());
        // Send the OTP via SMS
        //sendBulkSMS(phoneNumber, newOtpCode, USERNAME, PASSWORD);

        // Save the OTP to the repository
        userOTPRepository.save(existingOTP);
        
        return phoneNumber;
    }

    @Override
    public GlobeSuccessResponseBuilder verifySMSOTP(String phoneNumber, String otpCode) throws ItemNotFoundException, RandomExceptions, VerificationException {

        AccountEntity user = userManagerRepository.findAccountEntitiesByPhoneNumber(phoneNumber)
                .orElseThrow(() -> new ItemNotFoundException("No such user with given phone number"));

        UserOTP existingOTP = userOTPRepository.findUserOTPByUser(user);

        // Check if OTP is expired
        LocalDateTime createdTime = existingOTP.getSentTime();
        if (validationUtils.isOTPExpired(createdTime)) {
            throw new RandomExceptions("OTP expired");
        }

        if (existingOTP != null && existingOTP.getOtpCode().equals(otpCode)) {
            LocalDateTime currentTime = LocalDateTime.now();
            LocalDateTime expirationTime = existingOTP.getSentTime().plusDays(1); // Assuming OTP is valid for 1 day

            // Check if OTP is not expired and build success response
            GlobeSuccessResponseBuilder response = buildSMSSuccessResponse(user, currentTime, expirationTime, userManagerRepository, tokenProvider);
            if (response != null) return response;
        }

        throw new VerificationException("OTP or phone number provided is incorrect");
    }

    static GlobeSuccessResponseBuilder buildSMSSuccessResponse(AccountEntity user, LocalDateTime currentTime, LocalDateTime expirationTime, AccountRepo userManagerRepository, JWTProvider tokenProvider) {
        if (currentTime.isBefore(expirationTime)) {
            // Mark the user as verified
            user.setIsVerified(true);
            userManagerRepository.save(user);

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
                    "SMS OTP validation successful",
                    loginResponse
            );
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
