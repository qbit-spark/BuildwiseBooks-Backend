package com.qbitspark.buildwisebackend.clientsmng_service.controller;

import com.qbitspark.buildwisebackend.clientsmng_service.payloads.ClientResponse;
import com.qbitspark.buildwisebackend.clientsmng_service.payloads.CreateClientRequest;
import com.qbitspark.buildwisebackend.clientsmng_service.payloads.ProjectResponseForClient;
import com.qbitspark.buildwisebackend.clientsmng_service.payloads.UpdateClientRequest;
import com.qbitspark.buildwisebackend.clientsmng_service.service.ClientService;
import com.qbitspark.buildwisebackend.globeadvice.exceptions.ItemNotFoundException;
import com.qbitspark.buildwisebackend.globeresponsebody.GlobeSuccessResponseBuilder;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/clients/{organisationId}")
@RequiredArgsConstructor
public class ClientsController {

    private final ClientService clientsService;

    @PostMapping
    public ResponseEntity<GlobeSuccessResponseBuilder> createClient(
            @PathVariable UUID organisationId,
            @RequestBody @Validated CreateClientRequest request) throws ItemNotFoundException {

        ClientResponse client = clientsService.createClientWithinOrganisation(organisationId, request);
        return ResponseEntity.ok(GlobeSuccessResponseBuilder.success("Client created successfully", client));
    }

    @GetMapping("/{clientId}")
    public ResponseEntity<GlobeSuccessResponseBuilder> getClient(
            @PathVariable UUID clientId, @PathVariable String organisationId) throws ItemNotFoundException {

        ClientResponse client = clientsService.getClientByIdWithinOrganisation(clientId);
        return ResponseEntity.ok(GlobeSuccessResponseBuilder.success("Client retrieved successfully", client));
    }

    @GetMapping("/summary-list")
    public ResponseEntity<GlobeSuccessResponseBuilder> getAllClientsSummaryList(
            @PathVariable UUID organisationId) throws ItemNotFoundException {

        List<ClientResponse> clients = clientsService.getAllClientsWithinOrganisation(organisationId);
        return ResponseEntity.ok(GlobeSuccessResponseBuilder.success("Clients retrieved successfully", clients));
    }


    @GetMapping
    public ResponseEntity<GlobeSuccessResponseBuilder> getAllClients(
            @PathVariable UUID organisationId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "clientId") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDirection) throws ItemNotFoundException {

        Sort sort = sortDirection.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();

        Pageable pageable = PageRequest.of(page, size, sort);

        Page<ClientResponse> clientsPage = clientsService.getAllClientsWithinOrganisation(
                organisationId, pageable);

        return ResponseEntity.ok(GlobeSuccessResponseBuilder.success("Clients retrieved successfully", clientsPage));
    }

    @PutMapping("/{clientId}")
    public ResponseEntity<GlobeSuccessResponseBuilder> updateClient(
            @PathVariable UUID clientId,
            @RequestBody UpdateClientRequest request, @PathVariable String organisationId) throws ItemNotFoundException {

        ClientResponse client = clientsService.updateClientWithinOrganisation(clientId, request);
        return ResponseEntity.ok(GlobeSuccessResponseBuilder.success("Client updated successfully", client));
    }

    @DeleteMapping("/{clientId}")
    public ResponseEntity<GlobeSuccessResponseBuilder> deleteClient(
            @PathVariable UUID clientId, @PathVariable String organisationId) throws ItemNotFoundException {

        clientsService.deleteClientWithinOrganisation(clientId);
        return ResponseEntity.ok(GlobeSuccessResponseBuilder.success("Client deleted successfully"));
    }

    @GetMapping("/{clientId}/projects")
    public ResponseEntity<GlobeSuccessResponseBuilder> getClientProjects(
            @PathVariable UUID clientId, @PathVariable String organisationId) throws ItemNotFoundException {

        List<ProjectResponseForClient> projects = clientsService.getClientProjectsWithinOrganisation(clientId);
        return ResponseEntity.ok(GlobeSuccessResponseBuilder.success("Client projects retrieved successfully", projects));
    }
}