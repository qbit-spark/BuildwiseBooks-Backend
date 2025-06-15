package com.qbitspark.buildwisebackend.accounting_service.documentflow.payment.payload;

import com.qbitspark.buildwisebackend.accounting_service.documentflow.payment.enums.PaymentStatus;
import com.qbitspark.buildwisebackend.accounting_service.documentflow.payment.enums.PaymentType;
import com.qbitspark.buildwisebackend.accounting_service.documentflow.voucher.enums.PaymentMode;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
public class PaymentResponse {
    private UUID id;
    private String paymentNumber;
    private LocalDate paymentDate;
    private PaymentType paymentType;
    private PaymentMode paymentMode;
    private PaymentStatus paymentStatus;
    private BigDecimal totalAmount;
    private String currency;
    private String paymentReference;
    private String paymentDescription;
    private String notes;
    private UUID organisationId;
    private String organisationName;
    private UUID projectId;
    private String projectName;
    private String createdByName;
    private LocalDateTime processedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<PaymentLineItemResponse> lineItems;
}

