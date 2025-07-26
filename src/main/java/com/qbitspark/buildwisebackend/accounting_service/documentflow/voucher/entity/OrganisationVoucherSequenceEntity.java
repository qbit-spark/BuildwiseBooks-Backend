package com.qbitspark.buildwisebackend.accounting_service.documentflow.voucher.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "organisation_voucher_sequences", indexes = {
        @Index(name = "idx_org_voucher_seq_org", columnList = "organisation_id", unique = true),
        @Index(name = "idx_org_voucher_seq_sequence_updated", columnList = "current_sequence, updated_at"),
        @Index(name = "idx_org_voucher_seq_updated_at", columnList = "updated_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrganisationVoucherSequenceEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "organisation_id", nullable = false, unique = true)
    private UUID organisationId;

    @Column(name = "current_sequence", nullable = false)
    @Builder.Default
    private Integer currentSequence = 0;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}