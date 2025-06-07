package com.qbitspark.buildwisebackend.accounting_service.coa.repo;

import com.qbitspark.buildwisebackend.accounting_service.coa.entity.JournalEntryLine;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface JournalEntryLineRepo extends JpaRepository<JournalEntryLine, UUID> {
}
