package com.qbitspark.buildwisebackend.accounting_service.documentflow.voucher.paylaod;

import com.qbitspark.buildwisebackend.accounting_service.documentflow.voucher.enums.PaymentStatus;
import com.qbitspark.buildwisebackend.vendormng_service.enums.VendorType;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class VoucherPayeeResponse {
    private UUID id;
    private UUID vendorId;
    private String vendorName;
    private VendorType vendorType;
    private BigDecimal amount;
    private String description;
    private PaymentStatus paymentStatus;
    private LocalDateTime paidAt;
    private String paymentReference;
}