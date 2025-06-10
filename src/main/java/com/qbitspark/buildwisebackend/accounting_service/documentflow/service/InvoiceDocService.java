package com.qbitspark.buildwisebackend.accounting_service.documentflow.service;

import com.qbitspark.buildwisebackend.accounting_service.documentflow.paylaod.*;
import com.qbitspark.buildwisebackend.globeadvice.exceptions.AccessDeniedException;
import com.qbitspark.buildwisebackend.globeadvice.exceptions.ItemNotFoundException;

import java.util.List;
import java.util.UUID;

public interface InvoiceDocService {
    CreateInvoiceDocResponse createInvoice(UUID organisationId, CreateInvoiceDocRequest request) throws ItemNotFoundException, AccessDeniedException;
    PreviewInvoiceNumberResponse previewInvoiceNumber(UUID organisationId, PreviewInvoiceNumberRequest request) throws ItemNotFoundException, AccessDeniedException;
    InvoiceDocResponse getInvoiceById(UUID invoiceId) throws ItemNotFoundException;
    List<InvoiceDocResponse> getProjectInvoices(UUID projectId) throws ItemNotFoundException;
    List<InvoiceDocResponse> getOrganisationInvoices(UUID organisationId) throws ItemNotFoundException;
    void sendInvoice(UUID invoiceId) throws Exception;
    void approveInvoice(UUID invoiceId) throws Exception;
}
