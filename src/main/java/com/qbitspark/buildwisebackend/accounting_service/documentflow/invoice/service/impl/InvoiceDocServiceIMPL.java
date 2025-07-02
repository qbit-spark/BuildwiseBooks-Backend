package com.qbitspark.buildwisebackend.accounting_service.documentflow.invoice.service.impl;

import com.qbitspark.buildwisebackend.accounting_service.documentflow.invoice.entity.InvoiceDocEntity;
import com.qbitspark.buildwisebackend.accounting_service.documentflow.invoice.entity.InvoiceLineItemEntity;
import com.qbitspark.buildwisebackend.accounting_service.documentflow.invoice.entity.embedings.InvoiceTaxDetail;
import com.qbitspark.buildwisebackend.accounting_service.documentflow.invoice.paylaod.*;
import com.qbitspark.buildwisebackend.accounting_service.documentflow.invoice.repo.InvoiceDocRepo;
import com.qbitspark.buildwisebackend.accounting_service.documentflow.invoice.service.InvoiceDocService;
import com.qbitspark.buildwisebackend.accounting_service.documentflow.invoice.service.InvoiceNumberService;
import com.qbitspark.buildwisebackend.accounting_service.tax_mng.entity.TaxEntity;
import com.qbitspark.buildwisebackend.accounting_service.tax_mng.repo.TaxRepo;
import com.qbitspark.buildwisebackend.authentication_service.Repository.AccountRepo;
import com.qbitspark.buildwisebackend.authentication_service.entity.AccountEntity;
import com.qbitspark.buildwisebackend.clientsmng_service.entity.ClientEntity;
import com.qbitspark.buildwisebackend.globeadvice.exceptions.AccessDeniedException;
import com.qbitspark.buildwisebackend.globeadvice.exceptions.ItemNotFoundException;
import com.qbitspark.buildwisebackend.organisation_service.organisation_mng.entity.OrganisationEntity;
import com.qbitspark.buildwisebackend.organisation_service.organisation_mng.repo.OrganisationRepo;
import com.qbitspark.buildwisebackend.organisation_service.orgnisation_members_mng.entities.OrganisationMember;
import com.qbitspark.buildwisebackend.organisation_service.orgnisation_members_mng.enums.MemberRole;
import com.qbitspark.buildwisebackend.organisation_service.orgnisation_members_mng.enums.MemberStatus;
import com.qbitspark.buildwisebackend.organisation_service.orgnisation_members_mng.repo.OrganisationMemberRepo;
import com.qbitspark.buildwisebackend.projectmng_service.entity.ProjectEntity;
import com.qbitspark.buildwisebackend.projectmng_service.entity.ProjectTeamMemberEntity;
import com.qbitspark.buildwisebackend.projectmng_service.enums.TeamMemberRole;
import com.qbitspark.buildwisebackend.projectmng_service.repo.ProjectRepo;
import com.qbitspark.buildwisebackend.projectmng_service.repo.ProjectTeamMemberRepo;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.javamoney.moneta.Money;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.money.MonetaryAmount;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
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
    private final OrganisationMemberRepo organisationMemberRepo;
    private final ProjectTeamMemberRepo projectTeamMemberRepo;
    private final InvoiceNumberService invoiceNumberService;
    private final TaxRepo taxRepo;

    @Override
    @Transactional
    public InvoiceDocResponse createInvoiceWithAttachments(UUID organisationId, CreateInvoiceDocRequest request, List<MultipartFile> attachments) throws ItemNotFoundException, AccessDeniedException {

        AccountEntity currentUser = getAuthenticatedAccount();

        ProjectEntity project = projectRepo.findById(request.getProjectId())
                .orElseThrow(() -> new ItemNotFoundException("Project not found"));

        OrganisationEntity organisation = organisationRepo.findById(organisationId)
                .orElseThrow(() -> new ItemNotFoundException("Organisation not found"));

        validateProject(project, organisation);

        validateProjectMemberPermissions(currentUser, project,
                List.of(TeamMemberRole.ACCOUNTANT, TeamMemberRole.OWNER, TeamMemberRole.PROJECT_MANAGER));

        ClientEntity client = project.getClient();

        String invoiceNumber = invoiceNumberService.generateInvoiceNumber(project, client, organisation);

        // Create the invoice entity
        InvoiceDocEntity invoice = InvoiceDocEntity.builder()
                .invoiceNumber(invoiceNumber)
                .project(project)
                .client(client)
                .organisation(organisation)
                .dateOfIssue(request.getDateOfIssue())
                .dueDate(request.getDueDate())
                .reference(request.getReference())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .createdBy(currentUser.getId())
                .updatedBy(currentUser.getId())
                .build();

        // Create and add line items
        List<InvoiceLineItemEntity> lineItems = createLineItems(request.getLineItems(), invoice);
        invoice.setLineItems(lineItems);

        // Calculate totals using Money API for precision
        calculateInvoiceTotals(invoice, request.getTaxesToApply(), request.getCreditApplied());

        // Save the invoice
        InvoiceDocEntity savedInvoice = invoiceDocRepo.save(invoice);

        // Handle attachments if provided
        if (attachments != null && !attachments.isEmpty()) {

        }

        // Convert to response
        return mapToInvoiceResponse(savedInvoice, currentUser, request.getCreditApplied());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<SummaryInvoiceDocResponse> getAllInvoicesForOrganisation(UUID organisationId, int page, int size) throws ItemNotFoundException, AccessDeniedException {

        AccountEntity currentUser = getAuthenticatedAccount();
        OrganisationEntity organisation = organisationRepo.findById(organisationId)
                .orElseThrow(() -> new ItemNotFoundException("Organisation not found"));

        validateOrganisationMemberAccess(currentUser, organisation);

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<InvoiceDocEntity> invoicePage = invoiceDocRepo.findAllByOrganisation(organisation, pageable);

        return invoicePage.map(this::mapToSummaryResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<SummaryInvoiceDocResponse> getAllInvoicesForProject(UUID organisationId, UUID projectId, int page, int size) throws ItemNotFoundException, AccessDeniedException {

        AccountEntity currentUser = getAuthenticatedAccount();

        ProjectEntity project = projectRepo.findById(projectId)
                .orElseThrow(() -> new ItemNotFoundException("Project not found"));

        OrganisationEntity organisation = organisationRepo.findById(organisationId)
                .orElseThrow(() -> new ItemNotFoundException("Organisation not found"));

        validateProject(project, organisation);

        validateProjectMemberPermissions(currentUser, project,
                List.of(TeamMemberRole.ACCOUNTANT, TeamMemberRole.OWNER, TeamMemberRole.PROJECT_MANAGER));

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<InvoiceDocEntity> invoicePage = invoiceDocRepo.findAllByProject(project, pageable);

        return invoicePage.map(this::mapToSummaryResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public InvoiceDocResponse getInvoiceById(UUID organisationId, UUID invoiceId) throws ItemNotFoundException, AccessDeniedException {

        AccountEntity currentUser = getAuthenticatedAccount();
        OrganisationEntity organisation = organisationRepo.findById(organisationId)
                .orElseThrow(() -> new ItemNotFoundException("Organisation not found"));

        InvoiceDocEntity invoice = invoiceDocRepo.findByIdAndOrganisation(invoiceId, organisation)
                .orElseThrow(() -> new ItemNotFoundException("Invoice not found"));

        // Validate user has access to the project that this invoice belongs to
        validateProjectMemberPermissions(currentUser, invoice.getProject(),
                List.of(TeamMemberRole.ACCOUNTANT, TeamMemberRole.OWNER, TeamMemberRole.PROJECT_MANAGER, TeamMemberRole.MEMBER));

        return mapToInvoiceResponse(invoice, currentUser, null);
    }

    @Override
    @Transactional
    public InvoiceDocResponse updateInvoiceWithAttachments(UUID organisationId, UUID invoiceId, UpdateInvoiceDocRequest request, List<MultipartFile> attachments) throws ItemNotFoundException, AccessDeniedException {

        AccountEntity currentUser = getAuthenticatedAccount();
        OrganisationEntity organisation = organisationRepo.findById(organisationId)
                .orElseThrow(() -> new ItemNotFoundException("Organisation not found"));

        InvoiceDocEntity invoice = invoiceDocRepo.findByIdAndOrganisation(invoiceId, organisation)
                .orElseThrow(() -> new ItemNotFoundException("Invoice not found"));

        // Validate user has access to the project
        validateProjectMemberPermissions(currentUser, invoice.getProject(),
                List.of(TeamMemberRole.ACCOUNTANT, TeamMemberRole.OWNER, TeamMemberRole.PROJECT_MANAGER));

        // Update basic fields (keep existing if null)
        if (request.getDateOfIssue() != null) {
            invoice.setDateOfIssue(request.getDateOfIssue());
        }

        if (request.getDueDate() != null) {
            invoice.setDueDate(request.getDueDate());
        }

        if (request.getReference() != null) {
            invoice.setReference(request.getReference());
        }

        // Update line items if provided
        if (request.getLineItems() != null && !request.getLineItems().isEmpty()) {
            // Remove orphaned line items properly
            invoice.getLineItems().forEach(lineItem -> lineItem.setInvoice(null));
            invoice.getLineItems().clear();

            // Create new line items
            List<InvoiceLineItemEntity> newLineItems = createLineItems(request.getLineItems(), invoice);

            // Add new line items to invoice
            newLineItems.forEach(lineItem -> invoice.getLineItems().add(lineItem));
        }

        // Recalculate totals (always recalculate to ensure accuracy)
        List<UUID> taxesToApply = request.getTaxesToApply() != null ?
                request.getTaxesToApply() :
                invoice.getTaxDetails().stream()
                        .map(InvoiceTaxDetail::getOriginalTaxId)
                        .collect(Collectors.toList());

        BigDecimal creditApplied = request.getCreditApplied() != null ?
                request.getCreditApplied() :
                BigDecimal.ZERO;

        calculateInvoiceTotals(invoice, taxesToApply, creditApplied);

        // Update audit fields
        invoice.setUpdatedBy(currentUser.getId());

        // Handle attachments if provided
        if (attachments != null && !attachments.isEmpty()) {
            // TODO: Update attachments
            log.info("Updating {} attachments for invoice: {}", attachments.size(), invoice.getInvoiceNumber());
        }

        // Save an updated invoice
        InvoiceDocEntity savedInvoice = invoiceDocRepo.save(invoice);

        return mapToInvoiceResponse(savedInvoice, currentUser, creditApplied);
    }

    private List<InvoiceLineItemEntity> createLineItems(List<InvoiceLineItemRequest> lineItemRequests, InvoiceDocEntity invoice) {
        return lineItemRequests.stream()
                .map(request -> {
                    InvoiceLineItemEntity lineItem = InvoiceLineItemEntity.builder()
                            .invoice(invoice)
                            .description(request.getDescription())
                            .unitPrice(request.getRate())
                            .quantity(request.getQuantity())
                            .unitOfMeasure(request.getUnitOfMeasure())
                            .taxable(true)
                            .build();

                    // Use precise money calculation method
                    lineItem.calculateLineTotalWithMoney();
                    return lineItem;
                })
                .collect(Collectors.toList());
    }

    private void calculateInvoiceTotals(InvoiceDocEntity invoice, List<UUID> taxIds, BigDecimal creditApplied) throws ItemNotFoundException {
        // Calculate subtotal using BigDecimal then convert to Money for precision
        BigDecimal subtotalBD = invoice.getLineItems().stream()
                .filter(InvoiceLineItemEntity::getTaxable)
                .map(InvoiceLineItemEntity::getLineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        MonetaryAmount subtotalMoney = Money.of(subtotalBD, "TZS");
        invoice.setSubtotalMoney(subtotalMoney);

        // Calculate taxes if any are specified - now with organization validation
        MonetaryAmount totalTaxMoney = Money.of(BigDecimal.ZERO, "TZS");
        List<InvoiceTaxDetail> taxDetails = new ArrayList<>();

        if (taxIds != null && !taxIds.isEmpty()) {
            TaxCalculationResult taxResult = calculateTaxesWithMoney(taxIds, subtotalMoney, invoice.getOrganisation());
            taxDetails = taxResult.getTaxDetails();
            totalTaxMoney = taxResult.getTotalTaxMoney();
        }

        invoice.setTaxDetails(taxDetails);
        invoice.setTotalTaxAmount(totalTaxMoney.getNumber().numberValue(BigDecimal.class));

        // Calculate total amount using Money API
        MonetaryAmount totalMoney = subtotalMoney.add(totalTaxMoney);
        invoice.setTotalAmountMoney(totalMoney);

        // Calculate the amount due (total - credit applied)
        MonetaryAmount creditMoney = Money.of(creditApplied != null ? creditApplied : BigDecimal.ZERO, "TZS");
        MonetaryAmount amountDueMoney = totalMoney.subtract(creditMoney);
        invoice.setAmountDueMoney(amountDueMoney);
    }

    private TaxCalculationResult calculateTaxesWithMoney(List<UUID> taxIds, MonetaryAmount subtotalMoney, OrganisationEntity organisation) throws ItemNotFoundException {
        // Silently remove duplicates by converting to Set and back to List
        List<UUID> uniqueTaxIds = taxIds != null ?
                taxIds.stream().distinct().collect(Collectors.toList()) :
                new ArrayList<>();

        // Validate that all requested taxes exist and belong to the organization
        List<TaxEntity> taxes = validateAndGetTaxes(uniqueTaxIds, organisation);

        List<InvoiceTaxDetail> taxDetails = new ArrayList<>();
        MonetaryAmount totalTaxMoney = Money.of(BigDecimal.ZERO, "TZS");

        for (TaxEntity tax : taxes) {
            // Validate tax is active
            if (!tax.getIsActive()) {
                throw new IllegalArgumentException("Tax '" + tax.getTaxName() + "' is not active and cannot be applied");
            }

            // For simple cases, the entire subtotal is taxable
            MonetaryAmount taxableAmountMoney = subtotalMoney;
            MonetaryAmount taxAmountMoney = calculateTaxAmountWithMoney(taxableAmountMoney, tax.getTaxPercent());

            InvoiceTaxDetail taxDetail = InvoiceTaxDetail.builder()
                    .originalTaxId(tax.getTaxId())
                    .taxName(tax.getTaxName())
                    .taxPercent(tax.getTaxPercent())
                    .taxDescription(tax.getTaxDescription())
                    .taxableAmount(taxableAmountMoney.getNumber().numberValue(BigDecimal.class))
                    .taxAmount(taxAmountMoney.getNumber().numberValue(BigDecimal.class))
                    .build();

            taxDetails.add(taxDetail);
            totalTaxMoney = totalTaxMoney.add(taxAmountMoney);
        }

        return new TaxCalculationResult(taxDetails, totalTaxMoney);
    }

    private List<TaxEntity> validateAndGetTaxes(List<UUID> taxIds, OrganisationEntity organisation) throws ItemNotFoundException {
        if (taxIds == null || taxIds.isEmpty()) {
            return new ArrayList<>();
        }

        // Get taxes by IDs and organization
        List<TaxEntity> taxes = taxRepo.findByTaxIdInAndOrganisation(taxIds, organisation);

        // Check if all requested taxes were found
        if (taxes.size() != taxIds.size()) {
            Set<UUID> foundTaxIds = taxes.stream()
                    .map(TaxEntity::getTaxId)
                    .collect(Collectors.toSet());

            List<UUID> missingTaxIds = taxIds.stream()
                    .filter(id -> !foundTaxIds.contains(id))
                    .toList();

            throw new ItemNotFoundException("Tax(es) not found or do not belong to organization: " + missingTaxIds);
        }

        // Check for duplicate tax IDs in the request
        Set<UUID> uniqueTaxIds = new HashSet<>(taxIds);
        if (uniqueTaxIds.size() != taxIds.size()) {
            throw new IllegalArgumentException("Duplicate tax IDs found in request");
        }

        return taxes;
    }

    private MonetaryAmount calculateTaxAmountWithMoney(MonetaryAmount taxableAmount, BigDecimal taxPercent) {
        // Convert percentage to decimal with proper precision
        BigDecimal taxRate = taxPercent.divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP);
        return taxableAmount.multiply(taxRate);
    }

    private void validateProject(ProjectEntity project, OrganisationEntity organisation) throws ItemNotFoundException {
        if (!project.getOrganisation().getOrganisationId().equals(organisation.getOrganisationId())) {
            throw new ItemNotFoundException("Project does not belong to this organisation");
        }
    }

    private void validateProjectMemberPermissions(AccountEntity account, ProjectEntity project, List<TeamMemberRole> allowedRoles) throws ItemNotFoundException, AccessDeniedException {

        OrganisationMember organisationMember = organisationMemberRepo.findByAccountAndOrganisation(account, project.getOrganisation())
                .orElseThrow(() -> new ItemNotFoundException("Member is not found in organisation"));

        if (organisationMember.getStatus() != MemberStatus.ACTIVE) {
            throw new ItemNotFoundException("Member is not active");
        }

        ProjectTeamMemberEntity projectTeamMember = projectTeamMemberRepo.findByOrganisationMemberAndProject(organisationMember, project)
                .orElseThrow(() -> new ItemNotFoundException("Project team member not found"));

        if (!allowedRoles.contains(projectTeamMember.getRole())) {
            throw new AccessDeniedException("Member has insufficient permissions for this operation");
        }

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

    private InvoiceDocResponse mapToInvoiceResponse(InvoiceDocEntity invoice, AccountEntity currentUser, BigDecimal creditApplied) {
        InvoiceDocResponse response = new InvoiceDocResponse();
        response.setId(invoice.getId());
        response.setInvoiceNumber(invoice.getInvoiceNumber());
        response.setProjectId(invoice.getProject().getProjectId());
        response.setProjectName(invoice.getProject().getName());
        response.setClientId(invoice.getClient().getClientId());
        response.setClientName(invoice.getClient().getName());
        response.setInvoiceStatus(invoice.getInvoiceStatus());
        response.setDateOfIssue(invoice.getDateOfIssue());
        response.setDueDate(invoice.getDueDate());
        response.setReference(invoice.getReference());
        response.setOrganisationId(invoice.getOrganisation().getOrganisationId());
        response.setOrganisationName(invoice.getOrganisation().getOrganisationName());
        response.setSubtotal(invoice.getSubtotal());
        response.setTaxAmount(invoice.getTotalTaxAmount());
        response.setTotalAmount(invoice.getTotalAmount());
        response.setAmountPaid(invoice.getAmountPaid());
        response.setCreditApplied(creditApplied != null ? creditApplied : BigDecimal.ZERO);
        response.setAmountDue(invoice.getAmountDue());
        response.setCreatedAt(invoice.getCreatedAt());
        response.setUpdatedAt(invoice.getUpdatedAt());
        response.setCreatedByUserName(currentUser.getUserName());

        // Map line items sorted by order
        List<InvoiceLineItemResponse> lineItemResponses = invoice.getLineItems().stream()
                .map(this::mapToLineItemResponse)
                .collect(Collectors.toList());
        response.setLineItems(lineItemResponses);

        // Map tax details for transparency
        List<InvoiceTaxDetailResponse> taxDetailResponses = invoice.getTaxDetails().stream()
                .map(this::mapToTaxDetailResponse)
                .collect(Collectors.toList());
        response.setTaxDetails(taxDetailResponses);

        return response;
    }

    private InvoiceLineItemResponse mapToLineItemResponse(InvoiceLineItemEntity lineItem) {
        InvoiceLineItemResponse response = new InvoiceLineItemResponse();
        response.setId(lineItem.getId());
        response.setDescription(lineItem.getDescription());
        response.setRate(lineItem.getUnitPrice());
        response.setQuantity(lineItem.getQuantity());
        response.setLineTotal(lineItem.getLineTotal());
        response.setUnitOfMeasure(lineItem.getUnitOfMeasure());
        return response;
    }

    private InvoiceTaxDetailResponse mapToTaxDetailResponse(InvoiceTaxDetail taxDetail) {
        InvoiceTaxDetailResponse response = new InvoiceTaxDetailResponse();
        response.setOriginalTaxId(taxDetail.getOriginalTaxId());
        response.setTaxName(taxDetail.getTaxName());
        response.setTaxPercent(taxDetail.getTaxPercent());
        response.setTaxDescription(taxDetail.getTaxDescription());
        response.setTaxableAmount(taxDetail.getTaxableAmount());
        response.setTaxAmount(taxDetail.getTaxAmount());
        return response;
    }

    // Helper class for tax calculation results with Money API
    @Data
    @AllArgsConstructor
    private static class TaxCalculationResult {
        private List<InvoiceTaxDetail> taxDetails;
        private MonetaryAmount totalTaxMoney;

        public BigDecimal getTotalTaxAmount() {
            return totalTaxMoney.getNumber().numberValue(BigDecimal.class);
        }
    }

    private SummaryInvoiceDocResponse mapToSummaryResponse(InvoiceDocEntity invoice) {
        SummaryInvoiceDocResponse response = new SummaryInvoiceDocResponse();
        response.setInvoiceId(invoice.getId());
        response.setInvoiceNumber(invoice.getInvoiceNumber());
        response.setStatus(invoice.getInvoiceStatus());
        response.setTotalAmount(invoice.getTotalAmount());
        response.setProjectName(invoice.getProject().getName());
        response.setClientName(invoice.getClient().getName());
        response.setLineItemCount(invoice.getLineItems().size());
        response.setDateOfIssue(invoice.getDateOfIssue());
        response.setDueDate(invoice.getDueDate());
        return response;
    }

    private void validateOrganisationMemberAccess(AccountEntity account, OrganisationEntity organisation) throws ItemNotFoundException, AccessDeniedException {
        OrganisationMember member = organisationMemberRepo.findByAccountAndOrganisation(account, organisation)
                .orElseThrow(() -> new ItemNotFoundException("Member not found in organisation"));

        if (member.getStatus() != MemberStatus.ACTIVE) {
            throw new AccessDeniedException("Member is not active");
        }

        if (member.getRole() != MemberRole.OWNER && member.getRole() != MemberRole.ADMIN) {
            throw new AccessDeniedException("Insufficient permissions for this operation");
        }

    }

}