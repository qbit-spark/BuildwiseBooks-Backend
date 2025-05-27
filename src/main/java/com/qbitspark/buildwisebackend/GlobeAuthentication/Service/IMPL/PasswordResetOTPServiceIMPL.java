package com.qbitspark.buildwisebackend.GlobeAuthentication.Service.IMPL;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.qbitspark.buildwisebackend.GlobeAdvice.Exceptions.ItemNotFoundException;
import com.qbitspark.buildwisebackend.GlobeAdvice.Exceptions.RandomExceptions;
import com.qbitspark.buildwisebackend.GlobeAdvice.Exceptions.VerificationException;
import com.qbitspark.buildwisebackend.GlobeAuthentication.entity.AccountEntity;
import com.qbitspark.buildwisebackend.GlobeAuthentication.entity.UserOTP;
import com.qbitspark.buildwisebackend.GlobeAuthentication.Repository.GlobeAccountRepository;
import com.qbitspark.buildwisebackend.GlobeAuthentication.Repository.UserOTPRepository;
import com.qbitspark.buildwisebackend.GlobeAuthentication.Service.EmailOTPService;
import com.qbitspark.buildwisebackend.GlobeAuthentication.Service.PasswordResetOTPService;
import com.qbitspark.buildwisebackend.GlobeResponseBody.GlobalJsonResponseBody;
import com.qbitspark.buildwisebackend.GlobeValidationUtils.CustomValidationUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Date;

@RequiredArgsConstructor
@Service
public class PasswordResetOTPServiceIMPL implements PasswordResetOTPService {

    @Value("${otp.expire_time.minutes}")
    private String EXPIRE_TIME;

   private final GlobeAccountRepository globeAccountRepository;
    private final CustomValidationUtils validationUtils;
    private final PasswordEncoder passwordEncoder;
    private final EmailOTPService emailOTPService;
    private final UserOTPRepository userOTPRepository;

    @Override
    public String generateAndSendPSWDResetOTP(String email) throws RandomExceptions, JsonProcessingException, ItemNotFoundException {

            AccountEntity accountEntity = globeAccountRepository.findByEmail(email)
                .orElseThrow(()-> new ItemNotFoundException("No such user with given email"));

            if (accountEntity.getIsVerified().equals(false)){
                throw new RandomExceptions("You need to verify your account first before reset password");
            }


        //Todo: Send the OTP via SMS
        //sendBulkSMS(email, newOtpCode, USERNAME, PASSWORD);

        //Todo: Send the OTP via Email
        String emailHeader = "Password Reset Request";
        String instructionText = "Please use the following OTP to reset your password:";
        emailOTPService.generateAndSendEmailOTP(accountEntity, emailHeader, instructionText);


        return email;
    }

    @Override
    public GlobalJsonResponseBody verifyOTPAndResetPassword(String email, String otpCode, String newPassword) throws RandomExceptions, ItemNotFoundException, VerificationException {

        // Fetch the user by phone number
        AccountEntity user = globeAccountRepository.findByEmail(email)
                .orElseThrow(() -> new ItemNotFoundException("No such user with given phone number"));

        UserOTP existingOTP = userOTPRepository.findUserOTPByUser(user);
                if (existingOTP == null) {
            throw new VerificationException("No OTP found for this phone number/email. Please request a new OTP.");
        }


        // Check if OTP is expired
        LocalDateTime createdTime = existingOTP.getSentTime();
        if (validationUtils.isOTPExpired(createdTime)) {
            throw new RandomExceptions("OTP expired");
        }

        // Verify the OTP code
        if (existingOTP.getOtpCode().equals(otpCode)) {
            LocalDateTime currentTime = LocalDateTime.now();
            LocalDateTime expirationTime = existingOTP.getSentTime().plusMinutes(Long.parseLong(EXPIRE_TIME));

            // Check if OTP is not expired
            if (currentTime.isBefore(expirationTime)) {

                // Reset the password
                user.setPassword(passwordEncoder.encode(newPassword)); // Assuming you have a PasswordEncoder bean
                globeAccountRepository.save(user); // Save the new password to the user record

                // Make the OTP expire after successful password reset
                LocalDateTime expiration = existingOTP.getSentTime().minusHours(2);
                existingOTP.setSentTime(expiration);

                userOTPRepository.save(existingOTP);

                // Prepare success response
                GlobalJsonResponseBody globalJsonResponseBody = new GlobalJsonResponseBody();
                globalJsonResponseBody.setMessage("Password reset successful");
                globalJsonResponseBody.setData("Password reset successful");
                globalJsonResponseBody.setSuccess(true);
                globalJsonResponseBody.setAction_time(new Date());
                globalJsonResponseBody.setHttpStatus(HttpStatus.OK);

                return globalJsonResponseBody;
            }else {
                throw new VerificationException("OTP expired");
            }
        }

        throw new VerificationException("OTP or phone number provided is incorrect");
    }
}
