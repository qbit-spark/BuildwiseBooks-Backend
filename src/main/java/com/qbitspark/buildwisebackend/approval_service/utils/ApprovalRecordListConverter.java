package com.qbitspark.buildwisebackend.approval_service.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.qbitspark.buildwisebackend.approval_service.entities.embedings.ApprovalRecord;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

@Converter
@Slf4j
public class ApprovalRecordListConverter implements AttributeConverter<List<ApprovalRecord>, String> {

    private final ObjectMapper objectMapper;

    public ApprovalRecordListConverter() {
        this.objectMapper = new ObjectMapper();

        // Configure for enum and LocalDateTime support
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        // Enum configuration
        this.objectMapper.enable(SerializationFeature.WRITE_ENUMS_USING_TO_STRING);
        this.objectMapper.enable(DeserializationFeature.READ_ENUMS_USING_TO_STRING);

        // Ignore unknown properties for backward compatibility
        this.objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    }

    @Override
    public String convertToDatabaseColumn(List<ApprovalRecord> attribute) {
        if (attribute == null || attribute.isEmpty()) {
            return "[]";
        }

        try {
            String json = objectMapper.writeValueAsString(attribute);
            log.debug("Converting ApprovalRecord list to JSON: {}", json);
            return json;
        } catch (JsonProcessingException e) {
            log.error("Error converting ApprovalRecord list to JSON", e);
            return "[]";
        }
    }

    @Override
    public List<ApprovalRecord> convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.trim().isEmpty() || "null".equals(dbData)) {
            return new ArrayList<>();
        }

        try {
            List<ApprovalRecord> result = objectMapper.readValue(dbData, new TypeReference<List<ApprovalRecord>>() {});
            log.debug("Converting JSON to ApprovalRecord list: {} records", result != null ? result.size() : 0);
            return result != null ? result : new ArrayList<>();
        } catch (JsonProcessingException e) {
            log.error("Error converting JSON to ApprovalRecord list: {}", dbData, e);
            return new ArrayList<>();
        }
    }
}