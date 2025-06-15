package com.qbitspark.buildwisebackend.accounting_service.documentflow.payment.event;

import com.qbitspark.buildwisebackend.accounting_service.transactions_pipeline.events.BusinessEvent;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
@EqualsAndHashCode(callSuper = true)
public class PaymentEvent extends BusinessEvent {

    private UUID paymentId;
    private String paymentNumber;
    private BigDecimal totalAmount;
    private String paymentMode; // CASH, BANK_TRANSFER, etc.
    private String paymentReference;
    private List<VoucherPaymentInfo> voucherPayments = new ArrayList<>();

    @Data
    public static class VoucherPaymentInfo {
        private UUID voucherId;
        private String voucherNumber;
        private BigDecimal amount;
        private String description;
    }

    public PaymentEvent() {
        super();
    }

    @Override
    public boolean isValid() {
        return super.isValid() &&
                paymentId != null &&
                paymentNumber != null &&
                totalAmount != null &&
                totalAmount.compareTo(BigDecimal.ZERO) > 0 &&
                !voucherPayments.isEmpty();
    }
}
