package com.qbitspark.buildwisebackend.globeauthentication.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.qbitspark.buildwisebackend.globeadvice.exceptions.ItemNotFoundException;
import com.qbitspark.buildwisebackend.globeadvice.exceptions.ItemReadyExistException;
import com.qbitspark.buildwisebackend.globeadvice.exceptions.RandomExceptions;
import com.qbitspark.buildwisebackend.globeadvice.exceptions.VerificationException;

public interface PasswordResetOTPService {
    String generateAndSendPSWDResetOTP(String email) throws ItemReadyExistException, RandomExceptions, JsonProcessingException, ItemNotFoundException;
    boolean verifyOTPAndResetPassword(String email, String otpCode, String newPassword) throws ItemReadyExistException, RandomExceptions, ItemNotFoundException, VerificationException;
}
