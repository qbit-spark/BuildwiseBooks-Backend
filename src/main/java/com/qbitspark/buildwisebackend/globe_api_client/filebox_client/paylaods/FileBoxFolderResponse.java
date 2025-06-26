package com.qbitspark.buildwisebackend.globe_api_client.filebox_client.paylaods;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

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
    private Boolean hasSubFolders;
    private Integer fileCount;
}