package com.qbitspark.buildwisebackend.globeauthentication.payloads;

import lombok.Data;

@Data
public class RefreshTokenResponse {
    String newToken;
}
