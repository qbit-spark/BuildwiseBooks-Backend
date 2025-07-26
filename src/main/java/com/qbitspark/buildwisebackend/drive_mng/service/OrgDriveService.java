package com.qbitspark.buildwisebackend.drive_mng.service;

import com.qbitspark.buildwisebackend.drive_mng.enums.SystemFileType;
import com.qbitspark.buildwisebackend.drive_mng.payload.BatchUploadSyncResponse;
import com.qbitspark.buildwisebackend.drive_mng.payload.FileInfoResponse;
import com.qbitspark.buildwisebackend.globeadvice.exceptions.AccessDeniedException;
import com.qbitspark.buildwisebackend.globeadvice.exceptions.ItemNotFoundException;
import com.qbitspark.buildwisebackend.organisation_service.organisation_mng.entity.OrganisationEntity;
import com.qbitspark.buildwisebackend.projectmng_service.entity.ProjectEntity;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

public interface OrgDriveService {
    void initializeOrganisationDrive(OrganisationEntity organisation) throws ItemNotFoundException;
    void createProjectSystemFolder(OrganisationEntity organisation, ProjectEntity project) throws ItemNotFoundException;

    BatchUploadSyncResponse uploadFilesBatch(UUID organisationId, UUID projectId, SystemFileType type, List<MultipartFile> files) throws ItemNotFoundException, AccessDeniedException;

    List<FileInfoResponse> getFilesPreviewInfo(UUID organisationId, List<UUID> fileIds) throws ItemNotFoundException, AccessDeniedException;
}
