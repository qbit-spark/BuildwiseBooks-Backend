package com.qbitspark.buildwisebackend.clientsmng_service.service.Impl;
import com.qbitspark.buildwisebackend.authentication_service.Repository.AccountRepo;
import com.qbitspark.buildwisebackend.authentication_service.entity.AccountEntity;
import com.qbitspark.buildwisebackend.clientsmng_service.entity.ClientEntity;
import com.qbitspark.buildwisebackend.clientsmng_service.payloads.ClientResponse;
import com.qbitspark.buildwisebackend.clientsmng_service.payloads.CreateClientRequest;
import com.qbitspark.buildwisebackend.clientsmng_service.payloads.ProjectResponseForClient;
import com.qbitspark.buildwisebackend.clientsmng_service.payloads.UpdateClientRequest;
import com.qbitspark.buildwisebackend.clientsmng_service.repo.ClientsRepo;
import com.qbitspark.buildwisebackend.clientsmng_service.service.ClientService;
import com.qbitspark.buildwisebackend.globeadvice.exceptions.ItemNotFoundException;

import com.qbitspark.buildwisebackend.organisation_service.organisation_mng.entity.OrganisationEntity;
import com.qbitspark.buildwisebackend.organisation_service.organisation_mng.repo.OrganisationRepo;
import com.qbitspark.buildwisebackend.organisation_service.orgnisation_members_mng.entities.OrganisationMember;
import com.qbitspark.buildwisebackend.organisation_service.orgnisation_members_mng.enums.MemberRole;
import com.qbitspark.buildwisebackend.organisation_service.orgnisation_members_mng.enums.MemberStatus;
import com.qbitspark.buildwisebackend.organisation_service.orgnisation_members_mng.repo.OrganisationMemberRepo;
import com.qbitspark.buildwisebackend.projectmng_service.entity.ProjectEntity;
import com.qbitspark.buildwisebackend.projectmng_service.payloads.ProjectResponse;
import com.qbitspark.buildwisebackend.projectmng_service.payloads.TeamMemberResponse;
import com.qbitspark.buildwisebackend.projectmng_service.repo.ProjectRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ClientServiceImpl implements ClientService {

    private final ClientsRepo clientsRepo;
    private final OrganisationRepo organisationRepo;
    private final OrganisationMemberRepo organisationMemberRepo;
    private final AccountRepo accountRepo;
    private final ProjectRepo projectRepo;

    @Override
    public ClientResponse createClientWithinOrganisation(UUID organisationId, CreateClientRequest request) throws ItemNotFoundException {

        // Get organisation
        OrganisationEntity organisation = organisationRepo.findById(organisationId)
                .orElseThrow(() -> new ItemNotFoundException("Organisation not found"));

        // Validate user permissions (only OWNER/ADMIN can create clients)
        AccountEntity currentUser = getAuthenticatedAccount();
        validateMemberPermissions(currentUser, organisation, List.of(MemberRole.OWNER, MemberRole.ADMIN));


        // Check for duplicates (now with original values using IgnoreCase methods)
        if (clientsRepo.existsByNameIgnoreCaseAndOrganisationAndIsActiveTrue(request.getName(), organisation)) {
            throw new ItemNotFoundException("Client with this name already exists in the organisation");
        }
        if (clientsRepo.existsByAddressIgnoreCaseAndOrganisationAndIsActiveTrue(request.getAddress(), organisation)) {
            throw new ItemNotFoundException("Client with this address already exists in the organisation");
        }
        if (clientsRepo.existsByTinIgnoreCaseAndOrganisationAndIsActiveTrue(request.getTin(), organisation)) {
            throw new ItemNotFoundException("Client with this TIN already exists in the organisation");
        }
        if (clientsRepo.existsByEmailIgnoreCaseAndOrganisationAndIsActiveTrue(request.getEmail(), organisation)) {
            throw new ItemNotFoundException("Client with this email already exists in the organisation");
        }

        // Create client with original values
        ClientEntity client = new ClientEntity();
        client.setName(request.getName());
        client.setDescription(request.getDescription());
        client.setAddress(request.getAddress());
        client.setOfficePhone(request.getOfficePhone());
        client.setTin(request.getTin());
        client.setEmail(request.getEmail());
        client.setOrganisation(organisation);
        client.setIsActive(true);

        ClientEntity savedClient = clientsRepo.save(client);

        log.info("Client {} created in organisation {}", savedClient.getName(), organisation.getOrganisationName());

        return mapToResponse(savedClient);
    }

    @Override
    public ClientResponse getClientByIdWithinOrganisation(UUID clientId) throws ItemNotFoundException {

        AccountEntity currentUser = getAuthenticatedAccount();

        // Get client
        ClientEntity client = clientsRepo.findById(clientId)
                .orElseThrow(() -> new ItemNotFoundException("Client not found"));

        // Get organisation
        OrganisationEntity organisation = client.getOrganisation();

        // Validate user permissions (only OWNER/ADMIN/MEMBER can view clients)
        validateMemberPermissions(currentUser, organisation, List.of(MemberRole.OWNER, MemberRole.ADMIN, MemberRole.MEMBER));


        return mapToResponse(client);
    }

    @Override
    public Page<ClientResponse> getAllClientsWithinOrganisation(
            UUID organisationId,
            Pageable pageable
    ) throws ItemNotFoundException {

        AccountEntity currentUser = getAuthenticatedAccount();

        // Get organisation
        OrganisationEntity organisation = organisationRepo.findById(organisationId)
                .orElseThrow(() -> new ItemNotFoundException("Organisation not found"));

        validateMemberPermissions(currentUser, organisation,
                List.of(MemberRole.OWNER, MemberRole.ADMIN, MemberRole.MEMBER));

        // Get paginated active clients for this organisation
        Page<ClientEntity> clientsPage = clientsRepo.findByIsActiveTrueAndOrganisation(
                organisation, pageable);

        return clientsPage.map(this::mapToResponse);
    }


    @Override
    public List<ClientResponse> getAllClientsWithinOrganisation(UUID organisationId) throws ItemNotFoundException {

        AccountEntity currentUser = getAuthenticatedAccount();

        // Get organisation
        OrganisationEntity organisation = organisationRepo.findById(organisationId)
                .orElseThrow(() -> new ItemNotFoundException("Organisation not found"));


        validateMemberPermissions(currentUser, organisation, List.of(MemberRole.OWNER, MemberRole.ADMIN, MemberRole.MEMBER));

        // Get all active clients
        List<ClientEntity> clients = clientsRepo.findAll().stream()
                .filter(ClientEntity::getIsActive)
                .toList();

        return clients.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public ClientResponse updateClientWithinOrganisation(UUID clientId, UpdateClientRequest request) throws ItemNotFoundException {


        AccountEntity currentUser = getAuthenticatedAccount();

        // Get client
        ClientEntity client = clientsRepo.findById(clientId)
                .orElseThrow(() -> new ItemNotFoundException("Client not found"));

        // Validate user permissions (only OWNER/ADMIN can update clients)
        validateMemberPermissions(currentUser, client.getOrganisation(), List.of(MemberRole.OWNER, MemberRole.ADMIN));


        // Update fields if provided
        if (request.getName() != null && !request.getName().trim().isEmpty()) {
            client.setName(request.getName().trim());
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

        ClientEntity updatedClient = clientsRepo.save(client);

        log.info("Client {} updated", updatedClient.getName());

        return mapToResponse(updatedClient);
    }

    @Override
    public void deleteClientWithinOrganisation(UUID clientId) throws ItemNotFoundException {

        AccountEntity currentUser = getAuthenticatedAccount();

        // Get client
        ClientEntity client = clientsRepo.findById(clientId)
                .orElseThrow(() -> new ItemNotFoundException("Client not found"));

        // Validate user permissions (only OWNER/ADMIN can update clients)
        validateMemberPermissions(currentUser, client.getOrganisation(), List.of(MemberRole.OWNER, MemberRole.ADMIN));

        // Softly delete - set inactive
        client.setIsActive(false);
        clientsRepo.save(client);

        log.info("Client {} deleted (set inactive)", client.getName());
    }


    @Override
    public List<ProjectResponseForClient> getClientProjectsWithinOrganisation(UUID clientId) throws ItemNotFoundException {

        AccountEntity currentUser = getAuthenticatedAccount();

        // Get client
        ClientEntity client = clientsRepo.findById(clientId)
                .orElseThrow(() -> new ItemNotFoundException("Client not found"));

        // Validate user permissions (only OWNER/ADMIN can update clients)
        validateMemberPermissions(currentUser, client.getOrganisation(), List.of(MemberRole.OWNER, MemberRole.ADMIN));

        // Get all projects for this client
        List<ProjectEntity> projects = projectRepo.findAllByClientAndOrganisation(client, client.getOrganisation());

        // Map projects to client response format
        return projects.stream()
                .map(this::mapToProjectResponseForClient)
                .collect(Collectors.toList());
    }



    // ====================================================================
    // HELPER METHODS
    // ====================================================================

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
        response.setTotalProjects(client.getProjects().size()); 
        return response;
    }

    private ProjectResponseForClient mapToProjectResponseForClient(ProjectEntity project) {
        ProjectResponseForClient response = new ProjectResponseForClient();

        response.setProjectId(project.getProjectId());
        response.setName(project.getName());
        response.setDescription(project.getDescription());
        response.setOrganisationName(project.getOrganisation().getOrganisationName());
        response.setOrganisationId(project.getOrganisation().getOrganisationId());
        response.setStatus(project.getStatus().toString()); // Assuming status is an enum
        response.setContractNumber(project.getContractNumber());
        response.setCreatedAt(project.getCreatedAt());
        response.setUpdatedAt(project.getUpdatedAt());

        return response;
    }

    private AccountEntity getAuthenticatedAccount() throws ItemNotFoundException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return extractAccount(authentication);
    }

    private AccountEntity extractAccount(Authentication authentication) throws ItemNotFoundException {
        if (authentication != null && authentication.isAuthenticated()) {
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            String userName = userDetails.getUsername();
            return accountRepo.findByUserName(userName)
                    .orElseThrow(() -> new ItemNotFoundException("User given username does not exist"));
        }
        throw new ItemNotFoundException("User is not authenticated");
    }

    private OrganisationMember validateMemberPermissions(AccountEntity account, OrganisationEntity organisation, List<MemberRole> allowedRoles) throws ItemNotFoundException {

        OrganisationMember member = organisationMemberRepo.findByAccountAndOrganisation(account, organisation)
                .orElseThrow(() -> new ItemNotFoundException("Member is not found in organisation"));

        if (member.getStatus() != MemberStatus.ACTIVE) {
            throw new ItemNotFoundException("Member is not active");
        }

        if (!allowedRoles.contains(member.getRole())) {
            throw new ItemNotFoundException("Member has insufficient permissions");
        }

        return member;
    }
}