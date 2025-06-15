package com.qbitspark.buildwisebackend.accounting_service.documentflow.payment.payload;

import com.qbitspark.buildwisebackend.accounting_service.documentflow.voucher.enums.PaymentMode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
public class CreateVoucherPaymentRequest {


    @NotNull(message = "Payment mode is required")
    private PaymentMode paymentMode;

    @NotNull(message = "Payment reference is required")
    private String paymentReference;

    private String paymentDescription;
    private String notes;

    @NotEmpty(message = "At least one voucher must be specified")
    @Valid
    private List<VoucherPaymentItem> vouchers;

    @Data
    public static class VoucherPaymentItem {
        @NotNull(message = "Voucher ID is required")
        private UUID voucherId;

        private String description; // Optional override description
    }
}
