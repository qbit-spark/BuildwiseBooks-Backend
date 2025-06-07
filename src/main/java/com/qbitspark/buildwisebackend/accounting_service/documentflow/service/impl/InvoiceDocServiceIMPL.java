package com.qbitspark.buildwisebackend.accounting_service.documentflow.service.impl;

import com.qbitspark.buildwisebackend.accounting_service.coa.entity.JournalEntry;
import com.qbitspark.buildwisebackend.accounting_service.documentflow.entity.InvoiceDocEntity;
import com.qbitspark.buildwisebackend.accounting_service.documentflow.entity.InvoiceLineItemEntity;
import com.qbitspark.buildwisebackend.accounting_service.documentflow.enums.InvoiceStatus;
import com.qbitspark.buildwisebackend.accounting_service.documentflow.paylaod.*;
import com.qbitspark.buildwisebackend.accounting_service.documentflow.repo.InvoiceDocRepo;
import com.qbitspark.buildwisebackend.accounting_service.documentflow.service.InvoiceDocService;
import com.qbitspark.buildwisebackend.accounting_service.transactions_pipeline.events.user_transaction.InvoiceEvent;
import com.qbitspark.buildwisebackend.accounting_service.transactions_pipeline.transaction_service.TransactionService;
import com.qbitspark.buildwisebackend.authentication_service.Repository.AccountRepo;
import com.qbitspark.buildwisebackend.authentication_service.entity.AccountEntity;
import com.qbitspark.buildwisebackend.globeadvice.exceptions.ItemNotFoundException;
import com.qbitspark.buildwisebackend.organisation_service.organisation_mng.entity.OrganisationEntity;
import com.qbitspark.buildwisebackend.organisation_service.organisation_mng.repo.OrganisationRepo;
import com.qbitspark.buildwisebackend.organisation_service.orgnisation_members_mng.entities.OrganisationMember;
import com.qbitspark.buildwisebackend.organisation_service.orgnisation_members_mng.enums.MemberStatus;
import com.qbitspark.buildwisebackend.organisation_service.orgnisation_members_mng.repo.OrganisationMemberRepo;
import com.qbitspark.buildwisebackend.projectmng_service.entity.ProjectEntity;
import com.qbitspark.buildwisebackend.projectmng_service.repo.ProjectRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
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


    @Override
    public CreateInvoiceDocResponse createInvoice(CreateInvoiceDocRequest request) throws ItemNotFoundException {

        AccountEntity currentUser = getAuthenticatedAccount();
        ProjectEntity project = projectRepo.findById(request.getProjectId())
                .orElseThrow(() -> new ItemNotFoundException("Project not found"));

        OrganisationEntity organisation = organisationRepo.findById(request.getOrganisationId())
                .orElseThrow(() -> new ItemNotFoundException("Organisation not found"));

        // Validate user is an active member of this organisation
        validateUserIsActiveMember(currentUser, organisation);


        // Validate project belongs to this organisation
        if (!project.getOrganisation().getOrganisationId().equals(organisation.getOrganisationId())) {
            throw new ItemNotFoundException("Project does not belong to this organisation");
        }

        InvoiceDocEntity invoice = new InvoiceDocEntity();
        invoice.setInvoiceNumber(generateInvoiceNumber(project, organisation));
        invoice.setProject(project);
        invoice.setClientId(request.getClientId());
        invoice.setClientName("Name no set");
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

        CreateInvoiceDocResponse response = new CreateInvoiceDocResponse();
        response.setInvoiceId(savedInvoice.getId());
        response.setInvoiceNumber(savedInvoice.getInvoiceNumber());
        response.setStatus(savedInvoice.getInvoiceStatus().getDisplayName());
        response.setTotalAmount(savedInvoice.getTotalAmount());
        response.setProjectName(project.getName());
        response.setClientName(savedInvoice.getClientName());

        return response;
    }

    @Override
    public InvoiceDocResponse getInvoiceById(UUID invoiceId) throws ItemNotFoundException {
        InvoiceDocEntity invoice = invoiceDocRepo.findById(invoiceId)
                .orElseThrow(() -> new ItemNotFoundException("Invoice not found"));

        return mapToInvoiceResponse(invoice);
    }

    @Override
    public List<InvoiceDocResponse> getProjectInvoices(UUID projectId) throws ItemNotFoundException {
        ProjectEntity project = projectRepo.findById(projectId)
                .orElseThrow(() -> new ItemNotFoundException("Project not found"));

        return invoiceDocRepo.findAll().stream()
                .filter(invoice -> invoice.getProject().getProjectId().equals(projectId))
                .map(this::mapToInvoiceResponse)
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
        event.setCustomerId(invoice.getClientId());
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
        String projectCode = project.getName().replaceAll("[^A-Z0-9]", "").substring(0, Math.min(4, project.getName().length()));
        String orgCode = organisation.getOrganisationName().replaceAll("[^A-Z0-9]", "").substring(0, Math.min(3, organisation.getOrganisationName().length()));
        String timestamp = String.valueOf(System.currentTimeMillis()).substring(8);

        return String.format("INV-%s-%s-%s", orgCode, projectCode, timestamp);
    }

    private InvoiceDocResponse mapToInvoiceResponse(InvoiceDocEntity invoice) {
        InvoiceDocResponse response = new InvoiceDocResponse();
        response.setId(invoice.getId());
        response.setInvoiceNumber(invoice.getInvoiceNumber());
        response.setProjectId(invoice.getProject().getProjectId());
        response.setProjectName(invoice.getProject().getName());
        response.setClientId(invoice.getClientId());
        response.setClientName(invoice.getClientName());
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

    private void validateUserIsActiveMember(AccountEntity user, OrganisationEntity organisation) throws ItemNotFoundException {
        Optional<OrganisationMember> memberOptional = organisationMemberRepo
                .findByAccountAndOrganisation(user, organisation);

        if (memberOptional.isEmpty()) {
            throw new ItemNotFoundException("User is not a member of this organisation");
        }

        OrganisationMember member = memberOptional.get();
        if (member.getStatus() != MemberStatus.ACTIVE) {
            throw new ItemNotFoundException("User membership is not active in this organisation");
        }
    }

}