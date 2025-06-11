package com.qbitspark.buildwisebackend.accounting_service.documentflow.voucher.service;

import com.qbitspark.buildwisebackend.accounting_service.documentflow.voucher.paylaod.*;
import com.qbitspark.buildwisebackend.globeadvice.exceptions.AccessDeniedException;
import com.qbitspark.buildwisebackend.globeadvice.exceptions.ItemNotFoundException;

import java.util.List;
import java.util.UUID;

public interface VoucherService {

    VoucherResponse createVoucher(UUID organisationId, CreateVoucherRequest request)
            throws ItemNotFoundException, AccessDeniedException;

    VoucherResponse getVoucherById(UUID organisationId, UUID voucherId)
            throws ItemNotFoundException, AccessDeniedException;

    VoucherResponse getVoucherByNumber(UUID organisationId, String voucherNumber)
            throws ItemNotFoundException, AccessDeniedException;

    List<VoucherSummaryResponse> getProjectVouchers(UUID organisationId, UUID projectId)
            throws ItemNotFoundException, AccessDeniedException;

    List<VoucherSummaryResponse> getOrganisationVouchers(UUID organisationId)
            throws ItemNotFoundException, AccessDeniedException;

    VoucherResponse approveVoucher(UUID organisationId, UUID voucherId, ApproveVoucherRequest request)
            throws ItemNotFoundException, AccessDeniedException;

    PreviewVoucherNumberResponse previewVoucherNumber(UUID organisationId, PreviewVoucherNumberRequest request)
            throws ItemNotFoundException, AccessDeniedException;

    VoucherResponse updateVoucher(UUID organisationId, UUID voucherId, UpdateVoucherRequest request)
            throws ItemNotFoundException, AccessDeniedException;
}