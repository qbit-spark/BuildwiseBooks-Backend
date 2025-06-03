package com.qbitspark.buildwisebackend.organisation_service.orgnisation_members_mng.payloads;


import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class InviteMemberRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "Valid email address is required")
    private String email;

    @NotBlank(message = "Role is required")
    @Pattern(
            regexp = "^(ADMIN|MEMBER)$",
            message = "Role must be either ADMIN or MEMBER"
    )
    private String role;
}