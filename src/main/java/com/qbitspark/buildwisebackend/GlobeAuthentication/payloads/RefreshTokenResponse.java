package com.qbitspark.buildwisebackend.GlobeAuthentication.payloads;

import lombok.Data;

@Data
public class RefreshTokenResponse {
    String newToken;
}
