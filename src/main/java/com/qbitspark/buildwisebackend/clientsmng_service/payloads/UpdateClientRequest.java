package com.qbitspark.buildwisebackend.clientsmng_service.payloads;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UpdateClientRequest {

    @Size(max = 100, message = "Name must be less than 100 characters")
    @NotBlank(message = "The name is required")
    private String name;

    @NotBlank(message = "The description is required")
    private String description;

    @NotBlank(message = "The address is required")
    private String address;

    @NotBlank(message = "The phone number is required")
    private String officePhone;

    @Size(max = 50, message = "The tin must be less than 50 characters")
    @NotBlank(message = "The TIN number is required")
    private String tin;

    @Email(message = "Invalid email format")
    @NotBlank(message = "The email is required")
    private String email;

    private Boolean isActive;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
