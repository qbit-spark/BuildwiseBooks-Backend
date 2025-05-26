package com.qbitspark.buildwisebackend.GlobeAuthentication.Service;


import com.qbitspark.buildwisebackend.GlobeAdvice.Exceptions.ItemNotFoundException;
import com.qbitspark.buildwisebackend.GlobeAdvice.Exceptions.RandomExceptions;
import com.qbitspark.buildwisebackend.GlobeAdvice.Exceptions.VerificationException;
import com.qbitspark.buildwisebackend.GlobeAuthentication.Entity.GlobeUserEntity;
import com.qbitspark.buildwisebackend.GlobeResponseBody.GlobalJsonResponseBody;

public interface EmailOTPService {
    void generateAndSendEmailOTP(GlobeUserEntity userAuthEntity, String emailHeader, String instructionText) throws RandomExceptions, ItemNotFoundException;

    //Only this method should return GlobalJsonResponseBody direct from the service
    GlobalJsonResponseBody verifyEmailOTP(String email, String otpCode) throws RandomExceptions, VerificationException, ItemNotFoundException;
}
