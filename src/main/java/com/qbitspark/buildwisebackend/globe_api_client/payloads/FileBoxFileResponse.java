package com.qbitspark.buildwisebackend.globe_api_client.payloads;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileBoxFileResponse {
    private UUID fileId;
    private String fileName;
    private UUID folderId;
    private String folderPath;
    private Long fileSize;
    private String mimeType;
    private String extension;
    private String scanStatus;
    private LocalDateTime uploadedAt;
    private Boolean canPreview;
    private Boolean canDownload;
}