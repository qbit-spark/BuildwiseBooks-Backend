package com.qbitspark.buildwisebackend.accounting_service.transactions_pipeline.events.user_transaction;

import com.qbitspark.buildwisebackend.accounting_service.transactions_pipeline.events.BusinessEvent;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.math.BigDecimal;
import java.util.UUID;

@Data
@EqualsAndHashCode(callSuper = true)
public class SimpleTransactionEvent extends BusinessEvent {

    private UUID debitAccountId;
    private UUID creditAccountId;
    private BigDecimal amount;

    public SimpleTransactionEvent(UUID debitAccountId, UUID creditAccountId,
                                  BigDecimal amount, String description) {
        super();
        this.debitAccountId = debitAccountId;
        this.creditAccountId = creditAccountId;
        this.amount = amount;
        this.setDescription(description);
    }

    @Override
    public boolean isValid() {
        return super.isValid() &&
                this.debitAccountId != null &&
                this.creditAccountId != null &&
                this.amount != null &&
                this.amount.compareTo(BigDecimal.ZERO) > 0 &&
                !this.debitAccountId.equals(this.creditAccountId);
    }

}