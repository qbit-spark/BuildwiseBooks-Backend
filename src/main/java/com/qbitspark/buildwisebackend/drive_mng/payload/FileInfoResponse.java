package com.qbitspark.buildwisebackend.drive_mng.payload;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class FileInfoResponse {

    // Basic file information
    private UUID id;
    private String name;
    private String originalName;
    private Long size;
    private String sizeFormatted;
    private String mimeType;
    private String extension;

    // File categorization and status
    private String category;
    private String scanStatus;
    private boolean isDeleted;

    // Location information
    private UUID folderId;
    private String folderName;
    private String folderPath;
    private String fullPath;

    // Project and organization context
    private UUID organisationId;
    private String organisationName;
    private UUID projectId;
    private String projectCode;
    private String projectName;

    // Capabilities and permissions
    private boolean canPreview;
    private boolean canDownload;
    private boolean canEdit;
    private boolean canDelete;
    private boolean canShare;

    // Creator and modification info
    private UUID creatorId;
    private String creatorName;
    private String creatorEmail;
    private LocalDateTime uploadedAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;

    // Preview and download URLs (optional)
    private String previewUrl;
    private String downloadUrl;
    private String thumbnailUrl;

    // File metadata
    private FileMetadata metadata;

    // Error information (for failed requests)
    private String errorMessage;
    private String errorCode;

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class FileMetadata {
        // Image metadata
        private Integer width;
        private Integer height;
        private String colorSpace;
        private Integer dpi;

        // Document metadata
        private Integer pageCount;
        private String author;
        private String title;
        private String subject;
        private LocalDateTime createdDate;
        private LocalDateTime modifiedDate;

        // Video metadata
        private Integer duration; // in seconds
        private String videoCodec;
        private String audioCodec;
        private Integer bitrate;
        private Double frameRate;

        // Audio metadata
        private String artist;
        private String album;
        private String genre;
        private Integer year;
        private Integer trackNumber;

        // General metadata
        private String encoding;
        private String language;
        private Boolean isEncrypted;
        private Boolean hasPassword;
    }

    public boolean isCode() {
        return "code".equals(this.category);
    }


    // Static factory methods for common scenarios
    public static FileInfoResponse createErrorResponse(UUID fileId, String errorMessage, String errorCode) {
        return FileInfoResponse.builder()
                .id(fileId)
                .name("Unknown")
                .canPreview(false)
                .canDownload(false)
                .canEdit(false)
                .canDelete(false)
                .canShare(false)
                .category("error")
                .scanStatus("unknown")
                .isDeleted(false)
                .errorMessage(errorMessage)
                .errorCode(errorCode)
                .build();
    }

    public static FileInfoResponse createDeletedFileResponse(UUID fileId, String fileName) {
        return FileInfoResponse.builder()
                .id(fileId)
                .name(fileName)
                .canPreview(false)
                .canDownload(false)
                .canEdit(false)
                .canDelete(false)
                .canShare(false)
                .category("deleted")
                .scanStatus("unknown")
                .isDeleted(true)
                .errorMessage("File has been deleted")
                .errorCode("FILE_DELETED")
                .build();
    }
}