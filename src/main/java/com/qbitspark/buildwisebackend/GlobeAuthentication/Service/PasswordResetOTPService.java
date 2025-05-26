package com.qbitspark.buildwisebackend.GlobeAuthentication.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.qbitspark.buildwisebackend.GlobeAdvice.Exceptions.ItemNotFoundException;
import com.qbitspark.buildwisebackend.GlobeAdvice.Exceptions.ItemReadyExistException;
import com.qbitspark.buildwisebackend.GlobeAdvice.Exceptions.RandomExceptions;
import com.qbitspark.buildwisebackend.GlobeAdvice.Exceptions.VerificationException;
import com.qbitspark.buildwisebackend.GlobeResponseBody.GlobalJsonResponseBody;

public interface PasswordResetOTPService {
    String generateAndSendPSWDResetOTP(String email) throws ItemReadyExistException, RandomExceptions, JsonProcessingException, ItemNotFoundException;
    GlobalJsonResponseBody verifyOTPAndResetPassword(String email, String otpCode, String newPassword) throws ItemReadyExistException, RandomExceptions, ItemNotFoundException, VerificationException;
}
