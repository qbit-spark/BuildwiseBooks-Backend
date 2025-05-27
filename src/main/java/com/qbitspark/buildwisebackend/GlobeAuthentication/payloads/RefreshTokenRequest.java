package com.qbitspark.buildwisebackend.GlobeAuthentication.payloads;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RefreshTokenRequest {
    @NotBlank(message = "Refresh token should not be empty")
    String refreshToken;
}
