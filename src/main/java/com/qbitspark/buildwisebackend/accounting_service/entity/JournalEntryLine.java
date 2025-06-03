package com.qbitspark.buildwisebackend.accounting_service.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "journal_entry_line")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class JournalEntryLine {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "debit_amount", precision = 19, scale = 2)
    private BigDecimal debitAmount;

    @Column(name = "credit_amount", precision = 19, scale = 2)
    private BigDecimal creditAmount;

    /**
     * Many-to-One relationship with JournalEntry
     * Each journal entry line belongs to one journal entry
     * Each journal entry can have multiple lines
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "journal_entry_id", nullable = false)
    private JournalEntry journalEntry;

    /**
     * Many-to-One relationship with ChartOfAccounts
     * Each journal entry line references one account
     * Each account can be used in multiple journal entry lines
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private ChartOfAccounts account;

    @Column(name = "line_description", columnDefinition = "TEXT")
    private String description;
}