package com.qbitspark.buildwisebackend.authentication_service.Service.IMPL;

import com.qbitspark.buildwisebackend.authentication_service.Repository.TempTokenRepository;
import com.qbitspark.buildwisebackend.authentication_service.Service.TempTokenService;
import com.qbitspark.buildwisebackend.authentication_service.entity.AccountEntity;
import com.qbitspark.buildwisebackend.authentication_service.entity.TempTokenEntity;
import com.qbitspark.buildwisebackend.authentication_service.enums.TempTokenPurpose;
import com.qbitspark.buildwisebackend.globeadvice.exceptions.ItemNotFoundException;
import com.qbitspark.buildwisebackend.globeadvice.exceptions.RandomExceptions;
import com.qbitspark.buildwisebackend.globeadvice.exceptions.VerificationException;
import com.qbitspark.buildwisebackend.globesecurity.JWTProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class TempTokenServiceIMPL implements TempTokenService {

    private final TempTokenRepository tempTokenRepository;
    private final JWTProvider jwtProvider;
    private final PasswordEncoder passwordEncoder;

    @Value("${temp.token.expiry.minutes:10}")
    private int tempTokenExpiryMinutes;

    @Value("${temp.token.rate.limit.count:3}")
    private int rateLimitCount;

    @Value("${temp.token.rate.limit.window.minutes:15}")
    private int rateLimitWindowMinutes;

    @Override
    @Transactional
    public String createTempToken(AccountEntity account, TempTokenPurpose purpose, String identifier, String otpCode) throws RandomExceptions {

        // For registration, account will be null
        // For login/password reset, account will be provided
        String userIdentifier = (account != null) ? account.getEmail() : identifier;

        // Check rate limiting
        if (!isWithinRateLimit(account, userIdentifier, purpose)) {
            throw new RandomExceptions("Too many OTP requests. Please wait before requesting again.");
        }

        // Invalidate any existing active tokens for the same purpose
        invalidateAllTokensForPurpose(account, userIdentifier, purpose);

        // Create a JWT payload
        Map<String, Object> claims = new HashMap<>();
        claims.put("userIdentifier", userIdentifier);
        claims.put("purpose", purpose.name());
        claims.put("identifier", identifier);
        if (account != null) {
            claims.put("userId", account.getId().toString());
        }
        claims.put("exp", System.currentTimeMillis() + ((long) tempTokenExpiryMinutes * 60 * 1000));

        // Generate JWT token
        String jwtToken = jwtProvider.generateTempToken(claims);

        // Hash the token for database storage
        String tokenHash = hashString(jwtToken);

        // Hash the OTP for secure storage
        String otpHash = passwordEncoder.encode(otpCode);

        // Create temp token entity
        TempTokenEntity tempToken = new TempTokenEntity();
        tempToken.setTokenHash(tokenHash);
        tempToken.setPurpose(purpose);
        tempToken.setIdentifier(identifier);
        tempToken.setUserIdentifier(userIdentifier);  // ✅ Always store for auditing
        tempToken.setOtpHash(otpHash);
        tempToken.setAccount(account);  // ✅ Can be null for registration
        tempToken.setCreatedAt(LocalDateTime.now());
        tempToken.setExpiresAt(LocalDateTime.now().plusMinutes(tempTokenExpiryMinutes));

        tempTokenRepository.save(tempToken);

        return jwtToken;
    }

    @Override
    @Transactional
    public AccountEntity validateTempTokenAndOTP(String tempToken, String otpCode) throws VerificationException, ItemNotFoundException, RandomExceptions {

        // Hash the provided token to find it in a database
        String tokenHash = hashString(tempToken);

        // Find the temp token
        TempTokenEntity tempTokenEntity = tempTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new VerificationException("Invalid or expired temporary token"));

        // Check if a token is already used
        if (tempTokenEntity.getIsUsed()) {
            throw new VerificationException("Token has already been used");
        }

        // Check if the token is expired
        if (tempTokenEntity.isExpired()) {
            throw new VerificationException("Token has expired");
        }

        // Check if max attempts reached
        if (tempTokenEntity.isMaxAttemptsReached()) {
            throw new VerificationException("Maximum verification attempts exceeded");
        }

        // Verify OTP
        if (!passwordEncoder.matches(otpCode, tempTokenEntity.getOtpHash())) {
            // Increment failed attempts
            tempTokenEntity.incrementAttempts();
            tempTokenRepository.save(tempTokenEntity);
            throw new VerificationException("Invalid OTP code");
        }

        // Mark token as used
        tempTokenEntity.markAsUsed();
        tempTokenRepository.save(tempTokenEntity);

        AccountEntity account = tempTokenEntity.getAccount();

        //Todo: Take action based in purpose of token
        switch (tempTokenEntity.getPurpose()) {

            case REGISTRATION_OTP -> actAfterRegistrationOtpValid(account);
            case LOGIN_OTP -> actAfterLoginOtpValid();

        }

        return account;
    }

    @Override
    @Transactional
    public void invalidateAllTokensForPurpose(AccountEntity account, TempTokenPurpose purpose) {
        invalidateAllTokensForPurpose(account, null, purpose);
    }

    // Overloaded method to handle both account and userIdentifier
    @Transactional
    public void invalidateAllTokensForPurpose(AccountEntity account, String userIdentifier, TempTokenPurpose purpose) {
        List<TempTokenEntity> activeTokens;

        if (account != null) {
            // For login/password reset - find by account
            activeTokens = tempTokenRepository.findByAccountAndPurposeAndIsUsed(account, purpose, false);
        } else {
            // For registration - find by userIdentifier
            activeTokens = tempTokenRepository.findByUserIdentifierAndPurposeAndIsUsed(userIdentifier, purpose, false);
        }

        // Mark them all as used
        for (TempTokenEntity token : activeTokens) {
            token.markAsUsed();
        }

        if (!activeTokens.isEmpty()) {
            tempTokenRepository.saveAll(activeTokens);
        }
    }

    @Override
    @Transactional
    public void cleanupExpiredTokens() {
        LocalDateTime now = LocalDateTime.now();

        // Delete expired tokens
        List<TempTokenEntity> expiredTokens = tempTokenRepository.findByExpiresAtBefore(now);
        if (!expiredTokens.isEmpty()) {
            tempTokenRepository.deleteAll(expiredTokens);
        }

        // Delete old used tokens (older than 1 day)
        LocalDateTime cutoffTime = now.minusDays(1);
        List<TempTokenEntity> oldUsedTokens = tempTokenRepository
                .findByIsUsedAndCreatedAtBefore(true, cutoffTime);
        if (!oldUsedTokens.isEmpty()) {
            tempTokenRepository.deleteAll(oldUsedTokens);
        }
    }

    @Override
    public boolean isWithinRateLimit(AccountEntity account, TempTokenPurpose purpose) {
        String userIdentifier = (account != null) ? account.getEmail() : null;
        return isWithinRateLimit(account, userIdentifier, purpose);
    }

    // Overloaded method to handle both scenarios
    public boolean isWithinRateLimit(AccountEntity account, String userIdentifier, TempTokenPurpose purpose) {
        LocalDateTime windowStart = LocalDateTime.now().minusMinutes(rateLimitWindowMinutes);

        List<TempTokenEntity> recentTokens;

        if (account != null) {
            // For login/password reset - check by account
            recentTokens = tempTokenRepository.findByAccountAndPurposeAndCreatedAtAfter(account, purpose, windowStart);
        } else {
            // For registration - check by userIdentifier
            recentTokens = tempTokenRepository.findByUserIdentifierAndPurposeAndCreatedAtAfter(userIdentifier, purpose, windowStart);
        }

        return recentTokens.size() < rateLimitCount;
    }

    /**
     * Hash string using SHA-256
     */
    private String hashString(String input) throws RandomExceptions {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes());
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RandomExceptions("Error hashing token: " + e.getMessage());
        }
    }

    private void actAfterRegistrationOtpValid(AccountEntity account) {
    }

    private void actAfterLoginOtpValid() {
    }


}