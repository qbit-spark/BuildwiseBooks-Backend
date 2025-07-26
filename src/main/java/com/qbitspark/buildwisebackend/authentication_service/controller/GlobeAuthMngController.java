package com.qbitspark.buildwisebackend.authentication_service.controller;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.qbitspark.buildwisebackend.authentication_service.Repository.AccountRepo;
import com.qbitspark.buildwisebackend.authentication_service.Service.TempTokenService;
import com.qbitspark.buildwisebackend.authentication_service.entity.Roles;
import com.qbitspark.buildwisebackend.authentication_service.enums.TempTokenPurpose;
import com.qbitspark.buildwisebackend.authentication_service.enums.VerificationChannels;
import com.qbitspark.buildwisebackend.authentication_service.payloads.*;
import com.qbitspark.buildwisebackend.globeadvice.exceptions.*;
import com.qbitspark.buildwisebackend.authentication_service.entity.AccountEntity;
import com.qbitspark.buildwisebackend.authentication_service.Service.AccountService;
import com.qbitspark.buildwisebackend.globeresponsebody.GlobeSuccessResponseBuilder;
import com.qbitspark.buildwisebackend.globesecurity.JWTProvider;
import io.jsonwebtoken.Claims;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@AllArgsConstructor
@RestController
@RequestMapping("api/v1/auth")
public class GlobeAuthMngController {

    private final AccountService accountService;
    private final TempTokenService tempTokenService;
    private final JWTProvider tokenProvider;
    private final AccountRepo accountRepo;

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

        return ResponseEntity.ok(GlobeSuccessResponseBuilder.success(
                "Registration successful",
                registrationResponse
        ));
    }


    @PostMapping("/verify-otp")
    public ResponseEntity<GlobeSuccessResponseBuilder> verifyRegistrationOTP(
            @Valid @RequestBody VerifyRegistrationOTPRequest request)
            throws VerificationException, ItemNotFoundException, RandomExceptions {

        AccountEntity account = tempTokenService.validateTempTokenAndOTP(
                request.getTempToken(),
                request.getOtpCode()
        );

        Authentication authentication = new UsernamePasswordAuthenticationToken(
                account.getUserName(),
                null,
                mapRolesToAuthorities(account.getRoles())
        );

        String accessToken = tokenProvider.generateAccessToken(authentication);
        String refreshToken = tokenProvider.generateRefreshToken(authentication);

        // Create a login response with tokens and user data
        VerifiedAccountResponse verifiedAccountResponse = new VerifiedAccountResponse();
        verifiedAccountResponse.setAccessToken(accessToken);
        verifiedAccountResponse.setRefreshToken(refreshToken);
        
        AccountResponse accountResponse = new AccountResponse();
        accountResponse.setFirstName(account.getFirstName());
        accountResponse.setId(account.getId());
        accountResponse.setUserName(account.getUserName());
        accountResponse.setLastName(account.getLastName());
        accountResponse.setMiddleName(account.getMiddleName());
        accountResponse.setEmail(account.getEmail());
        accountResponse.setIsVerified(account.getIsVerified());
        accountResponse.setIsEmailVerified(account.getIsEmailVerified());
        accountResponse.setIsPhoneVerified(account.getIsPhoneVerified());
        accountResponse.setRoles(account.getRoles().stream()
                .map(Roles::getRoleName)
                .collect(Collectors.toSet()));
        accountResponse.setCreatedAt(account.getCreatedAt());
        accountResponse.setEditedAt(account.getEditedAt());

        verifiedAccountResponse.setUserData(accountResponse);

        // Build success response
        GlobeSuccessResponseBuilder response = GlobeSuccessResponseBuilder.success(
                "Registration completed successfully. You are now logged in.",
                verifiedAccountResponse
        );

        return ResponseEntity.ok(response);
    }


    @PostMapping("/resend-otp")
    public ResponseEntity<GlobeSuccessResponseBuilder> resendOTP(
            @Valid @RequestBody ResendOTPRequest request)
            throws VerificationException, ItemNotFoundException, RandomExceptions {

        // Validate and get user info from temp token
        Claims claims = tokenProvider.getTempTokenClaims(request.getTempToken());
        String userIdentifier = claims.get("userIdentifier", String.class);
        String purpose = claims.get("purpose", String.class);
        TempTokenPurpose tokenPurpose = TempTokenPurpose.valueOf(purpose);

        // Check if resend is allowed
        if (!tempTokenService.canResendOTP(userIdentifier, tokenPurpose)) {
            throw new RandomExceptions("Resend limit exceeded. Please wait before requesting again.");
        }

        // Resend OTP and get new temp token
        String newTempToken = tempTokenService.resendOTP(request.getTempToken());

        // Build response with security info
        ResendOTPResponse resendResponse = new ResendOTPResponse(
                newTempToken,
                "OTP has been resent successfully",
                LocalDateTime.now().plusMinutes(10),
                tempTokenService.getRemainingResendAttempts(userIdentifier, tokenPurpose),
                tempTokenService.getNextResendAllowedTime(userIdentifier, tokenPurpose)
        );

        GlobeSuccessResponseBuilder response = GlobeSuccessResponseBuilder.success(
                "OTP resent successfully",
                resendResponse
        );

        return ResponseEntity.ok(response);
    }


    @PostMapping("/psw-reset-otp")
    public ResponseEntity<GlobeSuccessResponseBuilder> requestOTP(@Valid @RequestBody EmailPasswordResetRequest requestOTPBody) throws RandomExceptions, JsonProcessingException, ItemReadyExistException, ItemNotFoundException {

        String newTempToken = tempTokenService.sendPSWDResetOTP(requestOTPBody.getEmail());



        GlobeSuccessResponseBuilder response = GlobeSuccessResponseBuilder.success(
                "OTP resent successfully",
                resendResponse
        );

        return ResponseEntity.ok(response);
    }



    @PostMapping("/login")
    public ResponseEntity<GlobeSuccessResponseBuilder> accountLogin(@Valid @RequestBody AccountLoginRequest accountLoginRequest) throws Exception {

        String tempToken = accountService.loginAccount(accountLoginRequest);

        RegistrationResponse registrationResponse = new RegistrationResponse(
                tempToken,
                "OTP has been sent to your " + getChannelName(VerificationChannels.EMAIL),
                LocalDateTime.now().plusMinutes(10)
        );

        return ResponseEntity.ok(GlobeSuccessResponseBuilder.success(
                "Login successful",
                registrationResponse
        ));
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

    private Collection<? extends GrantedAuthority> mapRolesToAuthorities(Set<Roles> roles) {
        return roles.stream()
                .map(role -> new SimpleGrantedAuthority(role.getRoleName()))
                .collect(Collectors.toList());
    }
}