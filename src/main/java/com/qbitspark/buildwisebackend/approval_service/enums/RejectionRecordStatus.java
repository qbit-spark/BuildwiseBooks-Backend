package com.qbitspark.buildwisebackend.approval_service.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum RejectionRecordStatus {
    ACTIVE("ACTIVE"),
    RESOLVED("RESOLVED");

    private final String value;

    RejectionRecordStatus(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static RejectionRecordStatus fromString(String status) {
        if (status == null || status.trim().isEmpty()) {
            return ACTIVE;
        }

        String normalizedStatus = status.trim().toUpperCase();

        for (RejectionRecordStatus recordStatus : RejectionRecordStatus.values()) {
            if (recordStatus.value.equals(normalizedStatus) ||
                    recordStatus.name().equals(normalizedStatus)) {
                return recordStatus;
            }
        }

        System.err.println("Unknown RejectionRecordStatus: '" + status + "', defaulting to ACTIVE");
        return ACTIVE;
    }

    @Override
    public String toString() {
        return value;
    }
}