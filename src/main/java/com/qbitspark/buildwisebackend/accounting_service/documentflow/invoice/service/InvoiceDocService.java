package com.qbitspark.buildwisebackend.accounting_service.documentflow.invoice.service;

import com.qbitspark.buildwisebackend.accounting_service.documentflow.invoice.enums.ActionType;
import com.qbitspark.buildwisebackend.accounting_service.documentflow.invoice.paylaod.*;
import com.qbitspark.buildwisebackend.globeadvice.exceptions.AccessDeniedException;
import com.qbitspark.buildwisebackend.globeadvice.exceptions.ItemNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

public interface InvoiceDocService {
    InvoiceDocResponse createInvoice(UUID organisationId, CreateInvoiceDocRequest request, ActionType actionType) throws ItemNotFoundException, AccessDeniedException;

    Page<SummaryInvoiceDocResponse> getAllInvoicesForOrganisation(UUID organisationId, int page, int size) throws ItemNotFoundException, AccessDeniedException;

   // List<SummaryInvoiceDocResponse> getAllInvoicesForOrganisationUnpaginated(UUID organisationId) throws ItemNotFoundException, AccessDeniedException;

    public Page<SummaryInvoiceDocResponse> getAllInvoicesForProject(UUID organisationId, UUID projectId, int page, int size) throws ItemNotFoundException, AccessDeniedException;

    public InvoiceDocResponse getInvoiceById(UUID organisationId, UUID invoiceId) throws ItemNotFoundException, AccessDeniedException;

    InvoiceDocResponse updateInvoice(UUID organisationId, UUID invoiceId, UpdateInvoiceDocRequest request) throws ItemNotFoundException, AccessDeniedException;
}


