package com.qbitspark.buildwisebackend.accounting_service.transactions.events.user_transaction;

import com.qbitspark.buildwisebackend.accounting_service.transactions.events.BusinessEvent;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.math.BigDecimal;
import java.util.UUID;

@Data
@EqualsAndHashCode(callSuper = true)
public class ExpenseEvent extends BusinessEvent {

    private UUID vendorId;
    private UUID expenseAccountId;
    private BigDecimal amount;
    private String category;
    private boolean isPaid;

    public ExpenseEvent() {
        super();
    }

    public ExpenseEvent(UUID expenseAccountId, BigDecimal amount,
                        String description, boolean isPaid) {
        super();
        this.expenseAccountId = expenseAccountId;
        this.amount = amount;
        this.isPaid = isPaid;
        this.setDescription(description);
    }

    public ExpenseEvent(UUID vendorId, UUID expenseAccountId, BigDecimal amount,
                        String category, String description, boolean isPaid) {
        super();
        this.vendorId = vendorId;
        this.expenseAccountId = expenseAccountId;
        this.amount = amount;
        this.category = category;
        this.isPaid = isPaid;
        this.setDescription(description);
    }

    @Override
    public boolean isValid() {
        return super.isValid() &&
                expenseAccountId != null &&
                amount != null &&
                amount.compareTo(BigDecimal.ZERO) > 0;
    }

    public String getPaymentType() {
        return isPaid ? "CASH" : "CREDIT";
    }
}