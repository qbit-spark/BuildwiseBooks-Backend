package com.qbitspark.buildwisebackend.accounting_service.documentflow.voucher.service;

import com.qbitspark.buildwisebackend.organisation_service.organisation_mng.entity.OrganisationEntity;
import com.qbitspark.buildwisebackend.projectmng_service.entity.ProjectEntity;

public interface VoucherNumberService {
    String generateVoucherNumber(ProjectEntity project, OrganisationEntity organisation);
    String previewNextVoucherNumber(ProjectEntity project, OrganisationEntity organisation);
}