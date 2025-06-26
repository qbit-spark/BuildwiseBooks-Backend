package com.qbitspark.buildwisebackend.globe_api_client.filebox_client.controller;

import com.qbitspark.buildwisebackend.globe_api_client.filebox_client.services.FileBoxService;
import com.qbitspark.buildwisebackend.globe_api_client.payloads.CreateFolderRequest;
import com.qbitspark.buildwisebackend.globe_api_client.payloads.FileBoxFolderResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("api/v1/filebox")
@RequiredArgsConstructor
@Slf4j
public class FileBoxController {

    private final FileBoxService fileBoxService;

    @GetMapping("/folders")
    public ResponseEntity<List<FileBoxFolderResponse>> getRootFolders() {
        log.info("Getting root folders from FileBox");

            List<FileBoxFolderResponse> folders = fileBoxService.getRootFolders();
            return ResponseEntity.ok(folders);

    }

    @PostMapping("/folders")
    public ResponseEntity<FileBoxFolderResponse> createFolder(
            @RequestBody CreateFolderRequest createFolderRequest) {

        log.info("Creating folder: {}", createFolderRequest.getFolderName());

        try {
            FileBoxFolderResponse folder = fileBoxService.createFolder(createFolderRequest);
            return ResponseEntity.ok(folder);

        } catch (Exception e) {
            log.error("Failed to create folder: {}", createFolderRequest.getFolderName(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
}