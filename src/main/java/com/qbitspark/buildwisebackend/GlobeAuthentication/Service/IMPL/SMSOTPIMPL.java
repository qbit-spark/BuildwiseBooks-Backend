package com.qbitspark.buildwisebackend.GlobeAuthentication.Service.IMPL;

import com.fasterxml.jackson.core.JsonProcessingException;

import com.qbitspark.buildwisebackend.GlobeAPIClient.BasicAuthApiClient;
import com.qbitspark.buildwisebackend.GlobeAdvice.Exceptions.ItemNotFoundException;
import com.qbitspark.buildwisebackend.GlobeAdvice.Exceptions.RandomExceptions;
import com.qbitspark.buildwisebackend.GlobeAdvice.Exceptions.VerificationException;
import com.qbitspark.buildwisebackend.GlobeAuthentication.payloads.AccountLoginRequest;
import com.qbitspark.buildwisebackend.GlobeAuthentication.entity.AccountEntity;
import com.qbitspark.buildwisebackend.GlobeAuthentication.entity.UserOTP;
import com.qbitspark.buildwisebackend.GlobeAuthentication.Repository.GlobeAccountRepository;
import com.qbitspark.buildwisebackend.GlobeAuthentication.Repository.UserOTPRepository;
import com.qbitspark.buildwisebackend.GlobeAuthentication.Service.SMSOTPService;
import com.qbitspark.buildwisebackend.GlobeResponseBody.GlobalJsonResponseBody;
import com.qbitspark.buildwisebackend.GlobeSecurity.JWTProvider;
import com.qbitspark.buildwisebackend.GlobeValidationUtils.CustomValidationUtils;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Random;

@RequiredArgsConstructor
@Service
public class SMSOTPIMPL implements SMSOTPService {

    private final UserOTPRepository userOTPRepository;
    private final GlobeAccountRepository userManagerRepository;
    private final ModelMapper modelMapper;
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
    public GlobalJsonResponseBody verifySMSOTP(String phoneNumber, String otpCode) throws ItemNotFoundException, RandomExceptions, ItemNotFoundException, VerificationException {
        AccountEntity user = userManagerRepository.findAccountEntitiesByPhoneNumber(phoneNumber)
                .orElseThrow(() -> new ItemNotFoundException("No such user with given phone number"));

        UserOTP existingOTP = userOTPRepository.findUserOTPByUser(user);

        //Todo: we have to check if OTP is expired
        LocalDateTime createdTime = existingOTP.getSentTime();
        if (validationUtils.isOTPExpired(createdTime)){
            throw new RandomExceptions("OTP expired");
        }

        if (existingOTP != null && existingOTP.getOtpCode().equals(otpCode)) {
            LocalDateTime currentTime = LocalDateTime.now();
            LocalDateTime expirationTime = existingOTP.getSentTime().plusDays(1); // Assuming OTP is valid for 1 day
            // Check if OTP is not expired
            GlobalJsonResponseBody globalJsonResponseBody = EmailOTPIMPL.getGlobalJsonResponseBody(user, currentTime, expirationTime, userManagerRepository, tokenProvider);
            if (globalJsonResponseBody != null) return globalJsonResponseBody;
        }

        throw new VerificationException("OTP or phone number provided is incorrect");
    }


//    public void sendBulkSMS(String phoneNumber, String code, String username, String password) throws RandomExceptions, JsonProcessingException {
//            PasswordResetOTPServiceIMPL.SMSRequestMethod(phoneNumber, code, username, password, SENDER_ADDRESS, basicAuthApiClient, API_URL);
//
//    }


    private String generateOtpCode() {
        // Generate a random OTP code of 6 digits
        Random random = new Random();
        int otp = random.nextInt(900000) + 100000;
        return String.valueOf(otp);
    }

    public AccountLoginRequest convertEntityToDTO(AccountEntity userManger) {
        return modelMapper.map(userManger, AccountLoginRequest.class);
    }
}
