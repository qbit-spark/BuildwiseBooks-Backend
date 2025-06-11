package com.qbitspark.buildwisebackend.accounting_service.documentflow.invoice.service;

import com.qbitspark.buildwisebackend.accounting_service.documentflow.invoice.paylaod.*;
import com.qbitspark.buildwisebackend.globeadvice.exceptions.AccessDeniedException;
import com.qbitspark.buildwisebackend.globeadvice.exceptions.ItemNotFoundException;

import java.util.List;
import java.util.UUID;

public interface InvoiceDocService {
    SummaryInvoiceDocResponse createInvoice(UUID organisationId, CreateInvoiceDocRequest request) throws ItemNotFoundException, AccessDeniedException;
    PreviewInvoiceNumberResponse previewInvoiceNumber(UUID organisationId, PreviewInvoiceNumberRequest request) throws ItemNotFoundException, AccessDeniedException;
    InvoiceDocResponse getInvoiceById(UUID organisationId, UUID invoiceId) throws ItemNotFoundException, AccessDeniedException;
    InvoiceDocResponse getInvoiceByNumber(UUID organisationId, String invoiceNumber) throws ItemNotFoundException, AccessDeniedException;
    List<SummaryInvoiceDocResponse> getProjectInvoices(UUID organisationId, UUID projectId) throws ItemNotFoundException, AccessDeniedException;
    List<SummaryInvoiceDocResponse> getClientInvoices(UUID organisationId, UUID clientId) throws ItemNotFoundException, AccessDeniedException;



    List<InvoiceDocResponse> getOrganisationInvoices(UUID organisationId) throws ItemNotFoundException;
    void sendInvoice(UUID invoiceId) throws Exception;
    void approveInvoice(UUID invoiceId) throws Exception;
}
