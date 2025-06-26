package com.qbitspark.buildwisebackend.accounting_service.documentflow.invoice.entity;


import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "invoice_attachments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InvoiceAttachmentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_id", nullable = false)
    private InvoiceDocEntity invoice;

    @Column(name = "filebox_file_id", nullable = false)
    private UUID fileboxFileId;

    @Column(name = "filebox_folder_path", nullable = false)
    private String fileboxFolderPath; // invoices_attachments/INV-2025-001

    @Column(name = "original_filename", nullable = false)
    private String originalFilename;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "mime_type")
    private String mimeType;

    @Column(name = "file_extension")
    private String fileExtension;

    @Column(name = "attachment_type")
    private String attachmentType; // Optional: "invoice", "receipt", "supporting", etc.

    @Column(name = "description")
    private String description;

    @Column(name = "scan_status")
    private String scanStatus; // CLEAN, INFECTED, PENDING, FAILED

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "uploaded_by")
    private UUID uploadedBy;

    @Column(name = "updated_by")
    private UUID updatedBy;

}