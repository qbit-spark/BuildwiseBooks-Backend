package com.qbitspark.buildwisebackend.accounting_service.transactions.intepreters;


import com.qbitspark.buildwisebackend.accounting_service.transactions.events.BusinessEvent;
import com.qbitspark.buildwisebackend.accounting_service.transactions.events.user_transaction.ExpenseEvent;
import com.qbitspark.buildwisebackend.accounting_service.transactions.paylaods.JournalEntryLineRequest;
import com.qbitspark.buildwisebackend.accounting_service.transactions.paylaods.JournalEntryRequest;
import com.qbitspark.buildwisebackend.accounting_service.transactions.service.AccountLookupService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.util.Arrays;

@Component
@RequiredArgsConstructor
public class ExpenseInterpreter implements BusinessEventInterpreter<ExpenseEvent> {

    private final AccountLookupService accountLookupService;

    @Override
    public JournalEntryRequest interpret(ExpenseEvent event) {

        JournalEntryRequest request = new JournalEntryRequest();
        request.setOrganisationId(event.getOrganisationId());
        request.setProjectId(event.getProjectId());
        request.setDescription("Expense - " + event.getDescription());
        request.setReferenceNumber(event.getReferenceNumber());
        request.setTransactionDateTime(event.getEventDate());

        // DEBIT: Expense Account
        JournalEntryLineRequest expenseLine = JournalEntryLineRequest.debit(
                event.getExpenseAccountId(),
                event.getAmount(),
                buildExpenseDescription(event)
        );

        // CREDIT: Cash or Accounts Payable (depending on payment status)
        JournalEntryLineRequest paymentLine = createPaymentLine(event);

        request.setJournalEntryLines(Arrays.asList(expenseLine, paymentLine));

        return request;
    }

    @Override
    public boolean canHandle(Class<? extends BusinessEvent> eventType) {
        return ExpenseEvent.class.equals(eventType);
    }

    private JournalEntryLineRequest createPaymentLine(ExpenseEvent event) {
        if (event.isPaid()) {
            // Paid immediately - credit cash
            return JournalEntryLineRequest.credit(
                    accountLookupService.getCashAccountId(event.getOrganisationId()),
                    event.getAmount(),
                    "Cash payment - " + event.getDescription()
            );
        } else {
            // On credit - credit accounts payable
            return JournalEntryLineRequest.credit(
                    accountLookupService.getAccountsPayableAccountId(event.getOrganisationId()),
                    event.getAmount(),
                    "Accounts payable - " + event.getDescription()
            );
        }
    }

    private String buildExpenseDescription(ExpenseEvent event) {
        StringBuilder description = new StringBuilder();
        description.append("Expense - ").append(event.getDescription());

        if (event.getCategory() != null && !event.getCategory().trim().isEmpty()) {
            description.append(" (").append(event.getCategory()).append(")");
        }

        return description.toString();
    }
}