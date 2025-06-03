package com.qbitspark.buildwisebackend.globe_api_client;


import com.qbitspark.buildwisebackend.globeresponsebody.GlobeFailureResponseBuilder;
import com.qbitspark.buildwisebackend.globeresponsebody.GlobeSuccessResponseBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.io.File;
import java.io.IOException;

@Component
@RequiredArgsConstructor
@Slf4j
public class ImageServerApiBasicClient {

    private final WebClient.Builder webClientBuilder;

    @Value("${server.url}")
    private String serverUrl;

    @Value("${spring.application.name}")
    private String applicationName;

    public Object uploadFiles(MultipartFile[] files, String folderName) {
        try {
            log.info("Starting file upload for {} files to folder: {}", files.length, folderName);

            // Validate input
            if (files == null || files.length == 0) {
                return GlobeFailureResponseBuilder.badRequest("No files provided for upload");
            }

            if (folderName == null || folderName.trim().isEmpty()) {
                return GlobeFailureResponseBuilder.badRequest("Folder name is required");
            }

            // Use the WebClient Builder with the base URL
            WebClient webClient = webClientBuilder.baseUrl(serverUrl).build();

            // Create the body for the multipart request
            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();

            // Construct the project folder
            String projectFolder = applicationName;

            // Add each file as a FileSystemResource in the multipart request body
            for (MultipartFile file : files) {
                if (file.isEmpty()) {
                    log.warn("Skipping empty file: {}", file.getOriginalFilename());
                    continue;
                }

                try {
                    // Convert MultipartFile to File
                    File tempFile = File.createTempFile("upload-", file.getOriginalFilename());
                    file.transferTo(tempFile);

                    // Add the file to the body as FileSystemResource
                    body.add("files", new FileSystemResource(tempFile));

                    log.debug("Added file to upload: {}", file.getOriginalFilename());
                } catch (IOException e) {
                    log.error("Failed to process file: {}", file.getOriginalFilename(), e);
                    return GlobeFailureResponseBuilder.error(
                            "Failed to process file: " + file.getOriginalFilename(),
                            HttpStatus.INTERNAL_SERVER_ERROR
                    );
                }
            }

            // Add the project folder path with the images subfolder to the request body
            body.add("projectName", projectFolder);
            body.add("folderName", folderName);

            // Perform the WebClient request
            Object response = webClient.post()
                    .uri("/upload")
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(BodyInserters.fromMultipartData(body))
                    .retrieve()
                    .bodyToMono(Object.class) // Use Object to handle any response type
                    .block();

            log.info("File upload completed successfully");

            // Return success response with the actual server response
            return GlobeSuccessResponseBuilder.success("Files uploaded successfully", response);

        } catch (WebClientResponseException e) {
            log.error("WebClient error during file upload: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());

            return GlobeFailureResponseBuilder.error(
                    "Upload failed: " + e.getResponseBodyAsString(),
                    HttpStatus.valueOf(e.getStatusCode().value())
            );

        } catch (Exception e) {
            log.error("Unexpected error during file upload", e);

            return GlobeFailureResponseBuilder.error(
                    "Upload failed: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }

    /**
     * Upload single file - convenience method
     */
    public Object uploadFile(MultipartFile file, String folderName) {
        if (file == null) {
            return GlobeFailureResponseBuilder.badRequest("File is required");
        }

        return uploadFiles(new MultipartFile[]{file}, folderName);
    }

    /**
     * Check server health
     */
    public Object checkServerHealth() {
        try {
            log.info("Checking server health at: {}", serverUrl);

            WebClient webClient = webClientBuilder.baseUrl(serverUrl).build();

            Object response = webClient.get()
                    .uri("/health")
                    .retrieve()
                    .bodyToMono(Object.class)
                    .block();

            return GlobeSuccessResponseBuilder.success("Server is healthy", response);

        } catch (WebClientResponseException e) {
            log.error("Server health check failed: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());

            return GlobeFailureResponseBuilder.error(
                    "Server health check failed",
                    HttpStatus.valueOf(e.getStatusCode().value())
            );

        } catch (Exception e) {
            log.error("Server health check failed", e);

            return GlobeFailureResponseBuilder.error(
                    "Server is not reachable: " + e.getMessage(),
                    HttpStatus.SERVICE_UNAVAILABLE
            );
        }
    }
}