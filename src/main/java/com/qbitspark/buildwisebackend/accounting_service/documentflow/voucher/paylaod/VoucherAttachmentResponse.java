package com.qbitspark.buildwisebackend.accounting_service.documentflow.voucher.paylaod;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class VoucherAttachmentResponse {
    private UUID id;
    private String filename;
    private String fileExtension;
    private String fileHash;
    private String systemDirectory;
    private String originalFilename;
    private Long fileSize;
    private String filePathUrl;
    private LocalDateTime uploadedAt;
}