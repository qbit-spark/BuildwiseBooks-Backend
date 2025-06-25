package com.qbitspark.buildwisebackend.accounting_service.documentflow.invoice.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.qbitspark.buildwisebackend.accounting_service.documentflow.invoice.enums.ActionType;
import com.qbitspark.buildwisebackend.accounting_service.documentflow.invoice.paylaod.*;
import com.qbitspark.buildwisebackend.accounting_service.documentflow.invoice.service.InvoiceDocService;
import com.qbitspark.buildwisebackend.globeadvice.exceptions.AccessDeniedException;
import com.qbitspark.buildwisebackend.globeadvice.exceptions.ItemNotFoundException;
import com.qbitspark.buildwisebackend.globeadvice.exceptions.RandomExceptions;
import com.qbitspark.buildwisebackend.globeresponsebody.GlobeSuccessResponseBuilder;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/accounting/{organisationId}/doc-invoices")
@RequiredArgsConstructor
@Slf4j
public class InvoiceDocController {

    private final InvoiceDocService invoiceDocService;



    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<GlobeSuccessResponseBuilder> createInvoice(
            @PathVariable UUID organisationId,
            @RequestPart("invoice") String invoiceJson,
            @RequestPart(value = "attachments", required = false) List<MultipartFile> attachments,
            @RequestParam(value = "action") ActionType action)
            throws ItemNotFoundException, AccessDeniedException, JsonProcessingException, RandomExceptions {

        // Input validation
        if (action == null) {
            throw new IllegalArgumentException("Action parameter is required and cannot be null");
        }

        // Parse JSON manually
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        CreateInvoiceDocRequest request = objectMapper.readValue(invoiceJson, CreateInvoiceDocRequest.class);

        InvoiceDocResponse response;
        String successMessage = "";

        switch (action) {
            case SAVE:
                response = invoiceDocService.createInvoiceWithAttachments(
                        organisationId, request, attachments);
                successMessage = "Invoice saved successfully";
                break;

            case SAVE_AND_APPROVAL:
                //Save and approval will be here
                response = invoiceDocService.createInvoiceWithAttachments(
                        organisationId, request, attachments);
                successMessage = "Invoice saved and ready for approval successfully";

                break;

            default:
                throw new RandomExceptions("Unsupported action type: " + action);
        }
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(GlobeSuccessResponseBuilder.success(successMessage, response));
    }

    @GetMapping
    public ResponseEntity<GlobeSuccessResponseBuilder> getAllInvoicesForOrganisation(
            @PathVariable UUID organisationId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) throws ItemNotFoundException, AccessDeniedException {

        Page<SummaryInvoiceDocResponse> response = invoiceDocService.getAllInvoicesForOrganisation(
                organisationId, page, size);

        return ResponseEntity.ok(GlobeSuccessResponseBuilder.success("Invoices retrieved successfully", response));
    }

    @GetMapping("/project/{projectId}")
    public ResponseEntity<GlobeSuccessResponseBuilder> getAllInvoicesForProject(
            @PathVariable UUID organisationId,
            @PathVariable UUID projectId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) throws ItemNotFoundException, AccessDeniedException {

        Page<SummaryInvoiceDocResponse> response = invoiceDocService.getAllInvoicesForProject(
                organisationId, projectId, page, size);

        return ResponseEntity.ok(GlobeSuccessResponseBuilder.success("Project invoices retrieved successfully", response));
    }

    @GetMapping("/{invoiceId}")
    public ResponseEntity<GlobeSuccessResponseBuilder> getInvoiceById(
            @PathVariable UUID organisationId,
            @PathVariable UUID invoiceId) throws ItemNotFoundException, AccessDeniedException {

        InvoiceDocResponse response = invoiceDocService.getInvoiceById(organisationId, invoiceId);

        return ResponseEntity.ok(GlobeSuccessResponseBuilder.success("Invoice retrieved successfully", response));
    }

    @PutMapping(value = "/{invoiceId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<GlobeSuccessResponseBuilder> updateInvoice(
            @PathVariable UUID organisationId,
            @PathVariable UUID invoiceId,
            @RequestPart("invoice") String invoiceJson,
            @RequestPart(value = "attachments", required = false) List<MultipartFile> attachments)
            throws ItemNotFoundException, AccessDeniedException, JsonProcessingException {

        // Parse JSON manually
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        UpdateInvoiceDocRequest request = objectMapper.readValue(invoiceJson, UpdateInvoiceDocRequest.class);

        InvoiceDocResponse response = invoiceDocService.updateInvoiceWithAttachments(
                organisationId, invoiceId, request, attachments);

        return ResponseEntity.ok(GlobeSuccessResponseBuilder.success("Invoice updated successfully", response));
    }
}