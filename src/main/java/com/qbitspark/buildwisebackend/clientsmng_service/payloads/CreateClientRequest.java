package com.qbitspark.buildwisebackend.clientsmng_service.payloads;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CreateClientRequest {

    @NotBlank(message = "Client name is required")
    @Size(max = 100, message = "Name must be less than 100 characters")
    private String name;

    @NotBlank(message = "The description is required")
    private String description;

    @NotBlank(message = "Address is required")
    private String address;

    @NotBlank(message = "Office phone is required")
    private String officePhone;

    @NotBlank(message = "The TIN is required")
    @Size(max = 50, message = "The tin must be less than 50 characters")
    private String tin;

    @Email(message = "Invalid email format")
    @NotBlank(message = "Email is required")
    private String email;

}
