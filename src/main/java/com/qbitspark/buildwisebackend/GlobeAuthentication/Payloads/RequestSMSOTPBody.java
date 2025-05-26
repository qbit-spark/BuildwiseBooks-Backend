package com.qbitspark.buildwisebackend.GlobeAuthentication.Payloads;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RequestSMSOTPBody {
    @NotBlank(message = "Email is mandatory")
    @Email(message = "Email must be valid")
    private String email;
}
