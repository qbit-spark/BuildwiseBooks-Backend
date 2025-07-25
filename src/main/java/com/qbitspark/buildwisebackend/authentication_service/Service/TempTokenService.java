package com.qbitspark.buildwisebackend.authentication_service.Service;

import com.qbitspark.buildwisebackend.authentication_service.entity.AccountEntity;
import com.qbitspark.buildwisebackend.authentication_service.enums.TempTokenPurpose;
import com.qbitspark.buildwisebackend.globeadvice.exceptions.ItemNotFoundException;
import com.qbitspark.buildwisebackend.globeadvice.exceptions.RandomExceptions;
import com.qbitspark.buildwisebackend.globeadvice.exceptions.VerificationException;

public interface TempTokenService {

    /**
     * Creates a temporary token for OTP verification
     * @param account The user account (can be null for registration)
     * @param purpose Purpose of the token (registration, login, password reset)
     * @param identifier The identifier used (email, phone, username)
     * @param otpCode The OTP code that will be sent
     * @return JWT temporary token string
     */
    String createTempToken(AccountEntity account, TempTokenPurpose purpose, String identifier, String otpCode) throws RandomExceptions;

    /**
     * Creates a temporary token for registration (when no account exists yet)
     * @param userIdentifier The user identifier (email)
     * @param purpose Purpose of the token (usually REGISTRATION_OTP)
     * @param otpCode The OTP code that will be sent
     * @return JWT temporary token string
     */
    default String createRegistrationTempToken(String userIdentifier, TempTokenPurpose purpose, String otpCode) throws RandomExceptions {
        return createTempToken(null, purpose, userIdentifier, otpCode);
    }

    /**
     * Validates temporary token and OTP
     * @param tempToken JWT temporary token
     * @param otpCode OTP code provided by user
     * @return The account entity if validation successful
     */
    AccountEntity validateTempTokenAndOTP(String tempToken, String otpCode) throws VerificationException, ItemNotFoundException, RandomExceptions;

    /**
     * Invalidates all temp tokens for a user and purpose
     * @param account The user account
     * @param purpose The token purpose
     */
    void invalidateAllTokensForPurpose(AccountEntity account, TempTokenPurpose purpose);

    /**
     * Cleanup expired tokens (scheduled job)
     */
    void cleanupExpiredTokens();

    /**
     * Check rate limiting for temp token generation
     * @param account The user account
     * @param purpose The token purpose
     * @return true if within rate limits
     */
    boolean isWithinRateLimit(AccountEntity account, TempTokenPurpose purpose);
}