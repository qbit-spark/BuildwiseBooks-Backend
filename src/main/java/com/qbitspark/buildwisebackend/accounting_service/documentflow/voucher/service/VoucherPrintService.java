package com.qbitspark.buildwisebackend.accounting_service.documentflow.voucher.service;

import com.qbitspark.buildwisebackend.accounting_service.documentflow.voucher.entity.VoucherEntity;

public interface VoucherPrintService {
    byte[] generateVoucherPdf(VoucherEntity voucher);

    String generateVoucherHtml(VoucherEntity voucher);
}