package com.qbitspark.buildwisebackend.clientsmngService.service.Impl;
import com.qbitspark.buildwisebackend.clientsmngService.entity.ClientEntity;
import com.qbitspark.buildwisebackend.clientsmngService.payloads.ClientResponse;
import com.qbitspark.buildwisebackend.clientsmngService.payloads.ClientWithProjectsResponse;
import com.qbitspark.buildwisebackend.clientsmngService.payloads.CreateClientRequest;
import com.qbitspark.buildwisebackend.clientsmngService.payloads.UpdateClientRequest;
import com.qbitspark.buildwisebackend.clientsmngService.repo.ClientsRepo;
import com.qbitspark.buildwisebackend.clientsmngService.service.ClientService;
import com.qbitspark.buildwisebackend.globeadvice.exceptions.DuplicateResourceException;
import com.qbitspark.buildwisebackend.globeadvice.exceptions.ResourceNotFoundException;
import com.qbitspark.buildwisebackend.projectmngService.entity.ProjectEntity;
import com.qbitspark.buildwisebackend.projectmngService.payloads.ProjectResponse;
import com.qbitspark.buildwisebackend.projectmngService.repo.ProjectRepo;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ClientServiceImpl implements ClientService {

    private final ClientsRepo clientRepository;
    private final ProjectRepo projectRepository;

    // Create client - handle constraints properly
    @Override
    public ClientResponse createClient(CreateClientRequest request) {
        try {
            // Check for existing client by email or TIN
            if (request.getEmail() != null && clientRepository.existsByEmail(request.getEmail())) {
                throw new IllegalArgumentException("Client with email already exists");
            }

            if (request.getTin() != null && clientRepository.existsByTin(request.getTin())) {
                throw new IllegalArgumentException("Client with TIN already exists");
            }

            ClientEntity client = new ClientEntity();
            client.setName(request.getName());
            client.setDescription(request.getDescription());
            client.setAddress(request.getAddress());
            client.setOfficePhone(request.getOfficePhone());
            client.setTin(request.getTin());
            client.setEmail(request.getEmail());
            client.setIsActive(true);

            ClientEntity savedClient = clientRepository.save(client);
            return mapToResponse(savedClient);

        } catch (DataIntegrityViolationException e) {
            throw new IllegalArgumentException("Data integrity violation: " + e.getMessage());
        } catch (Exception e) {
            throw new RuntimeException("Error creating client: " + e.getMessage());
        }
    }


    @Override
    @Transactional(readOnly = true)
    public ClientResponse getClientById(UUID clientId) {
        try {
            ClientEntity client = clientRepository.findById(clientId)
                    .orElseThrow(() -> new EntityNotFoundException("Client not found"));
            return mapToResponse(client);
        } catch (Exception e) {
            throw new RuntimeException("Error fetching client: " + e.getMessage());
        }
    }

    // Get all clients - read-only transaction
    @Override
    @Transactional(readOnly = true)
    public List<ClientResponse> getAllClients() {
        try {
            List<ClientEntity> clients = clientRepository.findAll();
            return clients.stream()
                    .map(this::mapToResponse)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            throw new RuntimeException("Error fetching clients: " + e.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClientResponse> getActiveClients() {
        log.info("Fetching active clients");

        List<ClientEntity> activeClients = clientRepository.findByIsActiveTrue();
        return activeClients.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public ClientResponse updateClient(UUID clientId, UpdateClientRequest request) {
        log.info("Updating client with ID: {}", clientId);

        ClientEntity client = clientRepository.findById(clientId)
                .orElseThrow(() -> new ResourceNotFoundException("Client not found"));

        // Check for duplicate TIN (excluding current client)
        if (request.getTin() != null && clientRepository.existsByTinAndClientIdNot(request.getTin(), clientId)) {
            throw new DuplicateResourceException("Client with TIN " + request.getTin() + " already exists");
        }

        // Check for duplicate email (excluding current client)
        if (request.getEmail() != null && clientRepository.existsByEmailAndClientIdNot(request.getEmail(), clientId)) {
            throw new DuplicateResourceException("Client with email " + request.getEmail() + " already exists");
        }

        // Update fields if provided
        if (request.getName() != null) {
            client.setName(request.getName());
        }
        if (request.getDescription() != null) {
            client.setDescription(request.getDescription());
        }
        if (request.getAddress() != null) {
            client.setAddress(request.getAddress());
        }
        if (request.getOfficePhone() != null) {
            client.setOfficePhone(request.getOfficePhone());
        }
        if (request.getTin() != null) {
            client.setTin(request.getTin());
        }
        if (request.getEmail() != null) {
            client.setEmail(request.getEmail());
        }
        if (request.getIsActive() != null) {
            client.setIsActive(request.getIsActive());
        }

        ClientEntity updatedClient = clientRepository.save(client);
        log.info("Successfully updated client with ID: {}", updatedClient.getClientId());

        return mapToResponse(updatedClient);
    }

    @Override
    public void deleteClient(UUID clientId) {
        log.info("Deleting client with ID: {}", clientId);

        ClientEntity client = clientRepository.findById(clientId)
                .orElseThrow(() -> new ResourceNotFoundException("Client not found with ID given "));

        // Soft delete - just set isActive to false
        client.setIsActive(false);
        clientRepository.save(client);

        log.info("Successfully deleted (deactivated) client with ID: {}", clientId);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ClientResponse> searchClients(String name, String email, Boolean isActive, Pageable pageable) {
        log.info("Searching clients with criteria - name: {}, email: {}, isActive: {}", name, email, isActive);

        Page<ClientEntity> clients = clientRepository.findBySearchCriteria(name, email, isActive, pageable);
        return clients.map(this::mapToResponse);
    }

    @Override
    public ClientResponse toggleClientStatus(UUID clientId) {
        log.info("Toggling status for client with ID: {}", clientId);

        ClientEntity client = clientRepository.findById(clientId)
                .orElseThrow(() -> new ResourceNotFoundException("Client not found with ID: " + clientId));

        client.setIsActive(!client.getIsActive());
        ClientEntity savedClient = clientRepository.save(client);

        log.info("Successfully toggled status for client with ID: {} to {}", clientId, savedClient.getIsActive());

        return mapToResponse(savedClient);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByTin(String tin) {
        return clientRepository.findByTin(tin).isPresent();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByEmail(String email) {
        return clientRepository.findByEmail(email).isPresent();
    }

    // NEW METHODS for getting client projects
    @Override
    @Transactional(readOnly = true)
    public List<ProjectResponse> getClientProjects(UUID clientId) {
        try {
            // Verify client exists
            if (!clientRepository.existsById(clientId)) {
                throw new EntityNotFoundException("Client not found with ID given ");
            }

            List<ProjectEntity> projects = projectRepository.findByClientClientId(clientId);
            return projects.stream()
                    .map(this::mapProjectToResponse)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            throw new RuntimeException("Error fetching client projects: " + e.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProjectResponse> getClientProjectsPaginated(UUID clientId, Pageable pageable) {
        try {
            // Verify client exists
            if (!clientRepository.existsById(clientId)) {
                throw new EntityNotFoundException("Client not found");
            }

            Page<ProjectEntity> projects = projectRepository.findByClientClientId(clientId, pageable);
            return projects.map(this::mapProjectToResponse);
        } catch (Exception e) {
            throw new RuntimeException("Error fetching client projects: " + e.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public ClientResponse getClientWithProjects(UUID clientId) {
        try {
            ClientEntity client = clientRepository.findById(clientId)
                    .orElseThrow(() -> new EntityNotFoundException("Client not found"));

            List<ProjectEntity> projects = projectRepository.findByClientClientId(clientId);

            ClientWithProjectsResponse response = new ClientWithProjectsResponse();
            response.setClient(mapToResponse(client));
            response.setProjects(projects.stream()
                    .map(this::mapProjectToResponse)
                    .collect(Collectors.toList()));
            response.setTotalProjects(projects.size());

            return response.getClient();
        } catch (Exception e) {
            throw new RuntimeException("Error fetching client with projects: " + e.getMessage());
        }
    }

    // Helper method to map Client entity to ClientResponse DTO
    private ClientResponse mapToResponse(ClientEntity client) {
        ClientResponse response = new ClientResponse();
        response.setClientId(client.getClientId());
        response.setName(client.getName());
        response.setDescription(client.getDescription());
        response.setAddress(client.getAddress());
        response.setOfficePhone(client.getOfficePhone());
        response.setTin(client.getTin());
        response.setEmail(client.getEmail());
        response.setIsActive(client.getIsActive());
        response.setCreatedAt(client.getCreatedAt());
        response.setUpdatedAt(client.getUpdatedAt());

        // Safe project counting using repository method
        try {
            Long projectCount = projectRepository.countByClientClientId(client.getClientId());
            response.setTotalProjects(projectCount != null ? projectCount.intValue() : 0);
        } catch (Exception e) {
            log.warn("Could not count projects for client {}: {}", client.getClientId(), e.getMessage());
            response.setTotalProjects(0);
        }

        return response;
    }

    // Helper method to map project to response
    private ProjectResponse mapProjectToResponse(ProjectEntity project) {
        ProjectResponse response = new ProjectResponse();
        response.setProjectId(project.getProjectId());
        response.setName(project.getName());
        response.setDescription(project.getDescription());
        response.setBudget(project.getBudget());
        response.setContractNumber(project.getContractNumber());
        response.setStatus(String.valueOf(project.getStatus()));
        response.setCreatedAt(project.getCreatedAt());
        response.setUpdatedAt(project.getUpdatedAt());

        // Set client information
        if (project.getClient() != null) {
            response.setClientId(project.getClient().getClientId());
            response.setClientName(project.getClient().getName());
        }

        // Set organization information
        if (project.getOrganisation() != null) {
            response.setOrganisationId(project.getOrganisation().getOrganisationId());
            response.setOrganisationName(project.getOrganisation().getOrganisationName());
        }

        return response;
    }
}