package com.qbitspark.buildwisebackend.clientsmng_service.controller;
import com.qbitspark.buildwisebackend.clientsmng_service.payloads.ClientResponse;
import com.qbitspark.buildwisebackend.clientsmng_service.payloads.CreateClientRequest;
import com.qbitspark.buildwisebackend.clientsmng_service.payloads.UpdateClientRequest;
import com.qbitspark.buildwisebackend.clientsmng_service.service.ClientService;
import com.qbitspark.buildwisebackend.projectmngService.payloads.ApiResponse;
import com.qbitspark.buildwisebackend.projectmngService.payloads.ProjectResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/clients")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class ClientController {

    private final ClientService clientService;

    @PostMapping
    public ResponseEntity<ApiResponse<ClientResponse>> createClient(@Valid @RequestBody CreateClientRequest request) {
        log.info("Creating new client: {}", request.getName());

        ClientResponse response = clientService.createClient(request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.<ClientResponse>builder()
                        .success(true)
                        .message("Client created successfully")
                        .data(response)
                        .build());
    }

    @GetMapping("/{clientId}")
    public ResponseEntity<ApiResponse<ClientResponse>> getClientById(@PathVariable UUID clientId) {
        log.info("Fetching client with ID: {}", clientId);

        ClientResponse response = clientService.getClientById(clientId);

        return ResponseEntity.ok(ApiResponse.<ClientResponse>builder()
                .success(true)
                .message("Client retrieved successfully")
                .data(response)
                .build());
    }

    // NEW ENDPOINT: Get all projects for a specific client
    @GetMapping("/{clientId}/projects")
    public ResponseEntity<ApiResponse<List<ProjectResponse>>> getClientProjects(@PathVariable UUID clientId) {
        log.info("Fetching projects for client with ID: {}", clientId);

        List<ProjectResponse> projects = clientService.getClientProjects(clientId);

        return ResponseEntity.ok(ApiResponse.<List<ProjectResponse>>builder()
                .success(true)
                .message("Client projects retrieved successfully")
                .data(projects)
                .build());
    }

    // NEW ENDPOINT: Get paginated projects for a specific client
    @GetMapping("/{clientId}/projects/paginated")
    public ResponseEntity<ApiResponse<Page<ProjectResponse>>> getClientProjectsPaginated(
            @PathVariable UUID clientId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        log.info("Fetching paginated projects for client with ID: {}", clientId);

        Sort sort = sortDir.equalsIgnoreCase("desc") ?
                Sort.by(sortBy).descending() :
                Sort.by(sortBy).ascending();

        Pageable pageable = PageRequest.of(page, size, sort);
        Page<ProjectResponse> projects = clientService.getClientProjectsPaginated(clientId, pageable);

        return ResponseEntity.ok(ApiResponse.<Page<ProjectResponse>>builder()
                .success(true)
                .message("Client projects retrieved successfully")
                .data(projects)
                .build());
    }

    // NEW ENDPOINT: Get client with detailed projects information
    @GetMapping("/{clientId}/with-projects")
    public ResponseEntity<ApiResponse<ClientResponse>> getClientWithProjects(@PathVariable UUID clientId) {
        log.info("Fetching client with projects for ID: {}", clientId);

        ClientResponse response = clientService.getClientWithProjects(clientId);

        return ResponseEntity.ok(ApiResponse.<ClientResponse>builder()
                .success(true)
                .message("Client with projects retrieved successfully")
                .data(response)
                .build());
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<ClientResponse>>> getAllClients(
            @RequestParam(defaultValue = "false") boolean activeOnly) {

        log.info("Fetching all clients, activeOnly: {}", activeOnly);

        List<ClientResponse> responses = activeOnly ?
                clientService.getActiveClients() :
                clientService.getAllClients();

        return ResponseEntity.ok(ApiResponse.<List<ClientResponse>>builder()
                .success(true)
                .message("Clients retrieved successfully")
                .data(responses)
                .build());
    }

    @PutMapping("/{clientId}")
    public ResponseEntity<ApiResponse<ClientResponse>> updateClient(
            @PathVariable UUID clientId,
            @Valid @RequestBody UpdateClientRequest request) {

        log.info("Updating client with ID: {}", clientId);

        ClientResponse response = clientService.updateClient(clientId, request);

        return ResponseEntity.ok(ApiResponse.<ClientResponse>builder()
                .success(true)
                .message("Client updated successfully")
                .data(response)
                .build());
    }

    @DeleteMapping("/{clientId}")
    public ResponseEntity<ApiResponse<Void>> deleteClient(@PathVariable UUID clientId) {
        log.info("Deleting client with ID: {}", clientId);

        clientService.deleteClient(clientId);

        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true)
                .message("Client deleted successfully")
                .build());
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<Page<ClientResponse>>> searchClients(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) Boolean isActive,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        log.info("Searching clients with criteria - name: {}, email: {}, isActive: {}", name, email, isActive);

        Sort sort = sortDir.equalsIgnoreCase("desc") ?
                Sort.by(sortBy).descending() :
                Sort.by(sortBy).ascending();

        Pageable pageable = PageRequest.of(page, size, sort);
        Page<ClientResponse> responses = clientService.searchClients(name, email, isActive, pageable);

        return ResponseEntity.ok(ApiResponse.<Page<ClientResponse>>builder()
                .success(true)
                .message("Clients search completed successfully")
                .data(responses)
                .build());
    }

    @PatchMapping("/{clientId}/toggle-status")
    public ResponseEntity<ApiResponse<ClientResponse>> toggleClientStatus(@PathVariable UUID clientId) {
        log.info("Toggling status for client with ID: {}", clientId);

        ClientResponse response = clientService.toggleClientStatus(clientId);

        return ResponseEntity.ok(ApiResponse.<ClientResponse>builder()
                .success(true)
                .message("Client status updated successfully")
                .data(response)
                .build());
    }

    @GetMapping("/check-tin/{tin}")
    public ResponseEntity<ApiResponse<Boolean>> checkTinExists(@PathVariable String tin) {
        boolean exists = clientService.existsByTin(tin);

        return ResponseEntity.ok(ApiResponse.<Boolean>builder()
                .success(true)
                .message("TIN check completed")
                .data(exists)
                .build());
    }

    @GetMapping("/check-email/{email}")
    public ResponseEntity<ApiResponse<Boolean>> checkEmailExists(@PathVariable String email) {
        boolean exists = clientService.existsByEmail(email);

        return ResponseEntity.ok(ApiResponse.<Boolean>builder()
                .success(true)
                .message("Email check completed")
                .data(exists)
                .build());
    }
}
