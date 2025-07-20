package com.qbitspark.buildwisebackend.accounting_service.documentflow.voucher.service;

import com.qbitspark.buildwisebackend.accounting_service.documentflow.invoice.enums.ActionType;
import com.qbitspark.buildwisebackend.accounting_service.documentflow.voucher.entity.VoucherEntity;
import com.qbitspark.buildwisebackend.accounting_service.documentflow.voucher.paylaod.*;
import com.qbitspark.buildwisebackend.globeadvice.exceptions.AccessDeniedException;
import com.qbitspark.buildwisebackend.globeadvice.exceptions.ItemNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface VoucherService {

    VoucherEntity createVoucher(UUID organisationId, CreateVoucherRequest request, ActionType actionType)
            throws ItemNotFoundException, AccessDeniedException;

    Page<VoucherEntity> getProjectVouchers(UUID organisationId, UUID projectId, Pageable pageable)
            throws ItemNotFoundException, AccessDeniedException;

    VoucherEntity updateVoucher(UUID organisationId, UUID voucherId, UpdateVoucherRequest request)
            throws ItemNotFoundException, AccessDeniedException;

    VoucherEntity getVoucherById(UUID organisationId, UUID voucherId) throws ItemNotFoundException, AccessDeniedException;

}