// ==================================================================
// STEP 2: Create VoucherInterpreter (Business → Accounting Rules)
// ==================================================================
package com.qbitspark.buildwisebackend.accounting_service.documentflow.voucher.Interpreter;

import com.qbitspark.buildwisebackend.accounting_service.documentflow.voucher.events.VoucherEvent;
import com.qbitspark.buildwisebackend.accounting_service.transactions_pipeline.events.BusinessEvent;
import com.qbitspark.buildwisebackend.accounting_service.transactions_pipeline.intepreters.BusinessEventInterpreter;
import com.qbitspark.buildwisebackend.accounting_service.transactions_pipeline.paylaods.JournalEntryLineRequest;
import com.qbitspark.buildwisebackend.accounting_service.transactions_pipeline.paylaods.JournalEntryRequest;
import com.qbitspark.buildwisebackend.accounting_service.transactions_pipeline.service.AccountLookupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class VoucherInterpreter implements BusinessEventInterpreter<VoucherEvent> {

    private final AccountLookupService accountLookupService;

    @Override
    public JournalEntryRequest interpret(VoucherEvent event) {

        log.info("Creating accounting entry for voucher: {}", event.getVoucherNumber());

        // Create the journal entry request
        JournalEntryRequest request = new JournalEntryRequest();
        request.setOrganisationId(event.getOrganisationId());
        request.setProjectId(event.getProjectId());
        request.setDescription("Voucher Payment - " + event.getVoucherNumber());
        request.setReferenceNumber(event.getVoucherNumber());
        request.setTransactionDateTime(event.getEventDate());

        List<JournalEntryLineRequest> lines = new ArrayList<>();

        // SIMPLE RULE: All vouchers are expenses
        // DEBIT: Default Expense Account (for total amount)
        UUID expenseAccountId = accountLookupService.getDefaultExpenseAccountId(event.getOrganisationId());
        JournalEntryLineRequest expenseLine = JournalEntryLineRequest.debit(
                expenseAccountId,
                event.getTotalAmount(),
                "Voucher Expense - " + event.getVoucherNumber()
        );
        lines.add(expenseLine);

        // CREDIT: Accounts Payable (for total amount)
        UUID payableAccountId = accountLookupService.getAccountsPayableAccountId(event.getOrganisationId());
        JournalEntryLineRequest payableLine = JournalEntryLineRequest.credit(
                payableAccountId,
                event.getTotalAmount(),
                "Accounts Payable - " + getPayeeNames(event.getPayees())
        );
        lines.add(payableLine);

        request.setJournalEntryLines(lines);

        log.info("Created journal entry with {} lines for voucher {}", lines.size(), event.getVoucherNumber());
        return request;
    }

    @Override
    public boolean canHandle(Class<? extends BusinessEvent> eventType) {
        return VoucherEvent.class.equals(eventType);
    }

    /**
     * Helper method to create a summary of payee names
     */
    private String getPayeeNames(List<VoucherEvent.PayeeInfo> payees) {
        if (payees.size() == 1) {
            return payees.get(0).getVendorName();
        } else {
            return payees.size() + " vendors";
        }
    }
}