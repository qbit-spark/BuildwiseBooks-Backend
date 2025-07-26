package com.qbitspark.buildwisebackend.authentication_service.Service;


import com.qbitspark.buildwisebackend.globeadvice.exceptions.ItemNotFoundException;
import com.qbitspark.buildwisebackend.globeadvice.exceptions.RandomExceptions;
import com.qbitspark.buildwisebackend.globeadvice.exceptions.VerificationException;
import com.qbitspark.buildwisebackend.authentication_service.entity.AccountEntity;
import com.qbitspark.buildwisebackend.globeresponsebody.GlobeSuccessResponseBuilder;

public interface EmailOTPService {
    void generateAndSendEmailOTP(AccountEntity userAuthEntity, String emailHeader, String instructionText) throws RandomExceptions, ItemNotFoundException;

    void sendRegistrationOTP(String email, String otpCode, String firstName, String emailHeader, String instructionText) throws RandomExceptions, ItemNotFoundException;

    void generateAndSendPasswordResetEmail(AccountEntity userAuthEntity, String emailHeader, String instructionText) throws RandomExceptions, ItemNotFoundException;

}
