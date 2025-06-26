package com.qbitspark.buildwisebackend.globe_api_client.payloads;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateFolderRequest {
    private String folderName;
    private UUID parentFolderId; // null for root folder
}