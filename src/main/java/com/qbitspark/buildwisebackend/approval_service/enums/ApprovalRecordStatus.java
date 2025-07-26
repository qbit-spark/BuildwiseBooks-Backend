package com.qbitspark.buildwisebackend.approval_service.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum ApprovalRecordStatus {
    ACTIVE("ACTIVE"),
    SUPERSEDED("SUPERSEDED");

    private final String value;

    ApprovalRecordStatus(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static ApprovalRecordStatus fromString(String status) {
        if (status == null || status.trim().isEmpty()) {
            return ACTIVE;
        }

        String normalizedStatus = status.trim().toUpperCase();

        for (ApprovalRecordStatus recordStatus : ApprovalRecordStatus.values()) {
            if (recordStatus.value.equals(normalizedStatus) ||
                    recordStatus.name().equals(normalizedStatus)) {
                return recordStatus;
            }
        }

        System.err.println("Unknown ApprovalRecordStatus: '" + status + "', defaulting to ACTIVE");
        return ACTIVE;
    }

    @Override
    public String toString() {
        return value;
    }
}