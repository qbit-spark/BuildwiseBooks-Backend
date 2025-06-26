package com.qbitspark.buildwisebackend.globe_api_client.filebox_client.services;

import com.qbitspark.buildwisebackend.globe_api_client.payloads.CreateFolderRequest;
import com.qbitspark.buildwisebackend.globe_api_client.payloads.FileBoxFileResponse;
import com.qbitspark.buildwisebackend.globe_api_client.payloads.FileBoxFolderContentsResponse;
import com.qbitspark.buildwisebackend.globe_api_client.payloads.FileBoxFolderResponse;
import org.springframework.util.MultiValueMap;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

public interface FileBoxService {

    List<FileBoxFolderResponse> getRootFolders();

    FileBoxFolderResponse createFolder(CreateFolderRequest createFolderRequest);
}