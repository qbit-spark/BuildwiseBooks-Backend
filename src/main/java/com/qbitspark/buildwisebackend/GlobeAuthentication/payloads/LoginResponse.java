package com.qbitspark.buildwisebackend.GlobeAuthentication.payloads;

import lombok.Data;

@Data
public class LoginResponse {
    private Object userData;
    private String accessToken;
    private String refreshToken;
}
