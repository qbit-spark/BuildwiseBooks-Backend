package com.qbitspark.buildwisebackend.vendormng_service.payloads;

import com.qbitspark.buildwisebackend.vendormng_service.enums.VendorType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateVendorRequest {

    @Size(max = 100, message = "Name must be less than 100 characters")
    private String name;

    private String description;

    private String address;

    private String officePhone;

    @Size(max = 50, message = "The tin must be less than 50 characters")
    private String tin;

    @Email(message = "Invalid email format")
    private String email;

    @Valid
    private BankDetails bankDetails;

    private VendorType vendorType;

    private Boolean isActive;
}