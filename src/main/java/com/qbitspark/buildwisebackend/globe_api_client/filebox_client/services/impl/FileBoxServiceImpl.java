package com.qbitspark.buildwisebackend.globe_api_client.filebox_client.services.impl;

import com.qbitspark.buildwisebackend.globe_api_client.GlobalApiClientGate;
import com.qbitspark.buildwisebackend.globe_api_client.filebox_client.dto.FileBoxFolderApiResponse;
import com.qbitspark.buildwisebackend.globe_api_client.filebox_client.services.FileBoxService;
import com.qbitspark.buildwisebackend.globe_api_client.payloads.ApiResponse;
import com.qbitspark.buildwisebackend.globe_api_client.payloads.CreateFolderRequest;
import com.qbitspark.buildwisebackend.globe_api_client.payloads.FileBoxFolderResponse;
import com.qbitspark.buildwisebackend.globe_api_client.payloads.FileBoxFoldersListApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class FileBoxServiceImpl implements FileBoxService {

    private final GlobalApiClientGate apiClient;

    @Value("${app.filebox.base-url}")
    private String baseUrl;

    @Value("${app.filebox.access-token}")
    private String accessToken;

    @Override
    public List<FileBoxFolderResponse> getRootFolders() {
        log.info("Getting root folders from FileBox");

        String url = baseUrl + "/folders";

        ApiResponse<FileBoxFoldersListApiResponse> response = apiClient.getWithAuth(
                url,
                accessToken,
                FileBoxFoldersListApiResponse.class
        );

        if (!response.isSuccess()) {
            log.error("Failed to get root folders: {}", response.getErrorMessage());
            throw new RuntimeException("Failed to get root folders: " + response.getErrorMessage());
        }

        FileBoxFoldersListApiResponse fileBoxResponse = response.getData();

        if (!fileBoxResponse.getSuccess()) {
            log.error("FileBox API returned error: {}", fileBoxResponse.getMessage());
            throw new RuntimeException("FileBox API error: " + fileBoxResponse.getMessage());
        }

        log.info("Successfully retrieved {} root folders", fileBoxResponse.getData().size());
        return fileBoxResponse.getData();
    }

    @Override
    public FileBoxFolderResponse createFolder(CreateFolderRequest createFolderRequest) {
        log.info("Creating folder: {} with parent: {}",
                createFolderRequest.getFolderName(),
                createFolderRequest.getParentFolderId());

        String url = baseUrl + "folders";

        ApiResponse<FileBoxFolderApiResponse> response = apiClient.postWithAuth(
                url,
                createFolderRequest,
                accessToken,
                FileBoxFolderApiResponse.class
        );

        if (!response.isSuccess()) {
            log.error("Failed to create folder: {}", response.getErrorMessage());
            throw new RuntimeException("Failed to create folder: " + response.getErrorMessage());
        }

        FileBoxFolderApiResponse fileBoxResponse = response.getData();

        if (!fileBoxResponse.getSuccess()) {
            log.error("FileBox API returned error: {}", fileBoxResponse.getMessage());
            throw new RuntimeException("FileBox API error: " + fileBoxResponse.getMessage());
        }

        log.info("Successfully created folder: {} with ID: {}",
                createFolderRequest.getFolderName(),
                fileBoxResponse.getData().getFolderId());

        return fileBoxResponse.getData();
    }

}