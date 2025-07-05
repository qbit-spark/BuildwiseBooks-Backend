package com.qbitspark.buildwisebackend.accounting_service.receipt_mng.service.impl;

import com.qbitspark.buildwisebackend.accounting_service.receipt_mng.repo.ReceiptRepo;
import com.qbitspark.buildwisebackend.organisation_service.organisation_mng.entity.OrganisationEntity;
import com.qbitspark.buildwisebackend.projectmng_service.entity.ProjectEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class ReceiptNumberService {

    private final ReceiptRepo receiptRepo;

    public String generateReceiptNumber(ProjectEntity project, OrganisationEntity organisation) {
        String projectCode = project.getProjectCode();
        String year = String.valueOf(LocalDate.now().getYear()).substring(2);

        String prefix = projectCode + "-RCP-" + year + "-";
        String pattern = prefix + "%";

        Integer maxSequence = getMaxSequenceForPattern(prefix, pattern, organisation);
        int nextSequence = (maxSequence != null ? maxSequence : 0) + 1;

        return prefix + String.format("%03d", nextSequence);
    }

    private Integer getMaxSequenceForPattern(String prefix, String pattern, OrganisationEntity organisation) {
        return receiptRepo.findByReceiptNumberContainingAndOrganisation(pattern.replace("%", ""), organisation)
                .stream()
                .map(receipt -> receipt.getReceiptNumber())
                .filter(number -> number.startsWith(prefix.substring(0, prefix.length() - 1)))
                .map(number -> extractSequenceNumber(number, prefix))
                .filter(seq -> seq != null)
                .max(Integer::compareTo)
                .orElse(0);
    }

    private Integer extractSequenceNumber(String receiptNumber, String prefix) {
        try {
            String sequencePart = receiptNumber.substring(prefix.length());
            return Integer.parseInt(sequencePart);
        } catch (Exception e) {
            return null;
        }
    }
}