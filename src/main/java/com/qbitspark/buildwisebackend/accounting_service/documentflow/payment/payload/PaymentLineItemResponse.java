package com.qbitspark.buildwisebackend.accounting_service.documentflow.payment.payload;

import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class PaymentLineItemResponse {
    private UUID id;
    private UUID voucherId;
    private String voucherNumber;
    private BigDecimal amount;
    private String description;
    private Integer lineOrder;
}