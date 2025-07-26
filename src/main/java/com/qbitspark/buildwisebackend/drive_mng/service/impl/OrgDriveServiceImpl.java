package com.qbitspark.buildwisebackend.drive_mng.service.impl;

import com.qbitspark.buildwisebackend.authentication_service.repo.AccountRepo;
import com.qbitspark.buildwisebackend.authentication_service.entity.AccountEntity;
import com.qbitspark.buildwisebackend.drive_mng.entity.OrgFileEntity;
import com.qbitspark.buildwisebackend.drive_mng.entity.OrgFolderEntity;
import com.qbitspark.buildwisebackend.drive_mng.enums.SystemFileType;
import com.qbitspark.buildwisebackend.drive_mng.payload.BatchUploadSyncResponse;
import com.qbitspark.buildwisebackend.drive_mng.payload.FileInfoResponse;
import com.qbitspark.buildwisebackend.drive_mng.payload.FileUploadResponse;
import com.qbitspark.buildwisebackend.drive_mng.repo.OrgFileRepo;
import com.qbitspark.buildwisebackend.drive_mng.repo.OrgFolderRepository;
import com.qbitspark.buildwisebackend.drive_mng.service.OrgDriveService;
import com.qbitspark.buildwisebackend.globeadvice.exceptions.AccessDeniedException;
import com.qbitspark.buildwisebackend.globeadvice.exceptions.ItemNotFoundException;
import com.qbitspark.buildwisebackend.minio_service.service.MinioService;
import com.qbitspark.buildwisebackend.organisation_service.organisation_mng.entity.OrganisationEntity;
import com.qbitspark.buildwisebackend.organisation_service.organisation_mng.repo.OrganisationRepo;
import com.qbitspark.buildwisebackend.organisation_service.orgnisation_members_mng.entities.OrganisationMember;
import com.qbitspark.buildwisebackend.organisation_service.orgnisation_members_mng.enums.MemberStatus;
import com.qbitspark.buildwisebackend.organisation_service.orgnisation_members_mng.repo.OrganisationMemberRepo;
import com.qbitspark.buildwisebackend.organisation_service.roles_mng.service.PermissionCheckerService;
import com.qbitspark.buildwisebackend.projectmng_service.entity.ProjectEntity;
import com.qbitspark.buildwisebackend.projectmng_service.entity.ProjectTeamMemberEntity;
import com.qbitspark.buildwisebackend.projectmng_service.repo.ProjectRepo;
import com.qbitspark.buildwisebackend.projectmng_service.repo.ProjectTeamMemberRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrgDriveServiceImpl implements OrgDriveService {

    private final MinioService minioService;
    private final OrgFolderRepository orgFolderRepository;
    private final AccountRepo accountRepo;
    private final OrgFileRepo orgFileRepo;
    private final OrganisationMemberRepo organisationMemberRepo;
    private final ProjectTeamMemberRepo projectTeamMemberRepo;
    private final OrganisationRepo organisationRepo;
    private final ProjectRepo projectRepo;
    private final PermissionCheckerService permissionChecker;

    @Override
    public void initializeOrganisationDrive(OrganisationEntity organisation) throws ItemNotFoundException {

        UUID organisationId = organisation.getOrganisationId();

        if (minioService.bucketExists(organisationId)) {
            return;
        }

        minioService.createOrganisationBucket(organisationId);
        createDefaultFolders(organisation);
        createSystemFolders(organisation);
    }

    @Override
    public void createProjectSystemFolder(OrganisationEntity organisation, ProjectEntity project) throws ItemNotFoundException {
        AccountEntity createdBy = getAuthenticatedAccount();

        OrgFolderEntity systemFilesFolder = orgFolderRepository.findByFolderNameAndOrganisationAndParentFolderIsNull("system_files", organisation);

        if (systemFilesFolder == null) {
            throw new ItemNotFoundException("system_files folder not found for organization");
        }

        String mainFolderPath = "system_files/" + project.getProjectCode();
        minioService.createFolderStructure(organisation.getOrganisationId(), mainFolderPath);

        OrgFolderEntity projectFolder = new OrgFolderEntity();
        projectFolder.setFolderName(project.getProjectCode());
        projectFolder.setCreator(createdBy);
        projectFolder.setOrganisation(organisation);
        projectFolder.setParentFolder(systemFilesFolder);
        projectFolder.setIsDeleted(false);
        projectFolder.setProject(project);
        OrgFolderEntity savedProjectFolder = orgFolderRepository.save(projectFolder);

        String[] subfolders = {"voucher", "invoice", "payment", "budget", "others", "vendors"};

        for (String subfolderName : subfolders) {
            String subfolderPath = mainFolderPath + "/" + subfolderName;
            minioService.createFolderStructure(organisation.getOrganisationId(), subfolderPath);

            OrgFolderEntity subfolder = new OrgFolderEntity();
            subfolder.setFolderName(subfolderName);
            subfolder.setCreator(createdBy);
            subfolder.setOrganisation(organisation);
            subfolder.setParentFolder(savedProjectFolder);
            subfolder.setIsDeleted(false);
            orgFolderRepository.save(subfolder);
        }
    }


    @Override
    public BatchUploadSyncResponse uploadFilesBatch(UUID organisationId, UUID projectId,
                                                    SystemFileType type, List<MultipartFile> files) throws ItemNotFoundException, AccessDeniedException {

        AccountEntity currentUser = getAuthenticatedAccount();

        ProjectEntity project = projectRepo.findById(projectId)
                .orElseThrow(() -> new ItemNotFoundException("Project not found"));

        OrganisationEntity organisation = organisationRepo.findById(organisationId)
                .orElseThrow(() -> new ItemNotFoundException("Organisation not found"));

        validateProject(project, organisation);

        OrganisationMember member = validateOrganisationMemberAccess(currentUser, organisation);

        permissionChecker.checkMemberPermission(member, "DRIVE", "uploadFiles");

        List<FileUploadResponse> uploadedFiles = new ArrayList<>();

        List<BatchUploadSyncResponse.FailedUpload> failures = new ArrayList<>();


        if (files == null || files.isEmpty()) {
            throw new IllegalArgumentException("No files provided for upload");
        }

        if (files.size() > 50) {
            throw new IllegalArgumentException("Maximum 50 files allowed per batch upload");
        }


        String targetPath = buildSystemPath(project.getProjectCode(), type);
        OrgFolderEntity targetFolder = findTargetFolder(organisation, project.getProjectCode(), type);

        if (targetFolder == null) {
            throw new ItemNotFoundException("Target folder not found: " + targetPath);
        }


        for (MultipartFile file : files) {
            try {
                processeSingleFile(file, organisation, project, targetFolder, targetPath, currentUser, uploadedFiles);
            } catch (Exception e) {
                failures.add(BatchUploadSyncResponse.FailedUpload.builder()
                        .fileName(file.getOriginalFilename())
                        .reason(e.getMessage())
                        .fileSize(file.getSize())
                        .build());
            }
        }


        return buildBatchResponse(files.size(), uploadedFiles, failures);
    }


    @Override
    public List<FileInfoResponse> getFilesPreviewInfo(UUID organisationId, List<UUID> fileIds)
            throws ItemNotFoundException, AccessDeniedException {

        AccountEntity currentUser = getAuthenticatedAccount();

        OrganisationEntity organisation = organisationRepo.findById(organisationId)
                .orElseThrow(() -> new ItemNotFoundException("Organisation not found"));


        OrganisationMember member = validateOrganisationMemberAccess(currentUser, organisation);

        permissionChecker.checkMemberPermission(member, "DRIVE", "viewFiles");


        if (fileIds == null || fileIds.isEmpty()) {
            throw new IllegalArgumentException("File IDs list cannot be empty");
        }

        if (fileIds.size() > 100) {
            throw new IllegalArgumentException("Maximum 100 files allowed per preview request");
        }

        List<FileInfoResponse> responses = new ArrayList<>();

        for (UUID fileId : fileIds) {
            try {
                FileInfoResponse fileInfo = processFilePreview(fileId, organisation, currentUser);
                responses.add(fileInfo);
            } catch (Exception e) {
                // Add a basic response indicating failure
                responses.add(FileInfoResponse.builder()
                        .id(fileId)
                        .name("Unknown")
                        .canPreview(false)
                        .canDownload(false)
                        .category("error")
                        .build());
            }
        }

        return responses;
    }


    private void createDefaultFolders(OrganisationEntity organisation) throws ItemNotFoundException {
        AccountEntity createdBy = getAuthenticatedAccount();

        minioService.createFolderStructure(organisation.getOrganisationId(), "global_files");
        OrgFolderEntity globalFolder = new OrgFolderEntity();
        globalFolder.setFolderName("global_files");
        globalFolder.setOrganisation(organisation);
        globalFolder.setParentFolder(null);
        globalFolder.setCreator(createdBy);
        globalFolder.setIsDeleted(false);
        globalFolder = orgFolderRepository.save(globalFolder);

        String[] folders = {"Documents", "Projects", "Templates", "Shared", "Archive"};
        for (String folderName : folders) {
            minioService.createFolderStructure(organisation.getOrganisationId(), "global_files/" + folderName);
            OrgFolderEntity subfolder = new OrgFolderEntity();
            subfolder.setFolderName(folderName);
            subfolder.setOrganisation(organisation);
            subfolder.setParentFolder(globalFolder);
            subfolder.setCreator(createdBy);
            subfolder.setIsDeleted(false);
            orgFolderRepository.save(subfolder);
        }
    }

    private void createSystemFolders(OrganisationEntity organisation) throws ItemNotFoundException {
        AccountEntity createdBy = getAuthenticatedAccount();

        minioService.createFolderStructure(organisation.getOrganisationId(), "system_files");
        OrgFolderEntity systemFolder = new OrgFolderEntity();
        systemFolder.setFolderName("system_files");
        systemFolder.setOrganisation(organisation);
        systemFolder.setCreator(createdBy);
        systemFolder.setParentFolder(null);
        systemFolder.setIsDeleted(false);
        orgFolderRepository.save(systemFolder);
    }


    private AccountEntity getAuthenticatedAccount() throws ItemNotFoundException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return extractAccount(authentication);
    }


    private AccountEntity extractAccount(Authentication authentication) throws ItemNotFoundException {
        if (authentication != null && authentication.isAuthenticated()) {
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            String userName = userDetails.getUsername();

            Optional<AccountEntity> userOptional = accountRepo.findByUserName(userName);
            if (userOptional.isPresent()) {
                return userOptional.get();
            } else {
                throw new ItemNotFoundException("User with given userName does not exist");
            }
        } else {
            throw new ItemNotFoundException("User is not authenticated");
        }
    }


    private void processeSingleFile(MultipartFile file, OrganisationEntity organisation, ProjectEntity project,
                                    OrgFolderEntity targetFolder, String targetPath, AccountEntity uploadedBy,
                                    List<FileUploadResponse> uploadedFiles) {

        validateFile(file);

        String fileName = generateUniqueFileName(file.getOriginalFilename());

        String minioKey = minioService.uploadFile(
                organisation.getOrganisationId(),
                targetPath,
                fileName,
                file
        );

        OrgFileEntity fileEntity = new OrgFileEntity();
        fileEntity.setFileName(fileName);
        fileEntity.setMinioKey(minioKey);
        fileEntity.setFolder(targetFolder);
        fileEntity.setFileSize(file.getSize());
        fileEntity.setMimeType(file.getContentType());
        fileEntity.setOrganisation(organisation);
        fileEntity.setProject(project);
        fileEntity.setCreator(uploadedBy);
        fileEntity.setIsDeleted(false);

        OrgFileEntity savedFile = orgFileRepo.save(fileEntity);

        // Generate URLs
        String downloadUrl = generateDownloadUrlForFile(organisation.getOrganisationId(), minioKey);
        String previewUrl = null;

        // Only generate preview URL if file can be previewed
        String extension = extractFileExtension(fileName);
        if (canPreviewFile(file.getContentType(), extension)) {
            previewUrl = downloadUrl; // Same URL for preview
        }

        uploadedFiles.add(FileUploadResponse.builder()
                .fileId(savedFile.getFileId())
                .fileName(savedFile.getFileName())
                .fileSize(savedFile.getFileSize())
                .sizeFormatted(formatFileSize(savedFile.getFileSize()))
                .mimeType(savedFile.getMimeType())
                .folderPath(targetPath)
                .folderId(targetFolder.getFolderId())
                .projectCode(project.getProjectCode())
                .uploadedAt(savedFile.getCreatedAt())
                .downloadUrl(downloadUrl)
                .previewUrl(previewUrl)
                .build());
    }


    private void validateFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }

        if (file.getOriginalFilename() == null || file.getOriginalFilename().trim().isEmpty()) {
            throw new IllegalArgumentException("File name is required");
        }

        // Size limit check (100MB per file)
        long maxSize = 100 * 1024 * 1024;
        if (file.getSize() > maxSize) {
            throw new IllegalArgumentException("File size exceeds maximum limit of 100MB");
        }

        // Content type validation
        String contentType = file.getContentType();
        if (contentType != null && contentType.toLowerCase().contains("executable")) {
            throw new IllegalArgumentException("Executable files are not allowed");
        }
    }

    private String buildSystemPath(String projectCode, SystemFileType type) {
        if (type == SystemFileType.GLOBAL) {
            return "global_files";
        }
        return "system_files/" + projectCode + "/" + type.getFolderName();
    }

    private OrgFolderEntity findTargetFolder(OrganisationEntity organisation, String projectCode, SystemFileType type) {
        if (type == SystemFileType.GLOBAL) {
            return orgFolderRepository.findByFolderNameAndOrganisationAndParentFolderIsNull("global_files", organisation);
        }

        OrgFolderEntity systemFilesFolder = orgFolderRepository.findByFolderNameAndOrganisationAndParentFolderIsNull("system_files", organisation);
        if (systemFilesFolder == null) return null;


        OrgFolderEntity projectFolder = orgFolderRepository.findByFolderNameAndOrganisationAndParentFolder(projectCode, organisation, systemFilesFolder);
        if (projectFolder == null) return null;

        return orgFolderRepository.findByFolderNameAndOrganisationAndParentFolder(type.getFolderName(), organisation, projectFolder);
    }


    private String generateUniqueFileName(String originalFilename) {
        String timestamp = String.valueOf(System.currentTimeMillis());
        int lastDotIndex = originalFilename.lastIndexOf('.');

        if (lastDotIndex != -1) {
            String name = originalFilename.substring(0, lastDotIndex);
            String extension = originalFilename.substring(lastDotIndex);
            return name + "_" + timestamp + extension;
        }

        return originalFilename + "_" + timestamp;
    }

    private BatchUploadSyncResponse buildBatchResponse(int totalRequested,
                                                       List<FileUploadResponse> uploadedFiles,
                                                       List<BatchUploadSyncResponse.FailedUpload> failures) {

        int successful = uploadedFiles.size();
        int failed = failures.size();

        String summary;
        if (failed == 0) {
            summary = String.format("Successfully uploaded %d files", successful);
        } else if (successful == 0) {
            summary = String.format("Failed to upload all %d files", totalRequested);
        } else {
            summary = String.format("Uploaded %d files successfully, %d failed", successful, failed);
        }

        return BatchUploadSyncResponse.builder()
                .totalFilesRequested(totalRequested)
                .successfulUploads(successful)
                .failedUploads(failed)
                .uploadedFiles(uploadedFiles)
                .failures(failures)
                .summary(summary)
                .build();
    }

    private OrganisationMember validateOrganisationMemberAccess(AccountEntity account, OrganisationEntity organisation) throws ItemNotFoundException {
        OrganisationMember member = organisationMemberRepo.findByAccountAndOrganisation(account, organisation)
                .orElseThrow(() -> new ItemNotFoundException("Member is not belong to this organisation"));

        if (member.getStatus() != MemberStatus.ACTIVE) {
            throw new ItemNotFoundException("Member is not active");
        }

        return member;
    }

    private void validateProject(ProjectEntity project, OrganisationEntity organisation) throws ItemNotFoundException {
        if (!project.getOrganisation().getOrganisationId().equals(organisation.getOrganisationId())) {
            throw new ItemNotFoundException("Project does not belong to this organisation");
        }
    }

    private void validateProjectMemberPermissions(AccountEntity account, ProjectEntity project) throws ItemNotFoundException, AccessDeniedException {

        OrganisationMember organisationMember = organisationMemberRepo.findByAccountAndOrganisation(account, project.getOrganisation())
                .orElseThrow(() -> new ItemNotFoundException("Member is not found in organisation"));

        if (organisationMember.getStatus() != MemberStatus.ACTIVE) {
            throw new ItemNotFoundException("Member is not active");
        }

        ProjectTeamMemberEntity projectTeamMember = projectTeamMemberRepo.findByOrganisationMemberAndProject(organisationMember, project)
                .orElseThrow(() -> new ItemNotFoundException("Project team member not found"));



    }

    private FileInfoResponse processFilePreview(UUID fileId, OrganisationEntity organisation, AccountEntity requestedBy)
            throws ItemNotFoundException, AccessDeniedException {

        OrgFileEntity file = orgFileRepo.findById(fileId)
                .orElseThrow(() -> new ItemNotFoundException("File not found with ID: " + fileId));

        // Validate file belongs to an organisation
        if (!file.getOrganisation().getOrganisationId().equals(organisation.getOrganisationId())) {
            throw new AccessDeniedException("File does not belong to this organisation");
        }

        // Check if file is deleted
        if (Boolean.TRUE.equals(file.getIsDeleted())) {
            throw new ItemNotFoundException("File has been deleted");
        }

        // Validate project access if file belongs to a project
        if (file.getProject() != null) {
            validateProjectMemberPermissions(requestedBy, file.getProject()); // Allow all project members to preview
        }

        return buildFileInfoResponse(file);
    }


    private String extractFileExtension(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return "";
        }

        int lastDotIndex = fileName.lastIndexOf('.');
        if (lastDotIndex > 0 && lastDotIndex < fileName.length() - 1) {
            return fileName.substring(lastDotIndex + 1).toLowerCase();
        }

        return "";
    }

    private String formatFileSize(Long sizeInBytes) {
        if (sizeInBytes == null || sizeInBytes == 0) {
            return "0 B";
        }

        String[] units = {"B", "KB", "MB", "GB", "TB"};
        int unitIndex = 0;
        double size = sizeInBytes.doubleValue();

        while (size >= 1024 && unitIndex < units.length - 1) {
            size /= 1024;
            unitIndex++;
        }

        return String.format("%.1f %s", size, units[unitIndex]);
    }

    private String determineFileCategory(String mimeType, String extension) {
        if (mimeType == null) {
            mimeType = "";
        }

        // Image files
        if (mimeType.startsWith("image/") ||
                List.of("jpg", "jpeg", "png", "gif", "bmp", "webp", "svg").contains(extension)) {
            return "image";
        }

        // Video files
        if (mimeType.startsWith("video/") ||
                List.of("mp4", "avi", "mov", "wmv", "flv", "webm", "mkv").contains(extension)) {
            return "video";
        }

        // Audio files
        if (mimeType.startsWith("audio/") ||
                List.of("mp3", "wav", "flac", "aac", "ogg", "wma").contains(extension)) {
            return "audio";
        }

        // Document files
        if (mimeType.contains("pdf") ||
                List.of("pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt", "rtf").contains(extension)) {
            return "document";
        }

        // Archive files
        if (List.of("zip", "rar", "7z", "tar", "gz", "bz2").contains(extension)) {
            return "archive";
        }

        // Code files
        if (List.of("java", "js", "html", "css", "py", "cpp", "c", "h", "json", "xml", "yml", "yaml").contains(extension)) {
            return "code";
        }

        return "other";
    }

    private boolean canPreviewFile(String mimeType, String extension) {
        if (mimeType == null) {
            mimeType = "";
        }

        // Images
        if (mimeType.startsWith("image/") ||
                List.of("jpg", "jpeg", "png", "gif", "bmp", "webp", "svg").contains(extension)) {
            return true;
        }

        // Text/Code files
        if (mimeType.startsWith("text/") ||
                List.of("txt", "json", "xml", "csv", "java", "js", "html", "css", "py", "yml", "yaml").contains(extension)) {
            return true;
        }

        // PDF
        if (mimeType.contains("pdf") || "pdf".equals(extension)) {
            return true;
        }

        // Videos (basic preview support)
        if (mimeType.startsWith("video/") || List.of("mp4", "webm").contains(extension)) {
            return true;
        }

        return false;
    }

    private String buildFolderPath(OrgFolderEntity folder) {
        if (folder == null) {
            return "/";
        }

        List<String> pathSegments = new ArrayList<>();
        OrgFolderEntity current = folder;

        while (current != null) {
            pathSegments.add(0, current.getFolderName());
            current = current.getParentFolder();
        }

        return "/" + String.join("/", pathSegments);
    }

    private FileInfoResponse buildFileInfoResponse(OrgFileEntity file) {
        String fileName = file.getFileName();
        String extension = extractFileExtension(fileName);
        String mimeType = file.getMimeType();
        String category = determineFileCategory(mimeType, extension);

        return FileInfoResponse.builder()
                // Basic file information
                .id(file.getFileId())
                .name(fileName)
                .originalName(extractOriginalFileName(fileName))
                .size(file.getFileSize())
                .sizeFormatted(formatFileSize(file.getFileSize()))
                .mimeType(mimeType)
                .extension(extension)

                // File categorization and status
                .category(category)
                .scanStatus("clean") // You can implement virus scanning later
                .isDeleted(Boolean.TRUE.equals(file.getIsDeleted()))

                // Location information
                .folderId(file.getFolder() != null ? file.getFolder().getFolderId() : null)
                .folderName(file.getFolder() != null ? file.getFolder().getFolderName() : null)
                .folderPath(buildFolderPath(file.getFolder()))
                .fullPath(buildFullFilePath(file))

                // Project and organization context
                .organisationId(file.getOrganisation().getOrganisationId())
                .organisationName(file.getOrganisation().getOrganisationName())
                .projectId(file.getProject() != null ? file.getProject().getProjectId() : null)
                .projectCode(file.getProject() != null ? file.getProject().getProjectCode() : null)
                .projectName(file.getProject() != null ? file.getProject().getName() : null)

                // Capabilities and permissions
                .canPreview(canPreviewFile(mimeType, extension))
                .canDownload(true) // Assuming all accessible files can be downloaded
                .canEdit(canEditFile(mimeType, extension))
                .canDelete(true) // Based on user permissions
                .canShare(true) // Based on user permissions

                // Creator and modification info
                .creatorId(file.getCreator() != null ? file.getCreator().getAccountId() : null)
                .creatorEmail(file.getCreator() != null ? file.getCreator().getUserName() : null)
                .uploadedAt(file.getCreatedAt())
                .updatedAt(file.getUpdatedAt())
                .deletedAt(file.getDeletedAt())

                // Preview and download URLs (generate if needed)
                .previewUrl(generatePreviewUrl(file))
                .downloadUrl(generateDownloadUrl(file))
                .thumbnailUrl(generateThumbnailUrl(file))

                // File metadata (basic implementation)
                .metadata(buildFileMetadata(file, mimeType, extension))

                .build();
    }

    // Helper methods
    private String extractOriginalFileName(String fileName) {
        if (fileName == null) return null;

        // Remove timestamp suffix if present (format: name_timestamp.ext)
        int lastUnderscore = fileName.lastIndexOf('_');
        int lastDot = fileName.lastIndexOf('.');

        if (lastUnderscore > 0 && lastDot > lastUnderscore) {
            String timestampPart = fileName.substring(lastUnderscore + 1, lastDot);
            if (timestampPart.matches("\\d{13}")) { // 13-digit timestamp
                return fileName.substring(0, lastUnderscore) + fileName.substring(lastDot);
            }
        }

        return fileName;
    }

    private String buildFullFilePath(OrgFileEntity file) {
        String folderPath = buildFolderPath(file.getFolder());
        return folderPath.endsWith("/") ? folderPath + file.getFileName() : folderPath + "/" + file.getFileName();
    }


    private boolean canEditFile(String mimeType, String extension) {
        if (mimeType == null) mimeType = "";

        // Text files that can be edited
        if (mimeType.startsWith("text/") ||
                List.of("txt", "json", "xml", "csv", "java", "js", "html", "css", "py", "yml", "yaml", "md").contains(extension)) {
            return true;
        }

        return false;
    }

    private String generatePreviewUrl(OrgFileEntity file) {
        if (!canPreviewFile(file.getMimeType(), extractFileExtension(file.getFileName()))) {
            return null;
        }

        // Generate presigned URL for preview (valid for 1 hour)
        try {
            return minioService.generatePresignedDownloadUrl(
                    file.getOrganisation().getOrganisationId(),
                    file.getMinioKey(),
                    60
            );
        } catch (Exception e) {
            log.warn("Failed to generate preview URL for file: {}", file.getFileId(), e);
            return null;
        }
    }

    private String generateDownloadUrl(OrgFileEntity file) {
        // Generate presigned URL for download (valid for 1 hour)
        try {
            return minioService.generatePresignedDownloadUrl(
                    file.getOrganisation().getOrganisationId(),
                    file.getMinioKey(),
                    60
            );
        } catch (Exception e) {
            log.warn("Failed to generate download URL for file: {}", file.getFileId(), e);
            return null;
        }
    }

    private String generateThumbnailUrl(OrgFileEntity file) {
        String mimeType = file.getMimeType();
        if (mimeType != null && mimeType.startsWith("image/")) {
            // For images, use the same as preview URL
            return generatePreviewUrl(file);
        }
        return null;
    }

    private FileInfoResponse.FileMetadata buildFileMetadata(OrgFileEntity file, String mimeType, String extension) {
        FileInfoResponse.FileMetadata.FileMetadataBuilder builder = FileInfoResponse.FileMetadata.builder();

        // Basic metadata that's always available
        builder.encoding("UTF-8"); // Default encoding
        builder.isEncrypted(false); // Default assumption
        builder.hasPassword(false); // Default assumption

        // Category-specific metadata (basic implementation)
        if (mimeType != null) {
            if (mimeType.startsWith("image/")) {
                // For images, you could extract EXIF data here
                builder.colorSpace("RGB"); // Default assumption
            } else if (mimeType.startsWith("video/")) {
                // For videos, you could extract video metadata here
                builder.videoCodec("unknown");
                builder.audioCodec("unknown");
            } else if (mimeType.contains("pdf")) {
                // For PDFs, you could extract document metadata here
                builder.pageCount(1); // Default assumption
            }
        }

        return builder.build();
    }

    private String generateDownloadUrlForFile(UUID organisationId, String minioKey) {
        try {
            return minioService.generatePresignedDownloadUrl(organisationId, minioKey, 60);
        } catch (Exception e) {
            log.warn("Failed to generate download URL for minioKey: {}", minioKey, e);
            return null;
        }
    }
}