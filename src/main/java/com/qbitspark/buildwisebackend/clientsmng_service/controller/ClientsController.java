package com.qbitspark.buildwisebackend.clientsmng_service.controller;

import com.qbitspark.buildwisebackend.clientsmng_service.payloads.ClientResponse;
import com.qbitspark.buildwisebackend.clientsmng_service.payloads.CreateClientRequest;
import com.qbitspark.buildwisebackend.clientsmng_service.payloads.ProjectResponseForClient;
import com.qbitspark.buildwisebackend.clientsmng_service.payloads.UpdateClientRequest;
import com.qbitspark.buildwisebackend.clientsmng_service.service.ClientService;
import com.qbitspark.buildwisebackend.globeadvice.exceptions.ItemNotFoundException;
import com.qbitspark.buildwisebackend.globeresponsebody.GlobeSuccessResponseBuilder;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/clients")
@RequiredArgsConstructor
public class ClientsController {

    private final ClientService clientsService;

    @PostMapping("/organisation/{organisationId}")
    public ResponseEntity<GlobeSuccessResponseBuilder> createClient(
            @PathVariable UUID organisationId,
            @RequestBody @Validated CreateClientRequest request) throws ItemNotFoundException {

        ClientResponse client = clientsService.createClientWithinOrganisation(organisationId, request);
        return ResponseEntity.ok(GlobeSuccessResponseBuilder.success("Client created successfully", client));
    }

    @GetMapping("/{clientId}")
    public ResponseEntity<GlobeSuccessResponseBuilder> getClient(
            @PathVariable UUID clientId) throws ItemNotFoundException {

        ClientResponse client = clientsService.getClientByIdWithinOrganisation(clientId);
        return ResponseEntity.ok(GlobeSuccessResponseBuilder.success("Client retrieved successfully", client));
    }

    @GetMapping("/organisation/{organisationId}")
    public ResponseEntity<GlobeSuccessResponseBuilder> getAllClients(
            @PathVariable UUID organisationId) throws ItemNotFoundException {

        List<ClientResponse> clients = clientsService.getAllClientsWithinOrganisation(organisationId);
        return ResponseEntity.ok(GlobeSuccessResponseBuilder.success("Clients retrieved successfully", clients));
    }

    @PutMapping("/{clientId}")
    public ResponseEntity<GlobeSuccessResponseBuilder> updateClient(
            @PathVariable UUID clientId,
            @RequestBody UpdateClientRequest request) throws ItemNotFoundException {

        ClientResponse client = clientsService.updateClientWithinOrganisation(clientId, request);
        return ResponseEntity.ok(GlobeSuccessResponseBuilder.success("Client updated successfully", client));
    }

    @DeleteMapping("/{clientId}")
    public ResponseEntity<GlobeSuccessResponseBuilder> deleteClient(
            @PathVariable UUID clientId) throws ItemNotFoundException {

        clientsService.deleteClientWithinOrganisation(clientId);
        return ResponseEntity.ok(GlobeSuccessResponseBuilder.success("Client deleted successfully"));
    }

    @GetMapping("/{clientId}/projects")
    public ResponseEntity<GlobeSuccessResponseBuilder> getClientProjects(
            @PathVariable UUID clientId) throws ItemNotFoundException {

        List<ProjectResponseForClient> projects = clientsService.getClientProjectsWithinOrganisation(clientId);
        return ResponseEntity.ok(GlobeSuccessResponseBuilder.success("Client projects retrieved successfully", projects));
    }
}