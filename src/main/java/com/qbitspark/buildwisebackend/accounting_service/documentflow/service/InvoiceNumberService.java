package com.qbitspark.buildwisebackend.accounting_service.documentflow.service;

import com.qbitspark.buildwisebackend.clientsmng_service.entity.ClientEntity;
import com.qbitspark.buildwisebackend.organisation_service.organisation_mng.entity.OrganisationEntity;
import com.qbitspark.buildwisebackend.projectmng_service.entity.ProjectEntity;

public interface InvoiceNumberService {
     String generateInvoiceNumber(ProjectEntity project, ClientEntity client, OrganisationEntity organisation);
}
