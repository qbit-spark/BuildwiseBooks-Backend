package com.qbitspark.buildwisebackend.GlobeAuthentication.Controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.qbitspark.buildwisebackend.GlobeAdvice.Exceptions.ItemNotFoundException;
import com.qbitspark.buildwisebackend.GlobeAdvice.Exceptions.ItemReadyExistException;
import com.qbitspark.buildwisebackend.GlobeAdvice.Exceptions.RandomExceptions;
import com.qbitspark.buildwisebackend.GlobeAdvice.Exceptions.VerificationException;
import com.qbitspark.buildwisebackend.GlobeAuthentication.payloads.PswResetAndOTPRequestBody;
import com.qbitspark.buildwisebackend.GlobeAuthentication.payloads.RequestSMSOTPBody;
import com.qbitspark.buildwisebackend.GlobeAuthentication.Service.PasswordResetOTPService;
import com.qbitspark.buildwisebackend.GlobeResponseBody.GlobalJsonResponseBody;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
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
    public ResponseEntity<GlobalJsonResponseBody> requestOTP(@Valid @RequestBody RequestSMSOTPBody requestOTPBody) throws RandomExceptions, JsonProcessingException, ItemReadyExistException, ItemNotFoundException {
        String email = passwordResetOTPService.generateAndSendPSWDResetOTP(requestOTPBody.getEmail());
        return new ResponseEntity<>(generateGlobalJsonResponseBody("OTP for password reset was generated and sent to: "+email, HttpStatus.OK,"OTP for password reset was generated and sent to: "+email ), HttpStatus.OK);
    }

    @PostMapping("/verify-otp-and-reset")
    public ResponseEntity<GlobalJsonResponseBody> verifyOTP(@Valid @RequestBody PswResetAndOTPRequestBody pswResetAndOTPRequestBody) throws  RandomExceptions, ItemReadyExistException, VerificationException, ItemNotFoundException {
        return new ResponseEntity<>(passwordResetOTPService.verifyOTPAndResetPassword(pswResetAndOTPRequestBody.getEmail(), pswResetAndOTPRequestBody.getCode(), pswResetAndOTPRequestBody.getNewPassword()), HttpStatus.CREATED);
    }

    private GlobalJsonResponseBody generateGlobalJsonResponseBody(String message, HttpStatus httpStatus, Object data) {
        GlobalJsonResponseBody globalJsonResponseBody = new GlobalJsonResponseBody();
        globalJsonResponseBody.setSuccess(true);
        globalJsonResponseBody.setHttpStatus(httpStatus);
        globalJsonResponseBody.setData(data);
        globalJsonResponseBody.setMessage(message);
        globalJsonResponseBody.setAction_time(new java.util.Date());
        return globalJsonResponseBody;
    }

}
