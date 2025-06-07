package com.qbitspark.buildwisebackend.accounting_service.coa.repo;

import com.qbitspark.buildwisebackend.accounting_service.coa.entity.JournalEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface JournalEntryRepo extends JpaRepository<JournalEntry, UUID> {
}
