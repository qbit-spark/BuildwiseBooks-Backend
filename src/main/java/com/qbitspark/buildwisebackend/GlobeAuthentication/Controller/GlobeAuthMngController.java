package com.qbitspark.buildwisebackend.GlobeAuthentication.Controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.qbitspark.buildwisebackend.GlobeAdvice.Exceptions.*;
import com.qbitspark.buildwisebackend.GlobeAuthentication.payloads.RefreshTokenRequest;
import com.qbitspark.buildwisebackend.GlobeAuthentication.payloads.AccountLoginRequest;
import com.qbitspark.buildwisebackend.GlobeAuthentication.entity.AccountEntity;
import com.qbitspark.buildwisebackend.GlobeAuthentication.payloads.CreateAccountRequest;
import com.qbitspark.buildwisebackend.GlobeAuthentication.payloads.LoginResponse;
import com.qbitspark.buildwisebackend.GlobeAuthentication.payloads.RefreshTokenResponse;
import com.qbitspark.buildwisebackend.GlobeAuthentication.Service.AccountService;
import com.qbitspark.buildwisebackend.GlobeResponseBody.GlobalJsonResponseBody;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@AllArgsConstructor
@RestController
@RequestMapping("api/v1/auth")
public class GlobeAuthMngController {

    private final AccountService accountService;

    @PostMapping("/register")
    public ResponseEntity<GlobalJsonResponseBody> accountRegistration(@Valid @RequestBody CreateAccountRequest createAccountRequest) throws RandomExceptions, JsonProcessingException, ItemReadyExistException, ItemNotFoundException {

        accountService.registerAccount(createAccountRequest);
        return new ResponseEntity<>(generateGlobalJsonResponseBody("User account created successful, please verify your email",HttpStatus.CREATED,"User account created successful, please verify your email"), HttpStatus.CREATED);

    }

    @PostMapping("/login")
    public ResponseEntity<GlobalJsonResponseBody> accountLogin(@Valid @RequestBody AccountLoginRequest accountLoginRequest) throws VerificationException, ItemNotFoundException {
        LoginResponse loginResponse = accountService.loginAccount(accountLoginRequest);
        return new ResponseEntity<>(generateGlobalJsonResponseBody("Account login successful", HttpStatus.OK, loginResponse), HttpStatus.OK);
    }

    @PostMapping("/refreshToken")
    public ResponseEntity<GlobalJsonResponseBody> refreshToken(@Valid @RequestBody RefreshTokenRequest refreshTokenRequest) throws RandomExceptions, TokenInvalidException {
        RefreshTokenResponse refreshTokenResponse = accountService.refreshToken(refreshTokenRequest.getRefreshToken());
        return new ResponseEntity<>(generateGlobalJsonResponseBody("Token refreshed successful",HttpStatus.OK, refreshTokenResponse), HttpStatus.ACCEPTED);
    }


    @GetMapping("/all-users")
    public ResponseEntity<GlobalJsonResponseBody> getAllUsers() {
        List<AccountEntity> userList = accountService.getAllAccounts();
        return new ResponseEntity<>(generateGlobalJsonResponseBody("All users retried successfully",HttpStatus.OK, userList), HttpStatus.CREATED);
    }


    @GetMapping("/single-user/{userId}")
    public ResponseEntity<GlobalJsonResponseBody> getSingleUser(@PathVariable UUID userId) throws  ItemNotFoundException {
        AccountEntity user = accountService.getAccountByID(userId);
        return new ResponseEntity<>(generateGlobalJsonResponseBody("User details retried successfully",HttpStatus.OK,user), HttpStatus.OK);
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
