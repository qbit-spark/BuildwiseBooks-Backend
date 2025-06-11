package com.qbitspark.buildwisebackend.accounting_service.documentflow.voucher.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "voucher_attachments_tb",
        indexes = {
                @Index(name = "idx_attachment_voucher", columnList = "voucher_id"),
                @Index(name = "idx_attachment_file_hash", columnList = "file_hash"),
                @Index(name = "idx_attachment_system_directory", columnList = "system_directory"),
                @Index(name = "idx_attachment_uploaded_at", columnList = "uploaded_at"),
                @Index(name = "idx_attachment_filename", columnList = "filename"),
                @Index(name = "idx_attachment_voucher_uploaded", columnList = "voucher_id, uploaded_at"),
                @Index(name = "idx_attachment_file_extension", columnList = "file_extension")
        })
public class VoucherAttachmentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "voucher_id", nullable = false)
    private VoucherEntity voucher;

    @Column(name = "filename", nullable = false, columnDefinition = "TEXT")
    private String filename;

    @Column(name = "file_extension", nullable = false, columnDefinition = "TEXT")
    private String fileExtension;

    @Column(name = "file_hash", nullable = false, columnDefinition = "TEXT")
    private String fileHash;

    @Column(name = "system_directory", nullable = false, columnDefinition = "TEXT")
    private String systemDirectory;

    @Column(name = "original_filename", nullable = false, columnDefinition = "TEXT")
    private String originalFilename;

    @Column(name = "file_size", nullable = false)
    private Long fileSize;

    @Column(name = "file_path", nullable = false, columnDefinition = "TEXT")
    private String filePathUrl;

    @Column(name = "uploaded_at", nullable = false)
    private LocalDateTime uploadedAt;

    @PrePersist
    protected void onCreate() {
        uploadedAt = LocalDateTime.now();
    }
}