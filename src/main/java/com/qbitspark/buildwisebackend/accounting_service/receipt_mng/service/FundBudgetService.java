package com.qbitspark.buildwisebackend.accounting_service.receipt_mng.service;

import com.qbitspark.buildwisebackend.globeadvice.exceptions.AccessDeniedException;
import com.qbitspark.buildwisebackend.globeadvice.exceptions.ItemNotFoundException;

import java.util.UUID;

public interface FundBudgetService {
    void fundBudget(UUID allocationId) throws ItemNotFoundException, AccessDeniedException;
}
