package com.qbitspark.buildwisebackend.accounting_service.documentflow.voucher.paylaod;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class VoucherAttachmentRequest {
    @NotBlank(message = "Filename is required")
    private String filename;

    @NotBlank(message = "File extension is required")
    private String fileExtension;

    @NotBlank(message = "File hash is required")
    private String fileHash;

    @NotBlank(message = "System directory is required")
    private String systemDirectory;

    @NotBlank(message = "Original filename is required")
    private String originalFilename;

    @NotNull(message = "File size is required")
    @Min(value = 1, message = "File size must be greater than 0")
    private Long fileSize;

    @NotBlank(message = "File path URL is required")
    private String filePathUrl;
}