package com.qbitspark.buildwisebackend.accounting_service.documentflow.invoice.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qbitspark.buildwisebackend.accounting_service.documentflow.invoice.entity.embedings.InvoiceTaxDetail;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

@Converter
@Slf4j
public class InvoiceTaxDetailListConverter implements AttributeConverter<List<InvoiceTaxDetail>, String> {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(List<InvoiceTaxDetail> attribute) {
        if (attribute == null || attribute.isEmpty()) {
            return "[]";
        }
        try {
            return objectMapper.writeValueAsString(attribute);
        } catch (JsonProcessingException e) {
            log.error("Error converting InvoiceTaxDetail list to JSON", e);
            return "[]";
        }
    }

    @Override
    public List<InvoiceTaxDetail> convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.trim().isEmpty()) {
            return new ArrayList<>();
        }
        try {
            return objectMapper.readValue(dbData, new TypeReference<List<InvoiceTaxDetail>>() {});
        } catch (JsonProcessingException e) {
            log.error("Error converting JSON to InvoiceTaxDetail list", e);
            return new ArrayList<>();
        }
    }
}