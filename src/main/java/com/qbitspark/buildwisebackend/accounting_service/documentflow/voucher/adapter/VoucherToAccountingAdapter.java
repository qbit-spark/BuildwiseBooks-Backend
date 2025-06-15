// ==================================================================
// STEP 3: Create VoucherToAccountingAdapter (Database → Business Event)
// ==================================================================
package com.qbitspark.buildwisebackend.accounting_service.documentflow.voucher.adapter;

import com.qbitspark.buildwisebackend.accounting_service.coa.entity.JournalEntry;
import com.qbitspark.buildwisebackend.accounting_service.documentflow.voucher.entity.VoucherEntity;
import com.qbitspark.buildwisebackend.accounting_service.documentflow.voucher.entity.VoucherPayeeEntity;
import com.qbitspark.buildwisebackend.accounting_service.documentflow.voucher.events.VoucherEvent;
import com.qbitspark.buildwisebackend.accounting_service.transactions_pipeline.transaction_service.TransactionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class VoucherToAccountingAdapter {

    private final TransactionService transactionService;

    /**
     * MAIN METHOD: Convert voucher to accounting entry
     * Call this when voucher is approved
     */
    public JournalEntry createAccountingEntry(VoucherEntity voucher) throws Exception {

        log.info("Converting voucher {} to accounting entry", voucher.getVoucherNumber());

        // Step 1: Convert VoucherEntity to VoucherEvent
        VoucherEvent event = convertToEvent(voucher);

        // Step 2: Let the transaction pipeline handle everything else
        JournalEntry journalEntry = transactionService.processBusinessEvent(event);

        log.info("Successfully created journal entry {} for voucher {}",
                journalEntry.getId(), voucher.getVoucherNumber());

        return journalEntry;
    }

    /**
     * Convert VoucherEntity to VoucherEvent
     */
    private VoucherEvent convertToEvent(VoucherEntity voucher) {

        VoucherEvent event = new VoucherEvent();

        // Basic voucher info
        event.setVoucherId(voucher.getId());
        event.setVoucherNumber(voucher.getVoucherNumber());
        event.setTotalAmount(voucher.getTotalAmount());

        // Event metadata
        event.setOrganisationId(voucher.getOrganisation().getOrganisationId());
        event.setProjectId(voucher.getProject() != null ? voucher.getProject().getProjectId() : null);
        event.setDescription("Voucher payment: " + voucher.getOverallDescription());
        event.setReferenceNumber(voucher.getVoucherNumber());

        // Convert payees
        List<VoucherEvent.PayeeInfo> payeeInfos = voucher.getPayees().stream()
                .map(this::convertPayee)
                .collect(Collectors.toList());
        event.setPayees(payeeInfos);

        log.debug("Converted voucher {} to event with {} payees",
                voucher.getVoucherNumber(), payeeInfos.size());

        return event;
    }

    /**
     * Convert VoucherPayeeEntity to PayeeInfo
     */
    private VoucherEvent.PayeeInfo convertPayee(VoucherPayeeEntity payee) {
        VoucherEvent.PayeeInfo info = new VoucherEvent.PayeeInfo();
        info.setVendorId(payee.getVendor().getVendorId());
        info.setVendorName(payee.getVendor().getName());
        info.setAmount(payee.getAmount());
        info.setDescription(payee.getDescription());
        return info;
    }
}