package com.qbitspark.buildwisebackend.drive_mng.payload;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FileInfoRequest {
    @NotNull
    @NotEmpty(message = "File IDs list cannot be empty")
    @Size(max = 100, message = "Maximum 100 files allowed per request")
    private List<UUID> fileIds;
}