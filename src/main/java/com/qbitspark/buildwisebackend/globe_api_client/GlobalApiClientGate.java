package com.qbitspark.buildwisebackend.globe_api_client;

import com.qbitspark.buildwisebackend.globe_api_client.payloads.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Map;

@Component
@Slf4j
@RequiredArgsConstructor
public class GlobalApiClientGate {

    private final WebClient webClient;

    @Value("${app.external.timeout:60000}")
    private int timeout;

    // ================================
    // SYNCHRONOUS METHODS
    // ================================

    public <T> ApiResponse<T> get(String url, Map<String, String> headers, Class<T> responseType) {
        try {
            log.info("GET: {}", url);

            T data = webClient.get()
                    .uri(url)
                    .headers(h -> addHeaders(h, headers))
                    .retrieve()
                    .bodyToMono(responseType)
                    .timeout(Duration.ofMillis(timeout))
                    .block();

            return success(data);

        } catch (Exception e) {
            log.error("GET failed: {}", url, e);
            return error(e);
        }
    }

    public <T, R> ApiResponse<R> post(String url, T body, Map<String, String> headers, Class<R> responseType) {
        try {
            log.info("POST: {}", url);

            R data = webClient.post()
                    .uri(url)
                    .headers(h -> addHeaders(h, headers))
                    .bodyValue(body != null ? body : "")
                    .retrieve()
                    .bodyToMono(responseType)
                    .timeout(Duration.ofMillis(timeout))
                    .block();

            return success(data);

        } catch (Exception e) {
            log.error("POST failed: {}", url, e);
            return error(e);
        }
    }

    public <T, R> ApiResponse<R> put(String url, T body, Map<String, String> headers, Class<R> responseType) {
        try {
            log.info("PUT: {}", url);

            R data = webClient.put()
                    .uri(url)
                    .headers(h -> addHeaders(h, headers))
                    .bodyValue(body != null ? body : "")
                    .retrieve()
                    .bodyToMono(responseType)
                    .timeout(Duration.ofMillis(timeout))
                    .block();

            return success(data);

        } catch (Exception e) {
            log.error("PUT failed: {}", url, e);
            return error(e);
        }
    }

    public <T> ApiResponse<T> delete(String url, Map<String, String> headers, Class<T> responseType) {
        try {
            log.info("DELETE: {}", url);

            T data = webClient.delete()
                    .uri(url)
                    .headers(h -> addHeaders(h, headers))
                    .retrieve()
                    .bodyToMono(responseType)
                    .timeout(Duration.ofMillis(timeout))
                    .block();

            return success(data);

        } catch (Exception e) {
            log.error("DELETE failed: {}", url, e);
            return error(e);
        }
    }

    public <T, R> ApiResponse<R> patch(String url, T body, Map<String, String> headers, Class<R> responseType) {
        try {
            log.info("PATCH: {}", url);

            R data = webClient.patch()
                    .uri(url)
                    .headers(h -> addHeaders(h, headers))
                    .bodyValue(body != null ? body : "")
                    .retrieve()
                    .bodyToMono(responseType)
                    .timeout(Duration.ofMillis(timeout))
                    .block();

            return success(data);

        } catch (Exception e) {
            log.error("PATCH failed: {}", url, e);
            return error(e);
        }
    }

    // ================================
    // ASYNCHRONOUS METHODS
    // ================================

    public <T> Mono<ApiResponse<T>> getAsync(String url, Map<String, String> headers, Class<T> responseType) {
        log.info("Async GET: {}", url);

        return webClient.get()
                .uri(url)
                .headers(h -> addHeaders(h, headers))
                .retrieve()
                .bodyToMono(responseType)
                .timeout(Duration.ofMillis(timeout))
                .map(this::success)
                .onErrorResume(this::errorAsync);
    }

    public <T, R> Mono<ApiResponse<R>> postAsync(String url, T body, Map<String, String> headers, Class<R> responseType) {
        log.info("Async POST: {}", url);

        return webClient.post()
                .uri(url)
                .headers(h -> addHeaders(h, headers))
                .bodyValue(body != null ? body : "")
                .retrieve()
                .bodyToMono(responseType)
                .timeout(Duration.ofMillis(timeout))
                .map(this::success)
                .onErrorResume(this::errorAsync);
    }

    public <T, R> Mono<ApiResponse<R>> putAsync(String url, T body, Map<String, String> headers, Class<R> responseType) {
        log.info("Async PUT: {}", url);

        return webClient.put()
                .uri(url)
                .headers(h -> addHeaders(h, headers))
                .bodyValue(body != null ? body : "")
                .retrieve()
                .bodyToMono(responseType)
                .timeout(Duration.ofMillis(timeout))
                .map(this::success)
                .onErrorResume(this::errorAsync);
    }

    public <T> Mono<ApiResponse<T>> deleteAsync(String url, Map<String, String> headers, Class<T> responseType) {
        log.info("Async DELETE: {}", url);

        return webClient.delete()
                .uri(url)
                .headers(h -> addHeaders(h, headers))
                .retrieve()
                .bodyToMono(responseType)
                .timeout(Duration.ofMillis(timeout))
                .map(this::success)
                .onErrorResume(this::errorAsync);
    }

    public <T, R> Mono<ApiResponse<R>> patchAsync(String url, T body, Map<String, String> headers, Class<R> responseType) {
        log.info("Async PATCH: {}", url);

        return webClient.patch()
                .uri(url)
                .headers(h -> addHeaders(h, headers))
                .bodyValue(body != null ? body : "")
                .retrieve()
                .bodyToMono(responseType)
                .timeout(Duration.ofMillis(timeout))
                .map(this::success)
                .onErrorResume(this::errorAsync);
    }

    // ================================
    // FILE OPERATIONS
    // ================================

    public <T> ApiResponse<T> uploadFile(String url, MultiValueMap<String, Object> files, Map<String, String> headers, Class<T> responseType) {
        try {
            log.info("File upload: {}", url);

            T data = webClient.post()
                    .uri(url)
                    .headers(h -> addHeaders(h, headers))
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .bodyValue(files)
                    .retrieve()
                    .bodyToMono(responseType)
                    .timeout(Duration.ofMillis(timeout * 3)) // Extended timeout for files
                    .block();

            return success(data);

        } catch (Exception e) {
            log.error("File upload failed: {}", url, e);
            return error(e);
        }
    }

    public ApiResponse<byte[]> downloadFile(String url, Map<String, String> headers) {
        try {
            log.info("File download: {}", url);

            byte[] data = webClient.get()
                    .uri(url)
                    .headers(h -> addHeaders(h, headers))
                    .retrieve()
                    .bodyToMono(byte[].class)
                    .timeout(Duration.ofMillis(timeout * 2)) // Extended timeout for downloads
                    .block();

            return success(data);

        } catch (Exception e) {
            log.error("File download failed: {}", url, e);
            return error(e);
        }
    }

    public <T> Mono<ApiResponse<T>> uploadFileAsync(String url, MultiValueMap<String, Object> files, Map<String, String> headers, Class<T> responseType) {
        log.info("Async file upload: {}", url);

        return webClient.post()
                .uri(url)
                .headers(h -> addHeaders(h, headers))
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .bodyValue(files)
                .retrieve()
                .bodyToMono(responseType)
                .timeout(Duration.ofMillis(timeout * 3))
                .map(this::success)
                .onErrorResume(this::errorAsync);
    }

    public Mono<ApiResponse<byte[]>> downloadFileAsync(String url, Map<String, String> headers) {
        log.info("Async file download: {}", url);

        return webClient.get()
                .uri(url)
                .headers(h -> addHeaders(h, headers))
                .retrieve()
                .bodyToMono(byte[].class)
                .timeout(Duration.ofMillis(timeout * 2))
                .map(this::success)
                .onErrorResume(this::errorAsync);
    }

    // ================================
    // CONVENIENCE METHODS
    // ================================

    // Simple calls without headers
    public <T> ApiResponse<T> get(String url, Class<T> responseType) {
        return get(url, Map.of(), responseType);
    }

    public <T, R> ApiResponse<R> post(String url, T body, Class<R> responseType) {
        return post(url, body, Map.of("Content-Type", "application/json"), responseType);
    }

    // With authentication
    public <T> ApiResponse<T> getWithAuth(String url, String token, Class<T> responseType) {
        return get(url, Map.of("Authorization", "Bearer " + token), responseType);
    }

    public <T, R> ApiResponse<R> postWithAuth(String url, T body, String token, Class<R> responseType) {
        Map<String, String> headers = Map.of(
                "Authorization", "Bearer " + token,
                "Content-Type", "application/json"
        );
        return post(url, body, headers, responseType);
    }

    // Async versions
    public <T> Mono<ApiResponse<T>> getAsync(String url, Class<T> responseType) {
        return getAsync(url, Map.of(), responseType);
    }

    public <T, R> Mono<ApiResponse<R>> postAsync(String url, T body, Class<R> responseType) {
        return postAsync(url, body, Map.of("Content-Type", "application/json"), responseType);
    }

    public <T> Mono<ApiResponse<T>> getWithAuthAsync(String url, String token, Class<T> responseType) {
        return getAsync(url, Map.of("Authorization", "Bearer " + token), responseType);
    }

    // ================================
    // HELPER METHODS
    // ================================

    private void addHeaders(HttpHeaders httpHeaders, Map<String, String> headers) {
        if (headers != null) {
            headers.forEach(httpHeaders::add);
        }
    }

    private <T> ApiResponse<T> success(T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .data(data)
                .statusCode(200)
                .build();
    }

    private <T> ApiResponse<T> error(Exception e) {
        return ApiResponse.<T>builder()
                .success(false)
                .errorMessage(getErrorMessage(e))
                .statusCode(getStatusCode(e))
                .exception(e.getClass().getSimpleName())
                .build();
    }

    private <T> Mono<ApiResponse<T>> errorAsync(Throwable e) {
        return Mono.just(ApiResponse.<T>builder()
                .success(false)
                .errorMessage(getErrorMessage(e))
                .statusCode(getStatusCode(e))
                .exception(e.getClass().getSimpleName())
                .build());
    }

    private String getErrorMessage(Throwable e) {
        if (e instanceof WebClientResponseException) {
            WebClientResponseException wcre = (WebClientResponseException) e;
            return String.format("HTTP %d: %s", wcre.getStatusCode().value(), wcre.getResponseBodyAsString());
        }
        return e.getMessage() != null ? e.getMessage() : "Unknown error";
    }

    private int getStatusCode(Throwable e) {
        if (e instanceof WebClientResponseException) {
            return ((WebClientResponseException) e).getStatusCode().value();
        }
        return 500;
    }
}

// ================================
// USAGE EXAMPLES
// ================================

/*
@Service
@RequiredArgsConstructor
public class FileBoxApiClient {

    private final GlobalApiClientGate apiClient;

    @Value("${app.filebox.base-url}")
    private String fileBoxUrl;

    @Value("${app.filebox.api-key}")
    private String apiKey;

    // Simple usage
    public FileBoxResponse getFileInfo(UUID fileId) {
        ApiResponse<FileBoxResponse> response = apiClient.getWithAuth(
            fileBoxUrl + "/api/v1/files/" + fileId,
            apiKey,
            FileBoxResponse.class
        );

        if (response.isSuccess()) {
            return response.getData();
        }
        throw new RuntimeException("Failed to get file info: " + response.getErrorMessage());
    }

    // File upload
    public FileUploadResponse uploadFiles(UUID folderId, List<MultipartFile> files) {
        MultiValueMap<String, Object> formData = new LinkedMultiValueMap<>();
        if (folderId != null) {
            formData.add("folderId", folderId.toString());
        }
        files.forEach(file -> formData.add("files", file.getResource()));

        ApiResponse<FileUploadResponse> response = apiClient.uploadFile(
            fileBoxUrl + "/api/v1/files/upload-batch-sync",
            formData,
            Map.of("Authorization", "Bearer " + apiKey),
            FileUploadResponse.class
        );

        if (response.isSuccess()) {
            return response.getData();
        }
        throw new RuntimeException("Upload failed: " + response.getErrorMessage());
    }

    // Async example
    public Mono<FileBoxResponse> getFileInfoAsync(UUID fileId) {
        return apiClient.getWithAuthAsync(
            fileBoxUrl + "/api/v1/files/" + fileId,
            apiKey,
            FileBoxResponse.class
        ).map(response -> {
            if (response.isSuccess()) {
                return response.getData();
            }
            throw new RuntimeException("Failed: " + response.getErrorMessage());
        });
    }
}
*/