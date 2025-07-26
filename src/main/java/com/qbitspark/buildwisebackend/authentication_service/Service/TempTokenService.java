package com.qbitspark.buildwisebackend.authentication_service.Service;

import com.qbitspark.buildwisebackend.authentication_service.entity.AccountEntity;
import com.qbitspark.buildwisebackend.authentication_service.enums.TempTokenPurpose;
import com.qbitspark.buildwisebackend.globeadvice.exceptions.ItemNotFoundException;
import com.qbitspark.buildwisebackend.globeadvice.exceptions.RandomExceptions;
import com.qbitspark.buildwisebackend.globeadvice.exceptions.VerificationException;

import java.time.LocalDateTime;

public interface TempTokenService {

    String createTempToken(AccountEntity account, TempTokenPurpose purpose, String identifier, String otpCode) throws RandomExceptions;


    String resendOTP(String tempToken) throws VerificationException, ItemNotFoundException, RandomExceptions;


    AccountEntity validateTempTokenAndOTP(String tempToken, String otpCode) throws VerificationException, ItemNotFoundException, RandomExceptions;


    void invalidateAllTokensForPurpose(AccountEntity account, TempTokenPurpose purpose);

    void cleanupExpiredTokens();

    boolean isWithinRateLimit(AccountEntity account, TempTokenPurpose purpose);

    LocalDateTime getNextResendAllowedTime(String userIdentifier, TempTokenPurpose purpose);

    int getRemainingResendAttempts(String userIdentifier, TempTokenPurpose purpose);

    boolean canResendOTP(String userIdentifier, TempTokenPurpose purpose);

    boolean canResendByEmail(String email, TempTokenPurpose purpose);
}