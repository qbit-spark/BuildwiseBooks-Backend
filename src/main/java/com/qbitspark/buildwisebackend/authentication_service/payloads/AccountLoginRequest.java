package com.qbitspark.buildwisebackend.authentication_service.payloads;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

@Data
public class AccountLoginRequest {
    @NotEmpty(message = "User identifier(email/username/phone) should not be empty")
    private String identifier;
    @NotEmpty(message = "Password should not be empty")
    private String password;
}
