package com.qbitspark.buildwisebackend.accounting_service.documentflow.voucher.controller;

import com.qbitspark.buildwisebackend.accounting_service.documentflow.voucher.paylaod.*;
import com.qbitspark.buildwisebackend.accounting_service.documentflow.voucher.service.VoucherService;
import com.qbitspark.buildwisebackend.globeadvice.exceptions.AccessDeniedException;
import com.qbitspark.buildwisebackend.globeadvice.exceptions.ItemNotFoundException;
import com.qbitspark.buildwisebackend.globeresponsebody.GlobeSuccessResponseBuilder;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/org-accounting/{organisationId}/vouchers")
@RequiredArgsConstructor
@Slf4j
public class VoucherController {

    private final VoucherService voucherService;

    @PostMapping
    public ResponseEntity<GlobeSuccessResponseBuilder> createVoucher(
            @PathVariable UUID organisationId,
            @Valid @RequestBody CreateVoucherRequest request) throws ItemNotFoundException, AccessDeniedException {

        VoucherResponse response = voucherService.createVoucher(organisationId, request);

        return ResponseEntity.ok(
                GlobeSuccessResponseBuilder.success(
                        "Voucher created successfully",
                        response
                )
        );
    }

    @GetMapping("/{voucherId}")
    public ResponseEntity<GlobeSuccessResponseBuilder> getVoucherById(
            @PathVariable UUID organisationId,
            @PathVariable UUID voucherId) throws ItemNotFoundException, AccessDeniedException {

        VoucherResponse response = voucherService.getVoucherById(organisationId, voucherId);

        return ResponseEntity.ok(
                GlobeSuccessResponseBuilder.success(
                        "Voucher retrieved successfully",
                        response
                )
        );
    }

    @GetMapping("/voucher-number/{voucherNumber}")
    public ResponseEntity<GlobeSuccessResponseBuilder> getVoucherByNumber(
            @PathVariable UUID organisationId,
            @PathVariable String voucherNumber) throws ItemNotFoundException, AccessDeniedException {

        VoucherResponse response = voucherService.getVoucherByNumber(organisationId, voucherNumber);

        return ResponseEntity.ok(
                GlobeSuccessResponseBuilder.success(
                        "Voucher retrieved successfully",
                        response
                )
        );
    }

    @GetMapping("/project/{projectId}")
    public ResponseEntity<GlobeSuccessResponseBuilder> getProjectVouchers(
            @PathVariable UUID organisationId,
            @PathVariable UUID projectId) throws ItemNotFoundException, AccessDeniedException {

        List<VoucherSummaryResponse> responses = voucherService.getProjectVouchers(organisationId, projectId);

        return ResponseEntity.ok(
                GlobeSuccessResponseBuilder.success(
                        "Project vouchers retrieved successfully",
                        responses
                )
        );
    }

    @GetMapping
    public ResponseEntity<GlobeSuccessResponseBuilder> getOrganisationVouchers(
            @PathVariable UUID organisationId) throws ItemNotFoundException, AccessDeniedException {

        List<VoucherSummaryResponse> responses = voucherService.getOrganisationVouchers(organisationId);

        return ResponseEntity.ok(
                GlobeSuccessResponseBuilder.success(
                        "Organisation vouchers retrieved successfully",
                        responses
                )
        );
    }

    @PostMapping("/{voucherId}/approve")
    public ResponseEntity<GlobeSuccessResponseBuilder> approveVoucher(
            @PathVariable UUID organisationId,
            @PathVariable UUID voucherId,
            @RequestBody ApproveVoucherRequest request) throws ItemNotFoundException, AccessDeniedException {

        VoucherResponse response = voucherService.approveVoucher(organisationId, voucherId, request);

        return ResponseEntity.ok(
                GlobeSuccessResponseBuilder.success(
                        "Voucher approved successfully",
                        response
                )
        );
    }

    @PostMapping("/preview-voucher-number")
    public ResponseEntity<GlobeSuccessResponseBuilder> previewVoucherNumber(
            @PathVariable UUID organisationId,
            @Valid @RequestBody PreviewVoucherNumberRequest request) throws ItemNotFoundException, AccessDeniedException {

        PreviewVoucherNumberResponse response = voucherService.previewVoucherNumber(organisationId, request);

        return ResponseEntity.ok(
                GlobeSuccessResponseBuilder.success(
                        "Voucher number preview generated successfully",
                        response
                )
        );
    }

    @PutMapping("/{voucherId}")
    public ResponseEntity<GlobeSuccessResponseBuilder> updateVoucher(
            @PathVariable UUID organisationId,
            @PathVariable UUID voucherId,
            @Valid @RequestBody UpdateVoucherRequest request) throws AccessDeniedException, ItemNotFoundException {

            VoucherResponse response = voucherService.updateVoucher(organisationId, voucherId, request);
            return ResponseEntity.ok(
                    GlobeSuccessResponseBuilder.success(
                            "Voucher updated successfully",
                            response
                    )
            );

    }
}