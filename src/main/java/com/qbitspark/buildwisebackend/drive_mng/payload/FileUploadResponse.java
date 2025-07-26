package com.qbitspark.buildwisebackend.drive_mng.payload;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class FileUploadResponse {
    private UUID fileId;
    private String fileName;
    private UUID folderId;
    private String folderPath;
    private Long fileSize;
    private String sizeFormatted;
    private String mimeType;
    private LocalDateTime uploadedAt;
    private String projectCode;

    // New fields for URLs
    private String downloadUrl;
    private String previewUrl;
}