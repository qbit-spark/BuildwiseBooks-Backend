package com.qbitspark.buildwisebackend.GlobeAPIClient;


import com.qbitspark.buildwisebackend.GlobeResponseBody.GlobalJsonResponseBody;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.File;
import java.io.IOException;

@Component
@RequiredArgsConstructor
public class ImageServerApiBasicClient {

    private final WebClient.Builder webClientBuilder; // Injected WebClient builder

    @Value("${server.url}") // Inject server URL from application.properties or application.yml
    private String serverUrl;

    @Value("${spring.application.name}") // Inject application name (project name)
    private String applicationName;

    public GlobalJsonResponseBody uploadFiles(MultipartFile[] files, String folderName) throws IOException {

        // Use the WebClient Builder with the base URL
        WebClient webClient = webClientBuilder.baseUrl(serverUrl).build();

        // Create the body for the multipart request
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();

        // Construct the project folder and images subfolder
        String projectFolder = applicationName;

        // Add each file as a FileSystemResource in the multipart request body
        for (MultipartFile file : files) {
            // Convert MultipartFile to File
            File tempFile = File.createTempFile("upload-", file.getOriginalFilename());
            file.transferTo(tempFile);

            // Add the file to the body as FileSystemResource
            body.add("files", new FileSystemResource(tempFile));
        }

        // Add the project folder path with the images subfolder to the request body
        body.add("projectName", projectFolder);
        body.add("folderName", folderName);

        // Perform the WebClient request and return the response
        return webClient.post()
                .uri("/upload")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(BodyInserters.fromMultipartData(body)) // Send data as multipart
                .retrieve()
                .bodyToMono(GlobalJsonResponseBody.class) // Convert response to GlobalJsonResponseBody
                .block(); // Block to wait for the response (can be made async if needed)
    }
}
