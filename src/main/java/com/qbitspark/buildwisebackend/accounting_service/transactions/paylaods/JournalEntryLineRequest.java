package com.qbitspark.buildwisebackend.accounting_service.transactions.paylaods;

import lombok.Data;
import java.math.BigDecimal;
import java.util.UUID;

@Data
public class JournalEntryLineRequest {

    private UUID accountId;
    private BigDecimal debitAmount;
    private BigDecimal creditAmount;
    private String description;

    public JournalEntryLineRequest() {}

    public JournalEntryLineRequest(UUID accountId, String description) {
        this.accountId = accountId;
        this.description = description;
    }

    public static JournalEntryLineRequest debit(UUID accountId, BigDecimal amount, String description) {
        JournalEntryLineRequest line = new JournalEntryLineRequest();
        line.setAccountId(accountId);
        line.setDebitAmount(amount);
        line.setDescription(description);
        return line;
    }

    public static JournalEntryLineRequest credit(UUID accountId, BigDecimal amount, String description) {
        JournalEntryLineRequest line = new JournalEntryLineRequest();
        line.setAccountId(accountId);
        line.setCreditAmount(amount);
        line.setDescription(description);
        return line;
    }

    public boolean isDebit() {
        return debitAmount != null && debitAmount.compareTo(BigDecimal.ZERO) > 0;
    }

    public boolean isCredit() {
        return creditAmount != null && creditAmount.compareTo(BigDecimal.ZERO) > 0;
    }

    public BigDecimal getAmount() {
        return isDebit() ? debitAmount : (isCredit() ? creditAmount : BigDecimal.ZERO);
    }

    public String getType() {
        return isDebit() ? "DEBIT" : (isCredit() ? "CREDIT" : "NONE");
    }
}