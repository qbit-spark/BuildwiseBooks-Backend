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

import static com.qbitspark.buildwisebackend.authentication_service.enums.VerificationChannels.WHATSAPP;

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


    @PostMapping("/resend-verification-otp")
    public ResponseEntity<GlobeSuccessResponseBuilder> resendVerificationOTPByEmail(
            @Valid @RequestBody ResendOTPByEmailRequest request)
            throws VerificationException, ItemNotFoundException, RandomExceptions {

        // Validate rate limiting
        if (!tempTokenService.canResendByEmail(request.getEmail(), request.getPurpose())) {
            throw new RandomExceptions("Too many attempts. Please wait before trying again.");
        }

        // Generate a new token and send OTP
        String newTempToken = tempTokenService.resendOTPByEmail(
                request.getEmail(),
                request.getPurpose()
        );

        // Build response
        ResendOTPResponse response = new ResendOTPResponse(
                newTempToken,
                "Verification code sent to your email",
                LocalDateTime.now().plusMinutes(10),
                tempTokenService.getRemainingResendAttempts(request.getEmail(), request.getPurpose()),
                tempTokenService.getNextResendAllowedTime(request.getEmail(), request.getPurpose())
        );

        GlobeSuccessResponseBuilder successResponse = GlobeSuccessResponseBuilder.success(
                "OTP sent successfully",
                response
        );

        return ResponseEntity.ok(successResponse);
    }

    // Convenience endpoint specifically for registration recovery
    @PostMapping("/continue-registration")
    public ResponseEntity<GlobeSuccessResponseBuilder> continueRegistration(
            @Valid @RequestBody RequestEmailOTPBody request)
            throws VerificationException, ItemNotFoundException, RandomExceptions {

        // Check account status first
        AccountEntity account = accountRepo.findByEmail(request.getEmail()).orElse(null);

        if (account == null) {
            throw new ItemNotFoundException("No registration found. Please start registration process.");
        }

        if (account.getIsVerified()) {
            // Account already verified - redirect to log in
            GlobeSuccessResponseBuilder response = GlobeSuccessResponseBuilder.success(
                    "Account already verified. Please login.",
                    Map.of(
                            "action", "LOGIN",
                            "message", "Your account is ready! You can login now."
                    )
            );
            return ResponseEntity.ok(response);
        }

        // Continue the registration flow
        String newTempToken = tempTokenService.resendOTPByEmail(
                request.getEmail(),
                TempTokenPurpose.REGISTRATION_OTP
        );

        RegistrationResponse registrationResponse = new RegistrationResponse(
                newTempToken,
                "Welcome back! Please check your email for the verification code.",
                LocalDateTime.now().plusMinutes(10)
        );

        GlobeSuccessResponseBuilder response = GlobeSuccessResponseBuilder.success(
                "Registration continuation sent",
                registrationResponse
        );

        return ResponseEntity.ok(response);
    }

    // Account status checker - helps frontend decide what to show
    @PostMapping("/account-status")
    public ResponseEntity<GlobeSuccessResponseBuilder> checkAccountStatus(
            @Valid @RequestBody RequestEmailOTPBody request) {

        String email = request.getEmail();
        AccountEntity account = accountRepo.findByEmail(email).orElse(null);

        Map<String, Object> status = new HashMap<>();

        if (account == null) {
            status.put("exists", false);
            status.put("verified", false);
            status.put("action", "REGISTER");
            status.put("message", "Ready to create your account!");

        } else if (!account.getIsVerified()) {
            status.put("exists", true);
            status.put("verified", false);
            status.put("action", "CONTINUE_REGISTRATION");
            status.put("message", "Let's complete your registration");
            status.put("registeredAt", account.getCreatedAt());
            status.put("canResend", tempTokenService.canResendByEmail(email, TempTokenPurpose.REGISTRATION_OTP));
            status.put("firstName", account.getFirstName());

        } else {
            status.put("exists", true);
            status.put("verified", true);
            status.put("action", "LOGIN");
            status.put("message", "Welcome back! You can login now.");
            status.put("firstName", account.getFirstName());
        }

        GlobeSuccessResponseBuilder response = GlobeSuccessResponseBuilder.success(
                "Account status retrieved",
                status
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