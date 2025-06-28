package com.qbitspark.buildwisebackend.vendormng_service.payloads;

import com.qbitspark.buildwisebackend.vendormng_service.enums.VendorType;
import lombok.Data;

import java.util.UUID;

@Data
public class VendorSummaryResponse {
    private UUID vendorId;
    private String name;
    private VendorType vendorType;
}
