package com.qbitspark.buildwisebackend.accounting_service.documentflow.payment.service.impl;


import com.qbitspark.buildwisebackend.accounting_service.documentflow.payment.service.PaymentNumberService;
import com.qbitspark.buildwisebackend.organisation_service.organisation_mng.entity.OrganisationEntity;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
public class PaymentNumberServiceImpl implements PaymentNumberService {

    /**
     * Generates payment number in format: PAY-[YEAR]-[ORG-PREFIX]-[SEQUENCE]
     * Example: PAY-25-QBS-001
     */
    @Override
    public String generatePaymentNumber(OrganisationEntity organisation) {

        // Get current year (2-digit format)
        String year = String.valueOf(LocalDate.now().getYear()).substring(2); // "25" from "2025"

        // Get organisation prefix (first 3 chars of name, uppercase)
        String orgPrefix = getOrganisationPrefix(organisation);

        // Get sequence (simplified - in production, use database sequence)
        String sequence = String.format("%03d", System.currentTimeMillis() % 1000);

        return String.format("PAY-%s-%s-%s", year, orgPrefix, sequence);
    }

    @Override
    public String previewNextPaymentNumber(OrganisationEntity organisation) {
        // Same logic as generate, but without incrementing any counters
        return generatePaymentNumber(organisation);
    }

    /**
     * Extract organisation prefix from name
     */
    private String getOrganisationPrefix(OrganisationEntity organisation) {
        String name = organisation.getOrganisationName().toUpperCase();

        // Remove common words and take first 3 letters
        String cleaned = name.replaceAll("\\b(CO|LTD|LIMITED|COMPANY|CONSTRUCTION|BUILDERS?)\\b", "")
                .replaceAll("[^A-Z]", "");

        if (cleaned.length() >= 3) {
            return cleaned.substring(0, 3);
        } else if (cleaned.length() > 0) {
            return cleaned + "X".repeat(3 - cleaned.length());
        } else {
            return "PAY"; // Fallback
        }
    }
}