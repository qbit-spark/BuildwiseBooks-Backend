package com.qbitspark.buildwisebackend.accounting_service.documentflow.repo;

import com.qbitspark.buildwisebackend.accounting_service.documentflow.entity.InvoiceDocEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface InvoiceDocRepo extends JpaRepository<InvoiceDocEntity, UUID> {
}
