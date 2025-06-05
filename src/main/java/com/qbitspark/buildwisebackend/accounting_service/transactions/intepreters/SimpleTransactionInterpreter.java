package com.qbitspark.buildwisebackend.accounting_service.transactions.intepreters;


import com.qbitspark.buildwisebackend.accounting_service.transactions.events.BusinessEvent;
import com.qbitspark.buildwisebackend.accounting_service.transactions.events.user_transaction.SimpleTransactionEvent;
import com.qbitspark.buildwisebackend.accounting_service.transactions.paylaods.JournalEntryLineRequest;
import com.qbitspark.buildwisebackend.accounting_service.transactions.paylaods.JournalEntryRequest;
import org.springframework.stereotype.Component;
import java.util.Arrays;

@Component
public class SimpleTransactionInterpreter implements BusinessEventInterpreter<SimpleTransactionEvent> {

    @Override
    public JournalEntryRequest interpret(SimpleTransactionEvent event) {

        JournalEntryRequest request = new JournalEntryRequest();
        request.setOrganisationId(event.getOrganisationId());
        request.setProjectId(event.getProjectId());
        request.setDescription(event.getDescription());
        request.setReferenceNumber(event.getReferenceNumber());
        request.setTransactionDateTime(event.getEventDate());

        JournalEntryLineRequest debitLine = JournalEntryLineRequest.debit(
                event.getDebitAccountId(),
                event.getAmount(),
                "Debit - " + event.getDescription()
        );

        JournalEntryLineRequest creditLine = JournalEntryLineRequest.credit(
                event.getCreditAccountId(),
                event.getAmount(),
                "Credit - " + event.getDescription()
        );

        request.setJournalEntryLines(Arrays.asList(debitLine, creditLine));

        return request;
    }

    @Override
    public boolean canHandle(Class<? extends BusinessEvent> eventType) {
        return SimpleTransactionEvent.class.equals(eventType);
    }
}