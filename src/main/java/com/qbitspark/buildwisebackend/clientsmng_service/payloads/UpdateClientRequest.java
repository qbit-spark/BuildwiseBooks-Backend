package com.qbitspark.buildwisebackend.clientsmng_service.payloads;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UpdateClientRequest {

    @Size(max = 100, message = "Name must be less than 100 characters")
    private String name;

    private String description;

    private String address;

    private String officePhone;

    @Size(max = 50, message = "The tin must be less than 50 characters")
    private String tin;

    @Email(message = "Invalid email format")
    private String email;

    private Boolean isActive;

}
