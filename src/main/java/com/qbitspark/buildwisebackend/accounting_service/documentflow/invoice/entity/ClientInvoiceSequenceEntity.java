package com.qbitspark.buildwisebackend.accounting_service.documentflow.invoice.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "client_invoice_sequences", indexes = {
        @Index(name = "idx_client_inv_seq_org_client", columnList = "organisation_id, client_id", unique = true),
        @Index(name = "idx_client_inv_seq_sequence_updated", columnList = "current_sequence, updated_at"),
        @Index(name = "idx_client_inv_seq_updated_at", columnList = "updated_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClientInvoiceSequenceEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "organisation_id", nullable = false)
    private UUID organisationId;

    @Column(name = "client_id", nullable = false)
    private UUID clientId;

    @Column(name = "current_sequence", nullable = false)
    @Builder.Default
    private Integer currentSequence = 0;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}