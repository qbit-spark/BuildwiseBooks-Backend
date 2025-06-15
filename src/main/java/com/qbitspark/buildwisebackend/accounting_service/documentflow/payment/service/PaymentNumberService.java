package com.qbitspark.buildwisebackend.accounting_service.documentflow.payment.service;

import com.qbitspark.buildwisebackend.organisation_service.organisation_mng.entity.OrganisationEntity;

public interface PaymentNumberService {
    String generatePaymentNumber(OrganisationEntity organisation);
    String previewNextPaymentNumber(OrganisationEntity organisation);
}

