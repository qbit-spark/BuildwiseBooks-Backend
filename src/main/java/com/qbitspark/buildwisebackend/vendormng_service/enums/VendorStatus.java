package com.qbitspark.buildwisebackend.vendormng_service.enums;

import lombok.Getter;

@Getter
public enum VendorStatus {
    ACTIVE,
    INACTIVE,
    SUSPENDED,
    BLACKLISTED,
    PENDING_APPROVAL,
    APPROVED,
}
