package com.qbitspark.buildwisebackend.drive_mng.controller;


import com.qbitspark.buildwisebackend.drive_mng.enums.SystemFileType;
import com.qbitspark.buildwisebackend.drive_mng.payload.BatchUploadSyncResponse;
import com.qbitspark.buildwisebackend.drive_mng.payload.FileInfoRequest;
import com.qbitspark.buildwisebackend.drive_mng.payload.FileInfoResponse;
import com.qbitspark.buildwisebackend.drive_mng.service.OrgDriveService;
import com.qbitspark.buildwisebackend.globeadvice.exceptions.AccessDeniedException;
import com.qbitspark.buildwisebackend.globeadvice.exceptions.ItemNotFoundException;
import com.qbitspark.buildwisebackend.globeresponsebody.GlobeSuccessResponseBuilder;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/drive/{organisationId}/attachments")
@RequiredArgsConstructor
@Slf4j
public class OrgDriveController {

    private final OrgDriveService orgDriveService;

    @PostMapping("/batch-upload")
    public ResponseEntity<GlobeSuccessResponseBuilder> batchUploadFiles(
            @PathVariable("organisationId") UUID organisationId,
            @RequestParam("projectId") UUID projectId,
            @RequestParam("type") SystemFileType type,
            @RequestParam("files") List<MultipartFile> files) throws ItemNotFoundException, AccessDeniedException {

        BatchUploadSyncResponse response = orgDriveService.uploadFilesBatch(organisationId, projectId, type, files);

        return ResponseEntity.ok(GlobeSuccessResponseBuilder.success("Attachments uploaded successfully", response));
    }

    @PostMapping("/file-info")
    public ResponseEntity<GlobeSuccessResponseBuilder> getFilesPreviewInfo(
            @PathVariable("organisationId") UUID organisationId,
            @RequestBody @Valid FileInfoRequest request) throws ItemNotFoundException, AccessDeniedException {

        List<FileInfoResponse> previewResponses = orgDriveService.getFilesPreviewInfo(organisationId, request.getFileIds());

        return ResponseEntity.ok(GlobeSuccessResponseBuilder.success("Files preview info retrieved successfully", previewResponses));
    }

}
