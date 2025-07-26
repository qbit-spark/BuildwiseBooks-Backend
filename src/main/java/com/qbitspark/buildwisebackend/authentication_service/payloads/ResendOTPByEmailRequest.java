package com.qbitspark.buildwisebackend.authentication_service.payloads;

import com.qbitspark.buildwisebackend.authentication_service.enums.TempTokenPurpose;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ResendOTPByEmailRequest {

    @NotBlank(message = "Email is mandatory")
    @Email(message = "Email should be valid")
    private String email;

    @NotNull(message = "Purpose is mandatory")
    private TempTokenPurpose purpose;

    private String additionalContext;
}