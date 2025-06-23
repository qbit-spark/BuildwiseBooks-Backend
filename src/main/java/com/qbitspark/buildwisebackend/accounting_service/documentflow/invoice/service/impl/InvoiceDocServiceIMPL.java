package com.qbitspark.buildwisebackend.accounting_service.documentflow.invoice.service.impl;

import com.qbitspark.buildwisebackend.accounting_service.coa.entity.JournalEntry;
import com.qbitspark.buildwisebackend.accounting_service.documentflow.invoice.entity.InvoiceDocEntity;
import com.qbitspark.buildwisebackend.accounting_service.documentflow.invoice.entity.InvoiceLineItemEntity;
import com.qbitspark.buildwisebackend.accounting_service.documentflow.invoice.enums.InvoiceStatus;
import com.qbitspark.buildwisebackend.accounting_service.documentflow.invoice.paylaod.*;
import com.qbitspark.buildwisebackend.accounting_service.documentflow.invoice.repo.InvoiceDocRepo;
import com.qbitspark.buildwisebackend.accounting_service.documentflow.invoice.service.InvoiceDocService;
import com.qbitspark.buildwisebackend.accounting_service.documentflow.invoice.service.InvoiceNumberService;
import com.qbitspark.buildwisebackend.accounting_service.transactions_pipeline.events.user_transaction.InvoiceEvent;
import com.qbitspark.buildwisebackend.accounting_service.transactions_pipeline.transaction_service.TransactionService;
import com.qbitspark.buildwisebackend.authentication_service.Repository.AccountRepo;
import com.qbitspark.buildwisebackend.authentication_service.entity.AccountEntity;
import com.qbitspark.buildwisebackend.clientsmng_service.entity.ClientEntity;
import com.qbitspark.buildwisebackend.clientsmng_service.repo.ClientsRepo;
import com.qbitspark.buildwisebackend.globeadvice.exceptions.AccessDeniedException;
import com.qbitspark.buildwisebackend.globeadvice.exceptions.ItemNotFoundException;
import com.qbitspark.buildwisebackend.organisation_service.organisation_mng.entity.OrganisationEntity;
import com.qbitspark.buildwisebackend.organisation_service.organisation_mng.repo.OrganisationRepo;
import com.qbitspark.buildwisebackend.organisation_service.orgnisation_members_mng.entities.OrganisationMember;
import com.qbitspark.buildwisebackend.organisation_service.orgnisation_members_mng.enums.MemberStatus;
import com.qbitspark.buildwisebackend.organisation_service.orgnisation_members_mng.repo.OrganisationMemberRepo;
import com.qbitspark.buildwisebackend.projectmng_service.entity.ProjectEntity;
import com.qbitspark.buildwisebackend.projectmng_service.entity.ProjectTeamMemberEntity;
import com.qbitspark.buildwisebackend.projectmng_service.enums.TeamMemberRole;
import com.qbitspark.buildwisebackend.projectmng_service.repo.ProjectRepo;
import com.qbitspark.buildwisebackend.projectmng_service.repo.ProjectTeamMemberRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class InvoiceDocServiceIMPL implements InvoiceDocService {

    private final InvoiceDocRepo invoiceDocRepo;
    private final ProjectRepo projectRepo;
    private final OrganisationRepo organisationRepo;
    private final AccountRepo accountRepo;
    private final TransactionService transactionService;
    private final OrganisationMemberRepo organisationMemberRepo;
    private final ClientsRepo clientsRepo;
    private final ProjectTeamMemberRepo projectTeamMemberRepo;
    private final InvoiceNumberService invoiceNumberService;


    @Override
    public SummaryInvoiceDocResponse createInvoice(UUID organisationId, CreateInvoiceDocRequest request)
            throws ItemNotFoundException, AccessDeniedException {

        AccountEntity currentUser = getAuthenticatedAccount();

        ProjectEntity project = projectRepo.findById(request.getProjectId())
                .orElseThrow(() -> new ItemNotFoundException("Project not found"));

        OrganisationEntity organisation = organisationRepo.findById(organisationId)
                .orElseThrow(() -> new ItemNotFoundException("Organisation not found"));

        // Validate project belongs to this organisation
        validateProject(project, organisation);

        // Validate user is an active member of this organisation
        validateProjectMemberPermissions(currentUser, project,
                List.of(TeamMemberRole.ACCOUNTANT, TeamMemberRole.OWNER, TeamMemberRole.PROJECT_MANAGER));

        // Validate client exists
        ClientEntity client = validateClientExists(request.getClientId(), project.getOrganisation());

        String invoiceNumber = invoiceNumberService.generateInvoiceNumber(project, client, organisation);

        InvoiceDocEntity invoice = new InvoiceDocEntity();
        invoice.setProject(project);
        invoice.setClient(client);
        invoice.setInvoiceType(request.getInvoiceType());
        invoice.setInvoiceStatus(InvoiceStatus.DRAFT);
        invoice.setDateOfIssue(request.getDateOfIssue());
        invoice.setDueDate(request.getDueDate());
        invoice.setReference(request.getReference());
        invoice.setOrganisation(organisation);
        invoice.setDiscountAmount(request.getDiscountAmount());
        invoice.setTaxAmount(request.getTaxAmount());
        invoice.setCurrency(request.getCurrency());
        invoice.setCreatedBy(currentUser.getId());
        invoice.setUpdatedBy(currentUser.getId());
        invoice.setInvoiceNumber(invoiceNumber);

        List<InvoiceLineItemEntity> lineItems = new ArrayList<>();
        BigDecimal subtotal = BigDecimal.ZERO;

        for (InvoiceLineItemRequest lineRequest : request.getLineItems()) {
            InvoiceLineItemEntity lineItem = new InvoiceLineItemEntity();
            lineItem.setInvoice(invoice);
            lineItem.setDescription(lineRequest.getDescription());
            lineItem.setRate(lineRequest.getRate());
            lineItem.setQuantity(lineRequest.getQuantity());

            BigDecimal lineTotal = lineRequest.getRate().multiply(lineRequest.getQuantity());
            lineItem.setLineTotal(lineTotal);

            lineItem.setTaxType(lineRequest.getTaxType());
            lineItem.setTaxRate(lineRequest.getTaxRate());

            BigDecimal taxAmount = lineTotal.multiply(lineRequest.getTaxRate().divide(BigDecimal.valueOf(100)));
            lineItem.setTaxAmount(taxAmount);

            lineItem.setUnitOfMeasure(lineRequest.getUnitOfMeasure());
            lineItem.setLineOrder(lineRequest.getLineOrder());

            lineItems.add(lineItem);
            subtotal = subtotal.add(lineTotal);
        }

        invoice.setLineItems(lineItems);
        invoice.setSubtotal(subtotal);

        BigDecimal totalAmount = subtotal.add(invoice.getTaxAmount()).subtract(invoice.getDiscountAmount());
        invoice.setTotalAmount(totalAmount);
        invoice.setAmountDue(totalAmount);

        InvoiceDocEntity savedInvoice = invoiceDocRepo.save(invoice);

        log.info("Invoice {} created for project {}", savedInvoice.getInvoiceNumber(), project.getName());

        // Use the shared mapping method instead of inline mapping
        return mapToCreateInvoiceDocResponse(savedInvoice);
    }

    @Override
    public PreviewInvoiceNumberResponse previewInvoiceNumber(UUID organisationId, PreviewInvoiceNumberRequest request) throws ItemNotFoundException, AccessDeniedException {

        AccountEntity currentUser = getAuthenticatedAccount();

        ProjectEntity project = projectRepo.findById(request.getProjectId())
                .orElseThrow(() -> new ItemNotFoundException("Project not found"));

        OrganisationEntity organisation = organisationRepo.findById(organisationId)
                .orElseThrow(() -> new ItemNotFoundException("Organisation not found"));


        // Validate project belongs to this organisation
        validateProject(project, organisation);


        // Validate user is an active member of this organisation
        validateProjectMemberPermissions(currentUser, project, List.of(TeamMemberRole.ACCOUNTANT, TeamMemberRole.OWNER, TeamMemberRole.PROJECT_MANAGER));


        //Validate client exists
        ClientEntity client = validateClientExists(request.getClientId(), project.getOrganisation());

        String previewInvoiceNumber = invoiceNumberService.previewNextInvoiceNumber(project, client, organisation);

        PreviewInvoiceNumberResponse response = new PreviewInvoiceNumberResponse();
        response.setNextInvoiceNumber(previewInvoiceNumber);
        response.setOrganisationName(organisation.getOrganisationName());
        response.setProjectName(project.getName());
        response.setClientName(client.getName());
        response.setProjectCode(project.getProjectCode());

        return response;
    }


    @Override
    public InvoiceDocResponse getInvoiceById(UUID organisationId, UUID invoiceId)throws ItemNotFoundException, AccessDeniedException {

        AccountEntity currentUser = getAuthenticatedAccount();

        OrganisationEntity organisation = organisationRepo.findById(organisationId)
                .orElseThrow(() -> new ItemNotFoundException("Organisation not found"));

        //Validate invoice exists
        InvoiceDocEntity invoice = invoiceDocRepo.findByIdAndOrganisation(invoiceId, organisation)
                .orElseThrow(() -> new ItemNotFoundException("Invoice not found"));

        ProjectEntity project = invoice.getProject();


        // Validate project belongs to this organisation
        validateProject(project, organisation);


        // Validate user is an active member of this organisation
        validateProjectMemberPermissions(currentUser, project, List.of(TeamMemberRole.ACCOUNTANT, TeamMemberRole.OWNER, TeamMemberRole.PROJECT_MANAGER));


        return mapToInvoiceResponse(invoice);
    }

    @Override
    public InvoiceDocResponse getInvoiceByNumber(UUID organisationId, String invoiceNumber)throws ItemNotFoundException, AccessDeniedException {

        AccountEntity currentUser = getAuthenticatedAccount();

        OrganisationEntity organisation = organisationRepo.findById(organisationId)
                .orElseThrow(() -> new ItemNotFoundException("Organisation not found"));

        //Validate invoice exists
        InvoiceDocEntity invoice = invoiceDocRepo.findByInvoiceNumberAndOrganisation(invoiceNumber, organisation)
                .orElseThrow(() -> new ItemNotFoundException("Invoice with given number not found in this organisation"));

        ProjectEntity project = invoice.getProject();


        // Validate project belongs to this organisation
        validateProject(project, organisation);


        // Validate user is an active member of this organisation
        validateProjectMemberPermissions(currentUser, project, List.of(TeamMemberRole.ACCOUNTANT, TeamMemberRole.OWNER, TeamMemberRole.PROJECT_MANAGER));


        return mapToInvoiceResponse(invoice);
    }

    @Override
    public Page<SummaryInvoiceDocResponse> getProjectInvoices(UUID organisationId, UUID projectId, Pageable pageable)
            throws ItemNotFoundException, AccessDeniedException {

        AccountEntity currentUser = getAuthenticatedAccount();

        ProjectEntity project = projectRepo.findById(projectId)
                .orElseThrow(() -> new ItemNotFoundException("Project not found"));

        OrganisationEntity organisation = organisationRepo.findById(organisationId)
                .orElseThrow(() -> new ItemNotFoundException("Organisation not found"));

        validateProject(project, organisation);


        validateProjectMemberPermissions(currentUser, project,
                List.of(TeamMemberRole.ACCOUNTANT, TeamMemberRole.OWNER, TeamMemberRole.PROJECT_MANAGER));

        Page<InvoiceDocEntity> invoicePage = invoiceDocRepo.findAllByProject(project, pageable);

        return invoicePage.map(this::mapToCreateInvoiceDocResponse);
    }

    @Override
    public List<SummaryInvoiceDocResponse> getClientInvoices(UUID organisationId, UUID clientId) throws ItemNotFoundException, AccessDeniedException {

        AccountEntity currentUser = getAuthenticatedAccount();

        OrganisationEntity organisation = organisationRepo.findById(organisationId)
                .orElseThrow(() -> new ItemNotFoundException("Organisation not found"));

        // Validate user is an active member of this organisation
        validateOrganisationMemberAccess(currentUser, organisation);

        // Validate client exists
        ClientEntity client = validateClientExists(clientId, organisation);

        // Get all invoices for the client and map to response DTOs
        return invoiceDocRepo.findAllByClient(client).stream()
                .map(this::mapToCreateInvoiceDocResponse)
                .collect(Collectors.toList());

    }


    @Override
    public List<InvoiceDocResponse> getOrganisationInvoices(UUID organisationId) throws ItemNotFoundException {
        OrganisationEntity organisation = organisationRepo.findById(organisationId)
                .orElseThrow(() -> new ItemNotFoundException("Organisation not found"));

        return invoiceDocRepo.findAll().stream()
                .filter(invoice -> invoice.getOrganisation().getOrganisationId().equals(organisationId))
                .map(this::mapToInvoiceResponse)
                .collect(Collectors.toList());
    }

    @Override
    public void sendInvoice(UUID invoiceId) throws Exception {
        InvoiceDocEntity invoice = invoiceDocRepo.findById(invoiceId)
                .orElseThrow(() -> new ItemNotFoundException("Invoice not found"));

        if (!invoice.getInvoiceStatus().canBeSent()) {
            throw new IllegalStateException("Invoice cannot be sent in current status: " + invoice.getInvoiceStatus());
        }

        InvoiceEvent invoiceEvent = createInvoiceEvent(invoice);
        JournalEntry journalEntry = transactionService.processBusinessEvent(invoiceEvent);

        invoice.setInvoiceStatus(InvoiceStatus.SENT);
        invoice.setUpdatedBy(getAuthenticatedAccount().getId());
        invoiceDocRepo.save(invoice);

        log.info("Invoice {} sent and journal entry {} created",
                invoice.getInvoiceNumber(), journalEntry.getId());
    }

    @Override
    public void approveInvoice(UUID invoiceId) throws Exception {
        InvoiceDocEntity invoice = invoiceDocRepo.findById(invoiceId)
                .orElseThrow(() -> new ItemNotFoundException("Invoice not found"));

        if (invoice.getInvoiceStatus() != InvoiceStatus.DRAFT &&
                invoice.getInvoiceStatus() != InvoiceStatus.PENDING_APPROVAL) {
            throw new IllegalStateException("Invoice cannot be approved in current status: " + invoice.getInvoiceStatus());
        }

        invoice.setInvoiceStatus(InvoiceStatus.APPROVED);
        invoice.setUpdatedBy(getAuthenticatedAccount().getId());
        invoiceDocRepo.save(invoice);

        log.info("Invoice {} approved", invoice.getInvoiceNumber());
    }

    private InvoiceEvent createInvoiceEvent(InvoiceDocEntity invoice) {
        InvoiceEvent event = new InvoiceEvent();
        event.setOrganisationId(invoice.getOrganisation().getOrganisationId());
        event.setProjectId(invoice.getProject().getProjectId());
        event.setCustomerId(invoice.getClient().getClientId());
        event.setTotalAmount(invoice.getTotalAmount());
        event.setTaxAmount(invoice.getTaxAmount());
        event.setDescription("Invoice: " + invoice.getInvoiceNumber());
        event.setReferenceNumber(invoice.getInvoiceNumber());

        for (InvoiceLineItemEntity lineItem : invoice.getLineItems()) {
            event.addLineItem(
                    lineItem.getDescription(),
                    lineItem.getQuantity(),
                    lineItem.getRate(),
                    getDefaultRevenueAccountId()
            );
        }

        return event;
    }

    private UUID getDefaultRevenueAccountId() {
        return UUID.fromString("00000000-0000-0000-0000-000000000001");
    }

    private String generateInvoiceNumber(ProjectEntity project, OrganisationEntity organisation) {
        return "INV-" + organisation.getOrganisationId().toString().substring(0, 8) +
                "-" + project.getProjectId().toString().substring(0, 8) +
                "-" + System.currentTimeMillis();
    }

    private InvoiceDocResponse mapToInvoiceResponse(InvoiceDocEntity invoice) {
        InvoiceDocResponse response = new InvoiceDocResponse();
        response.setId(invoice.getId());
        response.setInvoiceNumber(invoice.getInvoiceNumber());
        response.setProjectId(invoice.getProject().getProjectId());
        response.setProjectName(invoice.getProject().getName());
        response.setClientId(invoice.getClient().getClientId());
        response.setClientName(invoice.getClient().getName());
        response.setInvoiceType(invoice.getInvoiceType());
        response.setInvoiceStatus(invoice.getInvoiceStatus());
        response.setDateOfIssue(invoice.getDateOfIssue());
        response.setDueDate(invoice.getDueDate());
        response.setReference(invoice.getReference());
        response.setOrganisationId(invoice.getOrganisation().getOrganisationId());
        response.setOrganisationName(invoice.getOrganisation().getOrganisationName());
        response.setSubtotal(invoice.getSubtotal());
        response.setDiscountAmount(invoice.getDiscountAmount());
        response.setTaxAmount(invoice.getTaxAmount());
        response.setTotalAmount(invoice.getTotalAmount());
        response.setAmountPaid(invoice.getAmountPaid());
        response.setCreditApplied(invoice.getCreditApplied());
        response.setAmountDue(invoice.getAmountDue());
        response.setCurrency(invoice.getCurrency());
        response.setCreatedAt(invoice.getUpdatedAt());
        response.setUpdatedAt(invoice.getUpdatedAt());

        List<InvoiceLineItemResponse> lineItemResponses = invoice.getLineItems().stream()
                .map(this::mapToLineItemResponse)
                .collect(Collectors.toList());
        response.setLineItems(lineItemResponses);

        if (invoice.getCreatedBy() != null) {
            accountRepo.findById(invoice.getCreatedBy())
                    .ifPresent(account -> response.setCreatedByUserName(account.getUserName()));
        }

        return response;
    }

    private InvoiceLineItemResponse mapToLineItemResponse(InvoiceLineItemEntity lineItem) {
        InvoiceLineItemResponse response = new InvoiceLineItemResponse();
        response.setId(lineItem.getId());
        response.setDescription(lineItem.getDescription());
        response.setRate(lineItem.getRate());
        response.setQuantity(lineItem.getQuantity());
        response.setLineTotal(lineItem.getLineTotal());
        response.setTaxType(lineItem.getTaxType());
        response.setTaxRate(lineItem.getTaxRate());
        response.setTaxAmount(lineItem.getTaxAmount());
        response.setUnitOfMeasure(lineItem.getUnitOfMeasure());
        response.setLineOrder(lineItem.getLineOrder());
        return response;
    }

    private AccountEntity getAuthenticatedAccount() throws ItemNotFoundException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            String userName = userDetails.getUsername();
            return accountRepo.findByUserName(userName)
                    .orElseThrow(() -> new ItemNotFoundException("User not found"));
        }
        throw new ItemNotFoundException("User not authenticated");
    }

    private ProjectTeamMemberEntity validateProjectMemberPermissions(AccountEntity account, ProjectEntity project, List<TeamMemberRole> allowedRoles) throws ItemNotFoundException, AccessDeniedException {

        // Step 1: Find the organisation member
        OrganisationMember organisationMember = organisationMemberRepo.findByAccountAndOrganisation(account, project.getOrganisation())
                .orElseThrow(() -> new ItemNotFoundException("Member is not found in organisation"));

        // Step 2: Check if an organisation member is active
        if (organisationMember.getStatus() != MemberStatus.ACTIVE) {
            throw new ItemNotFoundException("Member is not active");
        }


        // Step 3: Get the project team member details
        ProjectTeamMemberEntity projectTeamMember = projectTeamMemberRepo.findByOrganisationMemberAndProject(organisationMember, project)
                .orElseThrow(() -> new ItemNotFoundException("Project team member not found"));

        // Step 5: Check if the member has one of the allowed roles
        if (!allowedRoles.contains(projectTeamMember.getRole())) {
            throw new AccessDeniedException("Member has insufficient permissions for this operation");
        }

        return projectTeamMember;
    }

    private ClientEntity validateClientExists(UUID clientId, OrganisationEntity organisation) throws ItemNotFoundException {

        return clientsRepo.findClientEntitiesByClientIdAndOrganisation(clientId, organisation)
                .orElseThrow(() -> new ItemNotFoundException("Client not found in this organisation"));
    }


    private OrganisationMember validateOrganisationMemberAccess(AccountEntity account, OrganisationEntity organisation) throws ItemNotFoundException {
        OrganisationMember member = organisationMemberRepo.findByAccountAndOrganisation(account, organisation)
                .orElseThrow(() -> new ItemNotFoundException("Member is not belong to this organisation"));

        if (member.getStatus() != MemberStatus.ACTIVE) {
            throw new ItemNotFoundException("Member is not active");
        }

        return member;
    }

    private void validateProject(ProjectEntity project, OrganisationEntity organisation) throws ItemNotFoundException {
        if (!project.getOrganisation().getOrganisationId().equals(organisation.getOrganisationId())) {
            throw new ItemNotFoundException("Project does not belong to this organisation");
        }
    }

    private SummaryInvoiceDocResponse mapToCreateInvoiceDocResponse(InvoiceDocEntity invoice) {
        SummaryInvoiceDocResponse response = new SummaryInvoiceDocResponse();
        response.setInvoiceId(invoice.getId());
        response.setInvoiceNumber(invoice.getInvoiceNumber());
        response.setStatus(invoice.getInvoiceStatus());
        response.setTotalAmount(invoice.getTotalAmount());
        response.setProjectName(invoice.getProject().getName());
        response.setLineItemCount(invoice.getLineItems().size());
        response.setClientName(invoice.getClient().getName());
        return response;
    }

}