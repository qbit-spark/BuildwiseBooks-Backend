package com.qbitspark.buildwisebackend.clientsmng_service.service;

import com.qbitspark.buildwisebackend.clientsmng_service.payloads.ClientResponse;
import com.qbitspark.buildwisebackend.clientsmng_service.payloads.CreateClientRequest;
import com.qbitspark.buildwisebackend.clientsmng_service.payloads.UpdateClientRequest;
import com.qbitspark.buildwisebackend.projectmngService.payloads.ProjectResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface ClientService {

    ClientResponse createClient(CreateClientRequest request);

    ClientResponse getClientById(UUID clientId);

    List<ClientResponse> getAllClients();

    List<ClientResponse> getActiveClients();

    ClientResponse updateClient(UUID clientId, UpdateClientRequest request);

    void deleteClient(UUID clientId);

    Page<ClientResponse> searchClients(String name, String email, Boolean isActive, Pageable pageable);

    ClientResponse toggleClientStatus(UUID clientId);

    boolean existsByTin(String tin);

    boolean existsByEmail(String email);

    // NEW METHODS for getting client projects
    List<ProjectResponse> getClientProjects(UUID clientId);

    Page<ProjectResponse> getClientProjectsPaginated(UUID clientId, Pageable pageable);

    ClientResponse getClientWithProjects(UUID clientId);
}