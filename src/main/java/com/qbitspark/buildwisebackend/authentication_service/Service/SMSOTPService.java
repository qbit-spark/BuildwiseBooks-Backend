package com.qbitspark.buildwisebackend.authentication_service.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.qbitspark.buildwisebackend.globeadvice.exceptions.ItemNotFoundException;
import com.qbitspark.buildwisebackend.globeadvice.exceptions.RandomExceptions;
import com.qbitspark.buildwisebackend.globeadvice.exceptions.VerificationException;
import com.qbitspark.buildwisebackend.globeresponsebody.GlobeSuccessResponseBuilder;


public interface SMSOTPService {
    String generateAndSendSMSOTP(String phoneNumber) throws RandomExceptions, JsonProcessingException, ItemNotFoundException;
    GlobeSuccessResponseBuilder verifySMSOTP(String phoneNumber, String otpCode) throws ItemNotFoundException, RandomExceptions, VerificationException, VerificationException;

}
