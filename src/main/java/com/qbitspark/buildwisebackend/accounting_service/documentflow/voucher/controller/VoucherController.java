package com.qbitspark.buildwisebackend.accounting_service.documentflow.voucher.controller;

import com.qbitspark.buildwisebackend.accounting_service.documentflow.voucher.entity.VoucherBeneficiaryEntity;
import com.qbitspark.buildwisebackend.accounting_service.documentflow.voucher.entity.VoucherDeductionEntity;
import com.qbitspark.buildwisebackend.accounting_service.documentflow.voucher.entity.VoucherEntity;
import com.qbitspark.buildwisebackend.accounting_service.documentflow.voucher.paylaod.*;
import com.qbitspark.buildwisebackend.accounting_service.documentflow.voucher.service.VoucherService;
import com.qbitspark.buildwisebackend.globeadvice.exceptions.AccessDeniedException;
import com.qbitspark.buildwisebackend.globeadvice.exceptions.ItemNotFoundException;
import com.qbitspark.buildwisebackend.globeresponsebody.GlobeSuccessResponseBuilder;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/accounting/{organisationId}/doc-vouchers")
@RequiredArgsConstructor
@Slf4j
public class VoucherController {

    private final VoucherService voucherService;

    @PostMapping
    public ResponseEntity<GlobeSuccessResponseBuilder> createVoucher(
            @PathVariable UUID organisationId,
            @Valid @RequestBody CreateVoucherRequest request)
            throws ItemNotFoundException, AccessDeniedException {

        VoucherEntity voucher = voucherService.createVoucher(organisationId, request);

        return ResponseEntity.ok(
                GlobeSuccessResponseBuilder.success(
                        "Voucher created successfully",
                        mapToVoucherResponse(voucher)
                )
        );
    }


    @GetMapping("/project/{projectId}")
    public ResponseEntity<GlobeSuccessResponseBuilder> getProjectVouchers(
            @PathVariable UUID organisationId,
            @PathVariable UUID projectId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDirection)
            throws ItemNotFoundException, AccessDeniedException {


        Sort sort = sortDirection.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();

        Pageable pageable = PageRequest.of(page, size, sort);


        Page<VoucherEntity> voucherPage = voucherService.getProjectVouchers(organisationId, projectId, pageable);


        Page<VoucherSummaryResponse> responsePage = voucherPage.map(this::mapToVoucherSummaryResponse);

        return ResponseEntity.ok(
                GlobeSuccessResponseBuilder.success(
                        "Project vouchers retrieved successfully",
                        responsePage
                )
        );
    }

    @PutMapping("/{voucherId}")
    public ResponseEntity<GlobeSuccessResponseBuilder> updateVoucher(
            @PathVariable UUID organisationId,
            @PathVariable UUID voucherId,
            @Valid @RequestBody UpdateVoucherRequest request)
            throws ItemNotFoundException, AccessDeniedException {

        VoucherEntity voucherEntity = voucherService.updateVoucher(organisationId, voucherId, request);
        VoucherResponse response = mapToVoucherResponse(voucherEntity);

        return ResponseEntity.ok(
                GlobeSuccessResponseBuilder.success(
                        "Voucher updated successfully",
                        response
                )
        );
    }


    private VoucherResponse mapToVoucherResponse(VoucherEntity voucher) {
        VoucherResponse response = new VoucherResponse();
        response.setId(voucher.getId());
        response.setVoucherNumber(voucher.getVoucherNumber());
        response.setGeneralDescription(voucher.getOverallDescription());
        response.setStatus(voucher.getStatus());
        response.setTotalAmount(voucher.getTotalAmount());
        response.setCurrency(voucher.getCurrency());
        response.setCreatedAt(voucher.getCreatedAt());
        response.setUpdatedAt(voucher.getUpdatedAt());

        // Organisation info
        response.setOrganisationId(voucher.getOrganisation().getOrganisationId());
        response.setOrganisationName(voucher.getOrganisation().getOrganisationName());

        // Project info
        response.setProjectId(voucher.getProject().getProjectId());
        response.setProjectName(voucher.getProject().getName());
        response.setProjectCode(voucher.getProject().getProjectCode());

        // Creator info
        response.setCreatedById(voucher.getCreatedBy().getMemberId());
        response.setCreatedByName(voucher.getCreatedBy().getAccount().getUserName());

        // Calculate total deductions
        BigDecimal totalDeductions = voucher.getBeneficiaries().stream()
                .flatMap(beneficiary -> beneficiary.getDeductions().stream())
                .map(VoucherDeductionEntity::getDeductionAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        response.setTotalDeductions(totalDeductions);
        response.setNetAmount(voucher.getTotalAmount().subtract(totalDeductions));

        // Map beneficiaries
        List<VoucherBeneficiaryResponse> beneficiaryResponses = voucher.getBeneficiaries().stream()
                .map(this::mapToBeneficiaryResponse)
                .collect(Collectors.toList());
        response.setBeneficiaries(beneficiaryResponses);

        // Placeholder for attachments
        response.setAttachmentIds(List.of());

        return response;
    }

    private VoucherBeneficiaryResponse mapToBeneficiaryResponse(VoucherBeneficiaryEntity beneficiary) {
        VoucherBeneficiaryResponse response = new VoucherBeneficiaryResponse();
        response.setVendorId(beneficiary.getVendor().getVendorId());
        response.setVendorName(beneficiary.getVendor().getName());
        response.setDescription(beneficiary.getDescription());
        response.setAmount(beneficiary.getAmount());

        // Calculate net amount for this beneficiary
        BigDecimal beneficiaryDeductions = beneficiary.getDeductions().stream()
                .map(VoucherDeductionEntity::getDeductionAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        response.setNetAmount(beneficiary.getAmount().subtract(beneficiaryDeductions));

        // Map deductions
        List<VoucherDeductionResponse> deductionResponses = beneficiary.getDeductions().stream()
                .map(this::mapToDeductionResponse)
                .collect(Collectors.toList());
        response.setDeductions(deductionResponses);

        return response;
    }

    private VoucherDeductionResponse mapToDeductionResponse(VoucherDeductionEntity deduction) {
        VoucherDeductionResponse response = new VoucherDeductionResponse();
        response.setDeductionType(deduction.getDeductionType());
        response.setPercentage(deduction.getPercentage());
        response.setDeductionAmount(deduction.getDeductionAmount());
        return response;
    }

    private VoucherSummaryResponse mapToVoucherSummaryResponse(VoucherEntity voucher) {
        VoucherSummaryResponse response = new VoucherSummaryResponse();

        response.setId(voucher.getId());
        response.setVoucherNumber(voucher.getVoucherNumber());
        response.setGeneralDescription(voucher.getOverallDescription());
        response.setStatus(voucher.getStatus());
        response.setNumberOfBeneficiaries(voucher.getBeneficiaries().size());


        BigDecimal totalDeductions = voucher.getBeneficiaries().stream()
                .flatMap(beneficiary -> beneficiary.getDeductions().stream())
                .map(VoucherDeductionEntity::getDeductionAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        response.setNetAmount(voucher.getTotalAmount().subtract(totalDeductions));

        return response;
    }


}