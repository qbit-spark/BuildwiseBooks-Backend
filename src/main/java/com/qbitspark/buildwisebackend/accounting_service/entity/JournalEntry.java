package com.qbitspark.buildwisebackend.accounting_service.entity;

import com.qbitspark.buildwisebackend.accounting_service.enums.TransactionLevel;
import com.qbitspark.buildwisebackend.organisation_service.organisation_mng.entity.OrganisationEntity;
import com.qbitspark.buildwisebackend.projectmng_service.entity.ProjectEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "journal_entries")
public class JournalEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "transaction_date_time", nullable = false)
    private LocalDateTime transactionDateTime;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "reference_number", unique = true)
    private String referenceNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organisation_id", nullable = false)
    private OrganisationEntity organisation;

    @Enumerated(EnumType.STRING)
    private TransactionLevel transactionLevel;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id")
    private ProjectEntity project;

    @OneToMany(mappedBy = "journalEntry", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<JournalEntryLine> journalEntryLines;

}
