package com.qbitspark.buildwisebackend.accounting_service.documentflow.voucher.events;

import com.qbitspark.buildwisebackend.accounting_service.transactions_pipeline.events.BusinessEvent;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
@EqualsAndHashCode(callSuper = true)
public class VoucherEvent extends BusinessEvent {

    private UUID voucherId;
    private String voucherNumber;
    private BigDecimal totalAmount;
    private List<PayeeInfo> payees = new ArrayList<>();

    @Data
    public static class PayeeInfo {
        private UUID vendorId;
        private String vendorName;
        private BigDecimal amount;
        private String description;
    }

    public VoucherEvent() {
        super();
    }

    @Override
    public boolean isValid() {
        return super.isValid() &&
                voucherId != null &&
                voucherNumber != null &&
                totalAmount != null &&
                totalAmount.compareTo(BigDecimal.ZERO) > 0 &&
                !payees.isEmpty();
    }
}
