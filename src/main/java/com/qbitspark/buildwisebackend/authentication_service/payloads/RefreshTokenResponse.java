package com.qbitspark.buildwisebackend.authentication_service.payloads;

import lombok.Data;

@Data
public class RefreshTokenResponse {
    String newToken;
}
