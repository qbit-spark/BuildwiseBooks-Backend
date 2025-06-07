package com.qbitspark.buildwisebackend.accounting_service.documentflow.controller;

import com.qbitspark.buildwisebackend.accounting_service.documentflow.paylaod.CreateInvoiceDocRequest;
import com.qbitspark.buildwisebackend.accounting_service.documentflow.paylaod.CreateInvoiceDocResponse;
import com.qbitspark.buildwisebackend.accounting_service.documentflow.paylaod.InvoiceDocResponse;
import com.qbitspark.buildwisebackend.accounting_service.documentflow.service.InvoiceDocService;
import com.qbitspark.buildwisebackend.globeadvice.exceptions.ItemNotFoundException;
import com.qbitspark.buildwisebackend.globeresponsebody.GlobeSuccessResponseBuilder;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/accounting/doc-invoices")
@RequiredArgsConstructor
@Slf4j
public class InvoiceDocController {

    private final InvoiceDocService invoiceDocService;

    @PostMapping
    public ResponseEntity<GlobeSuccessResponseBuilder> createInvoice(
            @Valid @RequestBody CreateInvoiceDocRequest request) throws ItemNotFoundException {

        log.info("Creating invoice for project: {} client: {}",
                request.getProjectId(), request.getClientId());

        CreateInvoiceDocResponse response = invoiceDocService.createInvoice(request);

        return ResponseEntity.ok(
                GlobeSuccessResponseBuilder.success(
                        "Invoice created successfully",
                        response
                )
        );
    }

    @GetMapping("/{invoiceId}")
    public ResponseEntity<GlobeSuccessResponseBuilder> getInvoiceById(
            @PathVariable UUID invoiceId) throws ItemNotFoundException {

        InvoiceDocResponse response = invoiceDocService.getInvoiceById(invoiceId);

        return ResponseEntity.ok(
                GlobeSuccessResponseBuilder.success(
                        "Invoice retrieved successfully",
                        response
                )
        );
    }

    @GetMapping("/project/{projectId}")
    public ResponseEntity<GlobeSuccessResponseBuilder> getProjectInvoices(
            @PathVariable UUID projectId) throws ItemNotFoundException {

        List<InvoiceDocResponse> responses = invoiceDocService.getProjectInvoices(projectId);

        return ResponseEntity.ok(
                GlobeSuccessResponseBuilder.success(
                        "Project invoices retrieved successfully",
                        responses
                )
        );
    }

    @GetMapping("/organisation/{organisationId}")
    public ResponseEntity<GlobeSuccessResponseBuilder> getOrganisationInvoices(
            @PathVariable UUID organisationId) throws ItemNotFoundException {

        List<InvoiceDocResponse> responses = invoiceDocService.getOrganisationInvoices(organisationId);

        return ResponseEntity.ok(
                GlobeSuccessResponseBuilder.success(
                        "Organisation invoices retrieved successfully",
                        responses
                )
        );
    }

    @PostMapping("/{invoiceId}/send")
    public ResponseEntity<GlobeSuccessResponseBuilder> sendInvoice(
            @PathVariable UUID invoiceId) throws Exception {

        invoiceDocService.sendInvoice(invoiceId);

        return ResponseEntity.ok(
                GlobeSuccessResponseBuilder.success(
                        "Invoice sent successfully and accounting entry created"
                )
        );
    }

    @PostMapping("/{invoiceId}/approve")
    public ResponseEntity<GlobeSuccessResponseBuilder> approveInvoice(
            @PathVariable UUID invoiceId) throws Exception {

        invoiceDocService.approveInvoice(invoiceId);

        return ResponseEntity.ok(
                GlobeSuccessResponseBuilder.success(
                        "Invoice approved successfully"
                )
        );
    }
}