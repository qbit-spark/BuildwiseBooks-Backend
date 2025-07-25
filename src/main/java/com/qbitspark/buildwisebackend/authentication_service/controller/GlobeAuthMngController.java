package com.qbitspark.buildwisebackend.authentication_service.controller;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.qbitspark.buildwisebackend.authentication_service.enums.VerificationChannels;
import com.qbitspark.buildwisebackend.authentication_service.payloads.*;
import com.qbitspark.buildwisebackend.globeadvice.exceptions.*;
import com.qbitspark.buildwisebackend.authentication_service.entity.AccountEntity;
import com.qbitspark.buildwisebackend.authentication_service.Service.AccountService;
import com.qbitspark.buildwisebackend.globeresponsebody.GlobeSuccessResponseBuilder;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static com.qbitspark.buildwisebackend.authentication_service.enums.VerificationChannels.WHATSAPP;

@AllArgsConstructor
@RestController
@RequestMapping("api/v1/auth")
public class GlobeAuthMngController {

    private final AccountService accountService;

    @PostMapping("/register")
    public ResponseEntity<GlobeSuccessResponseBuilder> accountRegistration(
            @Valid @RequestBody CreateAccountRequest createAccountRequest)
            throws Exception {

        String tempToken = accountService.registerAccount(createAccountRequest);

        RegistrationResponse registrationResponse = new RegistrationResponse(
                tempToken,
                "OTP has been sent to your " + getChannelName(createAccountRequest.getVerificationChannel()),
                LocalDateTime.now().plusMinutes(10)
        );

        GlobeSuccessResponseBuilder response = GlobeSuccessResponseBuilder.success(
                "Registration initiated successfully. Please verify with OTP.",
                registrationResponse
        );

        return ResponseEntity.status(201).body(response);
    }


    @PostMapping("/login")
    public ResponseEntity<GlobeSuccessResponseBuilder> accountLogin(@Valid @RequestBody AccountLoginRequest accountLoginRequest) throws VerificationException, ItemNotFoundException {

        LoginResponse loginResponse = accountService.loginAccount(accountLoginRequest);

        GlobeSuccessResponseBuilder response = GlobeSuccessResponseBuilder.success(
                "Account login successful",
                loginResponse
        );

        return ResponseEntity.ok(response);
    }

    @PostMapping("/refreshToken")
    public ResponseEntity<GlobeSuccessResponseBuilder> refreshToken(@Valid @RequestBody RefreshTokenRequest refreshTokenRequest) throws RandomExceptions, TokenInvalidException {

        RefreshTokenResponse refreshTokenResponse = accountService.refreshToken(refreshTokenRequest.getRefreshToken());

        GlobeSuccessResponseBuilder response = GlobeSuccessResponseBuilder.success(
                "Token refreshed successful",
                refreshTokenResponse
        );

        return ResponseEntity.ok(response);
    }

    @GetMapping("/all-users")
    public ResponseEntity<GlobeSuccessResponseBuilder> getAllUsers() {

        List<AccountEntity> userList = accountService.getAllAccounts();

        GlobeSuccessResponseBuilder response = GlobeSuccessResponseBuilder.success(
                "All users retrieved successfully",
                userList
        );

        return ResponseEntity.ok(response);
    }

    @GetMapping("/single-user/{userId}")
    public ResponseEntity<GlobeSuccessResponseBuilder> getSingleUser(@PathVariable UUID userId) throws ItemNotFoundException {

        AccountEntity user = accountService.getAccountByID(userId);

        GlobeSuccessResponseBuilder response = GlobeSuccessResponseBuilder.success(
                "User details retrieved successfully",
                user
        );

        return ResponseEntity.ok(response);
    }

    private String getChannelName(VerificationChannels channel) {
        return switch (channel) {
            case EMAIL -> "email";
            case SMS -> "phone";
            case WHATSAPP -> "WhatsApp";
            case VOICE_CALL -> "phone via voice call";
            case PUSH_NOTIFICATION -> "device";
            default -> "email";
        };
    }
}