package com.qbitspark.buildwisebackend.globe_api_client.payloads;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileBoxFolderContentsResponse {
    private UUID folderId;
    private String folderName;
    private String fullPath;
    private List<FileBoxFileResponse> files;
    private List<FileBoxFolderResponse> folders;
    private Integer totalFiles;
    private Integer totalFolders;
}
