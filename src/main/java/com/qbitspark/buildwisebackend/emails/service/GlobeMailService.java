package com.qbitspark.buildwisebackend.emails.service;

public interface GlobeMailService {
    boolean sendOTPEmail(String email, String otp, String userName) throws Exception;
}
