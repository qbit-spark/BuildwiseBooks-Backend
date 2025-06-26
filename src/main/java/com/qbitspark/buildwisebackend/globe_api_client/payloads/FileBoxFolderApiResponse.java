package com.qbitspark.buildwisebackend.globe_api_client.filebox_client.dto;

import com.qbitspark.buildwisebackend.globe_api_client.payloads.FileBoxFolderResponse;
import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileBoxFolderApiResponse {
    private Boolean success;
    private String message;
    private FileBoxFolderResponse data;
}