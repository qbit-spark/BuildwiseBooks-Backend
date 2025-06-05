package com.qbitspark.buildwisebackend.accounting_service.transactions.intepreters;


import com.qbitspark.buildwisebackend.accounting_service.transactions.events.BusinessEvent;
import com.qbitspark.buildwisebackend.accounting_service.transactions.events.user_transaction.InvoiceEvent;
import com.qbitspark.buildwisebackend.accounting_service.transactions.paylaods.JournalEntryLineRequest;
import com.qbitspark.buildwisebackend.accounting_service.transactions.paylaods.JournalEntryRequest;
import com.qbitspark.buildwisebackend.accounting_service.transactions.service.AccountLookupService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class InvoiceInterpreter implements BusinessEventInterpreter<InvoiceEvent> {

    private final AccountLookupService accountLookupService;

    @Override
    public JournalEntryRequest interpret(InvoiceEvent event) {

        JournalEntryRequest request = new JournalEntryRequest();
        request.setOrganisationId(event.getOrganisationId());
        request.setProjectId(event.getProjectId());
        request.setDescription("Invoice - " + event.getDescription());
        request.setReferenceNumber(event.getReferenceNumber());
        request.setTransactionDateTime(event.getEventDate());

        List<JournalEntryLineRequest> lines = new ArrayList<>();

        // DEBIT: Accounts Receivable for total amount
        JournalEntryLineRequest receivableLine = JournalEntryLineRequest.debit(
                accountLookupService.getAccountsReceivableAccountId(event.getOrganisationId()),
                event.getTotalAmount(),
                "Invoice receivable - " + event.getDescription()
        );
        lines.add(receivableLine);

        // CREDIT: Revenue accounts for each line item
        for (InvoiceEvent.InvoiceLineItem lineItem : event.getLineItems()) {
            JournalEntryLineRequest revenueLine = JournalEntryLineRequest.credit(
                    lineItem.getRevenueAccountId(),
                    lineItem.getLineTotal(),
                    "Revenue - " + lineItem.getDescription()
            );
            lines.add(revenueLine);
        }

        // CREDIT: Tax Payable if there's tax
        if (event.getTaxAmount() != null && event.getTaxAmount().compareTo(BigDecimal.ZERO) > 0) {
            JournalEntryLineRequest taxLine = JournalEntryLineRequest.credit(
                    accountLookupService.getTaxPayableAccountId(event.getOrganisationId()),
                    event.getTaxAmount(),
                    "Tax payable - " + event.getDescription()
            );
            lines.add(taxLine);
        }

        request.setJournalEntryLines(lines);
        return request;
    }

    @Override
    public boolean canHandle(Class<? extends BusinessEvent> eventType) {
        return InvoiceEvent.class.equals(eventType);
    }
}