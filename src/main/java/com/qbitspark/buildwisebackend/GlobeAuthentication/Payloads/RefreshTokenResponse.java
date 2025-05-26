package com.qbitspark.buildwisebackend.GlobeAuthentication.Payloads;

import lombok.Data;

@Data
public class RefreshTokenResponse {
    String newToken;
}
