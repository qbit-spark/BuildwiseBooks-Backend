package com.qbitspark.buildwisebackend.accounting_service.repo;

import com.qbitspark.buildwisebackend.accounting_service.entity.JournalEntryLine;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface JournalEntryLineRepo extends JpaRepository<JournalEntryLine, UUID> {
}
