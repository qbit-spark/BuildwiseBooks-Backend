package com.qbitspark.buildwisebackend.accounting_service.documentflow.service;

import com.qbitspark.buildwisebackend.accounting_service.documentflow.paylaod.CreateInvoiceDocRequest;
import com.qbitspark.buildwisebackend.accounting_service.documentflow.paylaod.CreateInvoiceDocResponse;
import com.qbitspark.buildwisebackend.accounting_service.documentflow.paylaod.InvoiceDocResponse;
import com.qbitspark.buildwisebackend.globeadvice.exceptions.ItemNotFoundException;

import java.util.List;
import java.util.UUID;

public interface InvoiceDocService {
    CreateInvoiceDocResponse createInvoice(CreateInvoiceDocRequest request) throws ItemNotFoundException;
    InvoiceDocResponse getInvoiceById(UUID invoiceId) throws ItemNotFoundException;
    List<InvoiceDocResponse> getProjectInvoices(UUID projectId) throws ItemNotFoundException;
    List<InvoiceDocResponse> getOrganisationInvoices(UUID organisationId) throws ItemNotFoundException;
    void sendInvoice(UUID invoiceId) throws Exception;
    void approveInvoice(UUID invoiceId) throws Exception;
}
