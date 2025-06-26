package com.qbitspark.buildwisebackend.accounting_service.documentflow.invoice.repo;

import com.qbitspark.buildwisebackend.accounting_service.documentflow.invoice.entity.InvoiceAttachmentEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface InvoiceAttachmentRepo extends JpaRepository<InvoiceAttachmentEntity, UUID> {
    List<InvoiceAttachmentEntity> findByInvoiceId(UUID invoiceId);
}
