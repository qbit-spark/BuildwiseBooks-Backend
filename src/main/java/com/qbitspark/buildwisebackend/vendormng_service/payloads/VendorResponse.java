package com.qbitspark.buildwisebackend.vendormng_service.payloads;

import com.qbitspark.buildwisebackend.vendormng_service.enums.VendorStatus;
import com.qbitspark.buildwisebackend.vendormng_service.enums.VendorType;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
public class VendorResponse {
    private UUID vendorId;
    private String name;
    private String description;
    private String address;
    private String officePhone;
    private String tin;
    private String email;
    private VendorType vendorType;
    private VendorStatus status;
    private BankDetails bankDetails;
    private List<UUID> attachmentIds;

    // Organisation info
    private UUID organisationId;
    private String organisationName;

    // Timestamps
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}