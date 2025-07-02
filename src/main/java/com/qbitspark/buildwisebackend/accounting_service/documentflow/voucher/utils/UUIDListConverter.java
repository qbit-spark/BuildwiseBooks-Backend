package com.qbitspark.buildwisebackend.accounting_service.documentflow.voucher.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Converter
@Slf4j
public class UUIDListConverter implements AttributeConverter<List<UUID>, String> {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(List<UUID> uuidList) {
        if (uuidList == null || uuidList.isEmpty()) {
            return "[]";
        }

        try {
            return objectMapper.writeValueAsString(uuidList);
        } catch (JsonProcessingException e) {
            log.error("Error converting UUID list to JSON", e);
            return "[]";
        }
    }

    @Override
    public List<UUID> convertToEntityAttribute(String jsonString) {
        if (jsonString == null || jsonString.trim().isEmpty() || "[]".equals(jsonString)) {
            return new ArrayList<>();
        }

        try {
            return objectMapper.readValue(jsonString, new TypeReference<List<UUID>>() {});
        } catch (JsonProcessingException e) {
            log.error("Error converting JSON to UUID list: {}", jsonString, e);
            return new ArrayList<>();
        }
    }
}
