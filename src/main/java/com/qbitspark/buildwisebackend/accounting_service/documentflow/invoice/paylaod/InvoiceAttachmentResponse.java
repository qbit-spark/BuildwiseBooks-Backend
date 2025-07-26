package com.qbitspark.buildwisebackend.accounting_service.documentflow.invoice.paylaod;

import lombok.Data;

import java.util.UUID;

@Data
public class InvoiceAttachmentResponse {
    private UUID fileId;
    private String fileName;
    private Long fileSize;
    private String sizeFormatted;
    private String mimeType;
    private String downloadUrl;
    private String previewUrl;
    private boolean canPreview;
}
