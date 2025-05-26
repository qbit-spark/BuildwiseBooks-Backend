package com.qbitspark.buildwisebackend.GlobeAuthentication.Controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.qbitspark.buildwisebackend.GlobeAdvice.Exceptions.ItemNotFoundException;
import com.qbitspark.buildwisebackend.GlobeAdvice.Exceptions.RandomExceptions;
import com.qbitspark.buildwisebackend.GlobeAdvice.Exceptions.VerificationException;
import com.qbitspark.buildwisebackend.GlobeAuthentication.Entity.GlobeUserEntity;
import com.qbitspark.buildwisebackend.GlobeAuthentication.Payloads.EmailOTPRequestBody;
import com.qbitspark.buildwisebackend.GlobeAuthentication.Payloads.RequestEmailOTPBody;
import com.qbitspark.buildwisebackend.GlobeAuthentication.Repository.GlobeUserRepository;
import com.qbitspark.buildwisebackend.GlobeAuthentication.Service.EmailOTPService;
import com.qbitspark.buildwisebackend.GlobeAuthentication.Service.SMSOTPService;
import com.qbitspark.buildwisebackend.GlobeResponseBody.GlobalJsonResponseBody;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Date;

@RequiredArgsConstructor
@RestController
@RequestMapping("api/v1/auth")
public class OTPController {

    private final SMSOTPService smsotpService;
    private final GlobeUserRepository globeUserRepository;
    private final EmailOTPService emailOTPService;


    @PostMapping("/request-otp")
    public ResponseEntity<GlobalJsonResponseBody> requestEmailOTP(@Valid @RequestBody RequestEmailOTPBody requestEmailOTPBody) throws RandomExceptions, JsonProcessingException, ItemNotFoundException {
        GlobeUserEntity userAuthEntity = globeUserRepository.findByEmail(requestEmailOTPBody.getEmail()).orElseThrow(
                ()->new ItemNotFoundException("User with provided email does not exist")
        );

        // Send the OTP via Email for password reset
        String emailHeader = "Welcome to Kitchen Support!";
        String instructionText = "Please use the following OTP to complete your registration:";
        emailOTPService.generateAndSendEmailOTP(userAuthEntity, emailHeader, instructionText);

        GlobalJsonResponseBody globalJsonResponseBody = new GlobalJsonResponseBody();
        globalJsonResponseBody.setMessage("New OTP code sent successful");
        globalJsonResponseBody.setData("New OTP code set successful to "+userAuthEntity.getEmail());
        globalJsonResponseBody.setSuccess(true);
        globalJsonResponseBody.setAction_time(new Date());
        globalJsonResponseBody.setHttpStatus(HttpStatus.OK);

        return ResponseEntity.ok(globalJsonResponseBody);
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<GlobalJsonResponseBody> verifyEmailOTP(@Valid @RequestBody EmailOTPRequestBody emailOTPRequestBody) throws RandomExceptions, VerificationException, ItemNotFoundException {
        return new ResponseEntity<>(emailOTPService.verifyEmailOTP(emailOTPRequestBody.getEmail(), emailOTPRequestBody.getCode()), HttpStatus.OK);
    }
}
