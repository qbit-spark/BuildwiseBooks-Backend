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
import com.qbitspark.buildwisebackend.globeadvice.exceptions.AccessDeniedException;
import com.qbitspark.buildwisebackend.globeadvice.exceptions.ItemNotFoundException;

import com.qbitspark.buildwisebackend.organisation_service.organisation_mng.entity.OrganisationEntity;
import com.qbitspark.buildwisebackend.organisation_service.organisation_mng.repo.OrganisationRepo;
import com.qbitspark.buildwisebackend.organisation_service.orgnisation_members_mng.entities.OrganisationMember;
import com.qbitspark.buildwisebackend.organisation_service.orgnisation_members_mng.enums.MemberStatus;
import com.qbitspark.buildwisebackend.organisation_service.orgnisation_members_mng.repo.OrganisationMemberRepo;
import com.qbitspark.buildwisebackend.organisation_service.roles_mng.service.PermissionCheckerService;
import com.qbitspark.buildwisebackend.projectmng_service.entity.ProjectEntity;
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
    private final PermissionCheckerService permissionChecker;

    @Override
    public ClientResponse createClientWithinOrganisation(UUID organisationId, CreateClientRequest request) throws ItemNotFoundException, AccessDeniedException {

        AccountEntity currentUser = getAuthenticatedAccount();

        OrganisationEntity organisation = organisationRepo.findById(organisationId)
                .orElseThrow(() -> new ItemNotFoundException("Organisation not found"));

        OrganisationMember member = validateOrganisationMemberAccess(currentUser, organisation);

        permissionChecker.checkMemberPermission(member, "CLIENTS", "createClient");


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

        return mapToResponse(savedClient);
    }

    @Override
    public ClientResponse getClientByIdWithinOrganisation(UUID organisationId, UUID clientId) throws ItemNotFoundException, AccessDeniedException {

        AccountEntity currentUser = getAuthenticatedAccount();

        OrganisationEntity organisation = organisationRepo.findById(organisationId).orElseThrow(() -> new ItemNotFoundException("Organisation not found"));

        ClientEntity client = clientsRepo.findByOrganisationAndClientId(organisation, clientId)
                .orElseThrow(() -> new ItemNotFoundException("Client not found"));

        OrganisationMember member = validateOrganisationMemberAccess(currentUser, organisation);

        permissionChecker.checkMemberPermission(member,"CLIENTS","viewClients");

        return mapToResponse(client);
    }

    @Override
    public Page<ClientResponse> getAllClientsWithinOrganisation(UUID organisationId, Pageable pageable) throws ItemNotFoundException, AccessDeniedException {

        AccountEntity currentUser = getAuthenticatedAccount();

        OrganisationEntity organisation = organisationRepo.findById(organisationId).orElseThrow(() -> new ItemNotFoundException("Organisation not found"));

        OrganisationMember member = validateOrganisationMemberAccess(currentUser, organisation);

        permissionChecker.checkMemberPermission(member,"CLIENTS", "viewClients");

        Page<ClientEntity> clientsPage = clientsRepo.findByIsActiveTrueAndOrganisation(organisation, pageable);

        return clientsPage.map(this::mapToResponse);
    }


    @Override
    public List<ClientResponse> getAllClientsWithinOrganisation(UUID organisationId) throws ItemNotFoundException, AccessDeniedException {

        AccountEntity currentUser = getAuthenticatedAccount();

        OrganisationEntity organisation = organisationRepo.findById(organisationId)
                .orElseThrow(() -> new ItemNotFoundException("Organisation not found"));

        OrganisationMember member = validateOrganisationMemberAccess(currentUser, organisation);

        permissionChecker.checkMemberPermission(member, "CLIENTS", "viewClients");

        List<ClientEntity> clients = clientsRepo.findAll().stream()
                .filter(ClientEntity::getIsActive)
                .toList();

        return clients.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public ClientResponse updateClientWithinOrganisation(UUID organisationId, UUID clientId, UpdateClientRequest request) throws ItemNotFoundException, AccessDeniedException {

        AccountEntity currentUser = getAuthenticatedAccount();

        OrganisationEntity organisation = organisationRepo.findById(organisationId).orElseThrow(() -> new ItemNotFoundException("Organisation not found"));

        ClientEntity client = clientsRepo.findByOrganisationAndClientId(organisation, clientId)
                .orElseThrow(() -> new ItemNotFoundException("Client not found"));

        OrganisationMember member = validateOrganisationMemberAccess(currentUser, organisation);

        permissionChecker.checkMemberPermission(member, "CLIENTS", "updateClient");

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

        return mapToResponse(updatedClient);
    }

    @Override
    public void deleteClientWithinOrganisation(UUID organisationId, UUID clientId) throws ItemNotFoundException, AccessDeniedException {

        AccountEntity currentUser = getAuthenticatedAccount();

        OrganisationEntity organisation = organisationRepo.findById(organisationId).orElseThrow(() -> new ItemNotFoundException("Organisation not found"));

        ClientEntity client = clientsRepo.findByOrganisationAndClientId(organisation, clientId)
                .orElseThrow(() -> new ItemNotFoundException("Client not found"));

        OrganisationMember member = validateOrganisationMemberAccess(currentUser, organisation);

        permissionChecker.checkMemberPermission(member, "CLIENTS", "deleteClient");

        client.setIsActive(false);
        clientsRepo.save(client);
    }


    @Override
    public List<ProjectResponseForClient> getClientProjectsWithinOrganisation(UUID organisationId, UUID clientId) throws ItemNotFoundException, AccessDeniedException {

        AccountEntity currentUser = getAuthenticatedAccount();

        OrganisationEntity organisation = organisationRepo.findById(organisationId).orElseThrow(() -> new ItemNotFoundException("Organisation not found"));

        ClientEntity client = clientsRepo.findByOrganisationAndClientId(organisation, clientId)
                .orElseThrow(() -> new ItemNotFoundException("Client not found"));

        OrganisationMember member = validateOrganisationMemberAccess(currentUser, organisation);

        permissionChecker.checkMemberPermission(member, "CLIENTS", "viewClientProjects");

        List<ProjectEntity> projects = projectRepo.findAllByClientAndOrganisation(client, client.getOrganisation());

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
        response.setStatus(project.getStatus().toString());
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


    private OrganisationMember validateOrganisationMemberAccess(AccountEntity account, OrganisationEntity organisation) throws ItemNotFoundException {
        OrganisationMember member = organisationMemberRepo.findByAccountAndOrganisation(account, organisation)
                .orElseThrow(() -> new ItemNotFoundException("Member is not belong to this organisation"));

        if (member.getStatus() != MemberStatus.ACTIVE) {
            throw new ItemNotFoundException("Member is not active");
        }

        return member;
    }

}