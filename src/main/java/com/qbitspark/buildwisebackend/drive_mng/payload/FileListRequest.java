package com.qbitspark.buildwisebackend.drive_mng.payload;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NonNull;

import java.util.List;
import java.util.UUID;

@Data
public class FileListRequest {
    @NotNull(message = "fileList should not be null")
    private List<UUID> fileList;
}
