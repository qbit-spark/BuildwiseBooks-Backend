package com.qbitspark.buildwisebackend.GlobeAuthentication.Controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.qbitspark.buildwisebackend.GlobeAdvice.Exceptions.*;
import com.qbitspark.buildwisebackend.GlobeAuthentication.DTOs.RefreshTokenDTO;
import com.qbitspark.buildwisebackend.GlobeAuthentication.DTOs.UserLoginDTO;
import com.qbitspark.buildwisebackend.GlobeAuthentication.DTOs.UserRegisterDTO;
import com.qbitspark.buildwisebackend.GlobeAuthentication.Entity.GlobeUserEntity;
import com.qbitspark.buildwisebackend.GlobeAuthentication.Payloads.LoginResponse;
import com.qbitspark.buildwisebackend.GlobeAuthentication.Payloads.RefreshTokenResponse;
import com.qbitspark.buildwisebackend.GlobeAuthentication.Service.PasswordResetOTPService;
import com.qbitspark.buildwisebackend.GlobeAuthentication.Service.UserManagementService;
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

    private final UserManagementService userManagementService;
    private final PasswordResetOTPService passwordResetOTPService;

    @PostMapping("/register")
    public ResponseEntity<GlobalJsonResponseBody> userRegistration(@Valid @RequestBody UserRegisterDTO userManagementDTO) throws RandomExceptions, JsonProcessingException, ItemReadyExistException, ItemNotFoundException {

        userManagementService.registerUser(userManagementDTO);
        return new ResponseEntity<>(generateGlobalJsonResponseBody("User account created successful, please verify your email",HttpStatus.CREATED,"User account created successful, please verify your email"), HttpStatus.CREATED);

    }

    @PostMapping("/login")
    public ResponseEntity<GlobalJsonResponseBody> userLogin(@Valid @RequestBody UserLoginDTO userLoginDTO) throws VerificationException, ItemNotFoundException {
        LoginResponse loginResponse = userManagementService.loginUser(userLoginDTO);
        return new ResponseEntity<>(generateGlobalJsonResponseBody("Account login successful", HttpStatus.OK, loginResponse), HttpStatus.OK);
    }

    @PostMapping("/refreshToken")
    public ResponseEntity<GlobalJsonResponseBody> refreshToken(@Valid @RequestBody RefreshTokenDTO refreshTokenDTO) throws RandomExceptions, TokenInvalidException {
        RefreshTokenResponse refreshTokenResponse = userManagementService.refreshToken(refreshTokenDTO.getRefreshToken());
        return new ResponseEntity<>(generateGlobalJsonResponseBody("Token refreshed successful",HttpStatus.OK, refreshTokenResponse), HttpStatus.ACCEPTED);
    }


    @GetMapping("/all-users")
    public ResponseEntity<GlobalJsonResponseBody> getAllUsers() {
        List<GlobeUserEntity> userList = userManagementService.getAllUser();
        return new ResponseEntity<>(generateGlobalJsonResponseBody("All users retried successfully",HttpStatus.OK, userList), HttpStatus.CREATED);
    }


    @GetMapping("/single-user/{userId}")
    public ResponseEntity<GlobalJsonResponseBody> getSingleUser(@PathVariable UUID userId) throws  ItemNotFoundException {
        GlobeUserEntity user = userManagementService.getSingleUser(userId);
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
