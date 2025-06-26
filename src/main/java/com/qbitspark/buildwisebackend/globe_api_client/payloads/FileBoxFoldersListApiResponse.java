package com.qbitspark.buildwisebackend.globe_api_client.payloads;

import com.qbitspark.buildwisebackend.globe_api_client.payloads.FileBoxFolderResponse;
import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileBoxFoldersListApiResponse {
    private Boolean success;
    private String message;
    private List<FileBoxFolderResponse> data;
}