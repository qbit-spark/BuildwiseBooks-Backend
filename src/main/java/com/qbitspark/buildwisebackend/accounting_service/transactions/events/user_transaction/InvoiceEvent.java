package com.qbitspark.buildwisebackend.accounting_service.transactions.events.user_transaction;

import com.qbitspark.buildwisebackend.accounting_service.transactions.events.BusinessEvent;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
@EqualsAndHashCode(callSuper = true)
public class InvoiceEvent extends BusinessEvent {

    private UUID customerId;
    private BigDecimal totalAmount;
    private BigDecimal taxAmount;
    private List<InvoiceLineItem> lineItems = new ArrayList<>();

    @Data
    public static class InvoiceLineItem {
        private String description;
        private BigDecimal quantity;
        private BigDecimal unitPrice;
        private BigDecimal lineTotal;
        private UUID revenueAccountId;

        public InvoiceLineItem() {}

        public InvoiceLineItem(String description, BigDecimal quantity,
                               BigDecimal unitPrice, UUID revenueAccountId) {
            this.description = description;
            this.quantity = quantity;
            this.unitPrice = unitPrice;
            this.revenueAccountId = revenueAccountId;
            this.lineTotal = quantity.multiply(unitPrice);
        }

        public boolean isValid() {
            return description != null && !description.trim().isEmpty() &&
                    quantity != null && quantity.compareTo(BigDecimal.ZERO) > 0 &&
                    unitPrice != null && unitPrice.compareTo(BigDecimal.ZERO) > 0 &&
                    lineTotal != null && lineTotal.compareTo(BigDecimal.ZERO) > 0 &&
                    revenueAccountId != null;
        }
    }

    public void addLineItem(String description, BigDecimal quantity,
                            BigDecimal unitPrice, UUID revenueAccountId) {
        InvoiceLineItem item = new InvoiceLineItem(description, quantity, unitPrice, revenueAccountId);
        this.lineItems.add(item);
        recalculateTotal();
    }

    public void recalculateTotal() {
        BigDecimal subtotal = lineItems.stream()
                .map(InvoiceLineItem::getLineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal tax = (taxAmount != null) ? taxAmount : BigDecimal.ZERO;
        this.totalAmount = subtotal.add(tax);
    }

    @Override
    public boolean isValid() {
        return super.isValid() &&
                !lineItems.isEmpty() &&
                lineItems.stream().allMatch(InvoiceLineItem::isValid) &&
                totalAmount != null && totalAmount.compareTo(BigDecimal.ZERO) > 0;
    }
}