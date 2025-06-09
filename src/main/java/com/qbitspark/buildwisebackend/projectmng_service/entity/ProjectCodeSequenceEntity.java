package com.qbitspark.buildwisebackend.projectmng_service.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor

@Entity
@Table(name = "project_code_sequences", indexes = {
        @Index(name = "idx_org_id", columnList = "organisation_id", unique = true),
        @Index(name = "idx_sequence_updated", columnList = "current_sequence, updated_at"),
        @Index(name = "idx_updated_at", columnList = "updated_at")
})
public class ProjectCodeSequenceEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "organisation_id", nullable = false, unique = true)
    private UUID organisationId;

    @Column(name = "current_sequence", nullable = false)
    private Integer currentSequence = 0;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}