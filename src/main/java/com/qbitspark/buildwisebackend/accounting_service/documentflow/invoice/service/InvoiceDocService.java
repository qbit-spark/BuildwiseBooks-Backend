package com.qbitspark.buildwisebackend.accounting_service.documentflow.invoice.service;

import com.qbitspark.buildwisebackend.accounting_service.documentflow.invoice.paylaod.*;
import com.qbitspark.buildwisebackend.globeadvice.exceptions.AccessDeniedException;
import com.qbitspark.buildwisebackend.globeadvice.exceptions.ItemNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

public interface InvoiceDocService {
    InvoiceDocResponse createInvoiceWithAttachments(UUID organisationId, CreateInvoiceDocRequest request, List<MultipartFile> attachments) throws ItemNotFoundException, AccessDeniedException;
}
