package com.qbitspark.buildwisebackend.vendormng_service.payloads;

import com.qbitspark.buildwisebackend.vendormng_service.enums.VendorType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
public class CreateVendorRequest {

    @NotBlank(message = "Vendor name is required")
    @Size(max = 100, message = "Name must be less than 100 characters")
    private String name;

    @NotBlank(message = "Description is required")
    private String description;

    @NotBlank(message = "Address is required")
    private String address;

    @NotBlank(message = "Office phone is required")
    private String officePhone;

    @NotBlank(message = "TIN is required")
    @Size(max = 50, message = "TIN must be less than 50 characters")
    private String tin;

    @Email(message = "Invalid email format")
    @NotBlank(message = "Email is required")
    private String email;

    @NotNull(message = "Vendor type is required")
    private VendorType vendorType;

    @Valid
    private BankDetails bankDetails;

    private List<UUID> attachmentIds = new ArrayList<>();
}