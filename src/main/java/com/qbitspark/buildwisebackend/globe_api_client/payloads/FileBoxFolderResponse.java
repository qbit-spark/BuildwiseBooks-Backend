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
public class FileBoxFolderResponse {
    private UUID folderId;
    private String folderName;
    private UUID parentFolderId;
    private String fullPath;
    private LocalDateTime createdAt;
}