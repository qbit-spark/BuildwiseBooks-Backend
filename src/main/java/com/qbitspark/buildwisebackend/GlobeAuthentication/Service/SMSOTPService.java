package com.qbitspark.buildwisebackend.GlobeAuthentication.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.qbitspark.buildwisebackend.GlobeAdvice.Exceptions.ItemNotFoundException;
import com.qbitspark.buildwisebackend.GlobeAdvice.Exceptions.RandomExceptions;
import com.qbitspark.buildwisebackend.GlobeAdvice.Exceptions.VerificationException;
import com.qbitspark.buildwisebackend.GlobeResponseBody.GlobalJsonResponseBody;


public interface SMSOTPService {
    String generateAndSendSMSOTP(String phoneNumber) throws RandomExceptions, JsonProcessingException, ItemNotFoundException;
    GlobalJsonResponseBody verifySMSOTP(String phoneNumber, String otpCode) throws ItemNotFoundException, RandomExceptions, VerificationException, VerificationException;

}
