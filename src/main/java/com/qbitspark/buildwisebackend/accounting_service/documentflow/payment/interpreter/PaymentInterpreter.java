package com.qbitspark.buildwisebackend.accounting_service.documentflow.payment.interpreter;

import com.qbitspark.buildwisebackend.accounting_service.documentflow.payment.event.PaymentEvent;
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
public class PaymentInterpreter implements BusinessEventInterpreter<PaymentEvent> {

    private final AccountLookupService accountLookupService;

    @Override
    public JournalEntryRequest interpret(PaymentEvent event) {

        log.info("Creating payment accounting entry for: {}", event.getPaymentNumber());

        JournalEntryRequest request = new JournalEntryRequest();
        request.setOrganisationId(event.getOrganisationId());
        request.setProjectId(event.getProjectId());
        request.setDescription("Payment - " + event.getPaymentNumber());
        request.setReferenceNumber(event.getPaymentNumber());
        request.setTransactionDateTime(event.getEventDate());

        List<JournalEntryLineRequest> lines = new ArrayList<>();

        // SIMPLE PAYMENT RULE:
        // DEBIT: Accounts Payable (clear the debt)
        // CREDIT: Cash (money going out)

        UUID payableAccountId = accountLookupService.getAccountsPayableAccountId(event.getOrganisationId());
        JournalEntryLineRequest payableLine = JournalEntryLineRequest.debit(
                payableAccountId,
                event.getTotalAmount(),
                "Payment of vouchers - " + getVoucherSummary(event.getVoucherPayments())
        );
        lines.add(payableLine);

        UUID cashAccountId = accountLookupService.getCashAccountId(event.getOrganisationId());
        JournalEntryLineRequest cashLine = JournalEntryLineRequest.credit(
                cashAccountId,
                event.getTotalAmount(),
                "Payment via " + event.getPaymentMode() + " - " + event.getPaymentReference()
        );
        lines.add(cashLine);

        request.setJournalEntryLines(lines);

        log.info("Created payment journal entry with {} lines", lines.size());
        return request;
    }

    @Override
    public boolean canHandle(Class<? extends BusinessEvent> eventType) {
        return PaymentEvent.class.equals(eventType);
    }

    private String getVoucherSummary(List<PaymentEvent.VoucherPaymentInfo> vouchers) {
        if (vouchers.size() == 1) {
            return vouchers.get(0).getVoucherNumber();
        } else {
            return vouchers.size() + " vouchers";
        }
    }
}
