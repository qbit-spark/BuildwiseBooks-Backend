package com.qbitspark.buildwisebackend.accounting_service.documentflow.voucher.paylaod;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class VoucherAttachmentResponse {
    private UUID fileId;
    private String fileName;
    private Long fileSize;
    private String sizeFormatted;
    private String mimeType;
    private String downloadUrl;
    private String previewUrl;
    private boolean canPreview;
}