package com.qbitspark.buildwisebackend.authentication_service.controller;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.qbitspark.buildwisebackend.authentication_service.Service.TempTokenService;
import com.qbitspark.buildwisebackend.authentication_service.entity.Roles;
import com.qbitspark.buildwisebackend.authentication_service.enums.VerificationChannels;
import com.qbitspark.buildwisebackend.authentication_service.payloads.*;
import com.qbitspark.buildwisebackend.globeadvice.exceptions.*;
import com.qbitspark.buildwisebackend.authentication_service.entity.AccountEntity;
import com.qbitspark.buildwisebackend.authentication_service.Service.AccountService;
import com.qbitspark.buildwisebackend.globeresponsebody.GlobeSuccessResponseBuilder;
import com.qbitspark.buildwisebackend.globesecurity.JWTProvider;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.qbitspark.buildwisebackend.authentication_service.enums.VerificationChannels.WHATSAPP;

@AllArgsConstructor
@RestController
@RequestMapping("api/v1/auth")
public class GlobeAuthMngController {

    private final AccountService accountService;
    private final TempTokenService tempTokenService;
    private final JWTProvider tokenProvider;

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


    @PostMapping("/verify-registration-otp")
    public ResponseEntity<GlobeSuccessResponseBuilder> verifyRegistrationOTP(
            @Valid @RequestBody VerifyRegistrationOTPRequest request)
            throws VerificationException, ItemNotFoundException, RandomExceptions {

        // Validate the temp token and OTP using the TempTokenService
        AccountEntity account = tempTokenService.validateTempTokenAndOTP(
                request.getTempToken(),
                request.getOtpCode()
        );

        // Generate access and refresh tokens for the newly verified user
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                account.getUserName(),
                null,
                mapRolesToAuthorities(account.getRoles())
        );

        String accessToken = tokenProvider.generateAccessToken(authentication);
        String refreshToken = tokenProvider.generateRefreshToken(authentication);

        // Create a login response with tokens and user data
        LoginResponse loginResponse = new LoginResponse();
        loginResponse.setAccessToken(accessToken);
        loginResponse.setRefreshToken(refreshToken);
        loginResponse.setUserData(account);

        // Build success response
        GlobeSuccessResponseBuilder response = GlobeSuccessResponseBuilder.success(
                "Registration completed successfully. You are now logged in.",
                loginResponse
        );

        return ResponseEntity.ok(response);
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

    private Collection<? extends GrantedAuthority> mapRolesToAuthorities(Set<Roles> roles) {
        return roles.stream()
                .map(role -> new SimpleGrantedAuthority(role.getRoleName()))
                .collect(Collectors.toList());
    }
}