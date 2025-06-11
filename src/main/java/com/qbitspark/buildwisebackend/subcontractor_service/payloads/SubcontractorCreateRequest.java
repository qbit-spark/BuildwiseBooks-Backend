package com.qbitspark.buildwisebackend.subcontractor_service.payloads;

import com.qbitspark.buildwisebackend.subcontractor_service.enums.SpecializationType;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class SubcontractorCreateRequest {

    @NotBlank(message = "Company name is required")
    @Size(min = 2, max = 100, message = "Company name must be between 2 and 100 characters")
    private String companyName;

    @NotBlank(message = "Email is required")
    @Email(message = "Please provide a valid email address")
    @Size(max = 100, message = "Email must not exceed 100 characters")
    private String email;

    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^[+]?[0-9\\s\\-()]{7,20}$", message = "Please provide a valid phone number")
    @Size(max = 20, message = "Phone number must not exceed 20 characters")
    private String phoneNumber;

    @NotBlank(message = "TIN is required")
    @Size(min = 9, max = 50, message = "TIN must be between 9 and 50 characters")
    @Pattern(regexp = "^[A-Z0-9\\-]+$", message = "TIN must contain only alphanumeric characters and hyphens")
    private String tin;

    @Size(max = 1000, message = "Address must not exceed 1000 characters")
    private String address;

    @NotBlank(message = "Registration number is required")
    @Size(min = 5, max = 50, message = "Registration number must be between 5 and 50 characters")
    private String registrationNumber;

    @NotEmpty(message = "At least one specialization is required")
    @Size(max = 10, message = "Maximum 10 specializations allowed")
    private List<@NotNull(message = "Specialization cannot be null") SpecializationType> specializations;

//    @NotNull(message = "Organisation ID is required")
    private UUID organisationId;

    private List<UUID> projectIds;
}