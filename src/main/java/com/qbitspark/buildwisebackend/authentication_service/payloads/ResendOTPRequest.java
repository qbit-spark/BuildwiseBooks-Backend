package com.qbitspark.buildwisebackend.authentication_service.payloads;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ResendOTPRequest {

    @NotBlank(message = "Temp token is mandatory")
    private String tempToken;
}