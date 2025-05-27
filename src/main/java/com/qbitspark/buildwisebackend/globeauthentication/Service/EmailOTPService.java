package com.qbitspark.buildwisebackend.globeauthentication.Service;


import com.qbitspark.buildwisebackend.globeadvice.exceptions.ItemNotFoundException;
import com.qbitspark.buildwisebackend.globeadvice.exceptions.RandomExceptions;
import com.qbitspark.buildwisebackend.globeadvice.exceptions.VerificationException;
import com.qbitspark.buildwisebackend.globeauthentication.entity.AccountEntity;
import com.qbitspark.buildwisebackend.globeresponsebody.GlobeSuccessResponseBuilder;

public interface EmailOTPService {
    void generateAndSendEmailOTP(AccountEntity userAuthEntity, String emailHeader, String instructionText) throws RandomExceptions, ItemNotFoundException;

    void generateAndSendPasswordResetEmail(AccountEntity userAuthEntity, String emailHeader, String instructionText) throws RandomExceptions, ItemNotFoundException;

    //Only this method should return GlobalJsonResponseBody direct from the service
    GlobeSuccessResponseBuilder verifyEmailOTP(String email, String otpCode) throws RandomExceptions, VerificationException, ItemNotFoundException;
}
