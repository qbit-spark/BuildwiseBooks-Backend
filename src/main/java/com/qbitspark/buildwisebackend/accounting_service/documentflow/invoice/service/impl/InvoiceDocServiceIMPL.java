package com.qbitspark.buildwisebackend.accounting_service.documentflow.invoice.service.impl;

import com.qbitspark.buildwisebackend.accounting_service.documentflow.invoice.entity.InvoiceDocEntity;
import com.qbitspark.buildwisebackend.accounting_service.documentflow.invoice.entity.InvoiceLineItemEntity;
import com.qbitspark.buildwisebackend.accounting_service.documentflow.invoice.entity.embedings.InvoiceTaxDetail;
import com.qbitspark.buildwisebackend.accounting_service.documentflow.invoice.enums.InvoiceStatus;
import com.qbitspark.buildwisebackend.accounting_service.documentflow.invoice.paylaod.*;
import com.qbitspark.buildwisebackend.accounting_service.documentflow.invoice.repo.InvoiceDocRepo;
import com.qbitspark.buildwisebackend.accounting_service.documentflow.invoice.service.InvoiceDocService;
import com.qbitspark.buildwisebackend.accounting_service.documentflow.invoice.service.InvoiceNumberService;
import com.qbitspark.buildwisebackend.accounting_service.receipt_mng.entity.ReceiptEntity;
import com.qbitspark.buildwisebackend.accounting_service.receipt_mng.enums.ReceiptStatus;
import com.qbitspark.buildwisebackend.accounting_service.receipt_mng.repo.ReceiptRepo;
import com.qbitspark.buildwisebackend.accounting_service.tax_mng.entity.TaxEntity;
import com.qbitspark.buildwisebackend.accounting_service.tax_mng.repo.TaxRepo;
import com.qbitspark.buildwisebackend.authentication_service.Repository.AccountRepo;
import com.qbitspark.buildwisebackend.authentication_service.entity.AccountEntity;
import com.qbitspark.buildwisebackend.clientsmng_service.entity.ClientEntity;
import com.qbitspark.buildwisebackend.drive_mng.entity.OrgFileEntity;
import com.qbitspark.buildwisebackend.drive_mng.repo.OrgFileRepo;
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
import com.qbitspark.buildwisebackend.projectmng_service.repo.ProjectTeamMemberRepo;
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
    private final OrgFileRepo orgFileRepo;
    private final ReceiptRepo receiptRepo;
    private final PermissionCheckerService permissionChecker;

    @Transactional
    public InvoiceDocResponse createInvoice(UUID organisationId, CreateInvoiceDocRequest request) throws ItemNotFoundException, AccessDeniedException {

        AccountEntity currentUser = getAuthenticatedAccount();

        OrganisationEntity organisation = organisationRepo.findById(organisationId).orElseThrow(() -> new ItemNotFoundException("Organisation not found"));

        ProjectEntity project = projectRepo.findById(request.getProjectId())
                .orElseThrow(() -> new ItemNotFoundException("Project not found"));

        OrganisationMember member = validateProjectAndOrganisationAccess(currentUser, project, organisation);

        permissionChecker.checkMemberPermission(member, "INVOICES","createInvoice");

        ClientEntity client = project.getClient();

        String invoiceNumber = invoiceNumberService.generateInvoiceNumber(project, client, organisation);

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
                .attachments(request.getAttachments())
                .build();

        List<InvoiceLineItemEntity> lineItems = createLineItems(request.getLineItems(), invoice);
        invoice.setLineItems(lineItems);

        calculateInvoiceTotals(invoice, request.getTaxesToApply(), request.getCreditApplied());

        InvoiceDocEntity savedInvoice = invoiceDocRepo.save(invoice);

        return mapToInvoiceResponse(savedInvoice, currentUser, request.getCreditApplied());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<SummaryInvoiceDocResponse> getAllInvoicesForOrganisation(UUID organisationId, int page, int size) throws ItemNotFoundException, AccessDeniedException {

        AccountEntity currentUser = getAuthenticatedAccount();

        OrganisationEntity organisation = organisationRepo.findById(organisationId).orElseThrow(() -> new ItemNotFoundException("Organisation not found"));

        OrganisationMember member = validateOrganisationMemberAccess(currentUser, organisation);

        permissionChecker.checkMemberPermission(member, "INVOICES","viewInvoices");


        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<InvoiceDocEntity> invoicePage = invoiceDocRepo.findAllByOrganisation(organisation, pageable);

        return invoicePage.map(this::mapToSummaryResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<SummaryInvoiceDocResponse> getAllInvoicesForProject(UUID organisationId, UUID projectId, int page, int size) throws ItemNotFoundException, AccessDeniedException {

        AccountEntity currentUser = getAuthenticatedAccount();

        OrganisationEntity organisation = organisationRepo.findById(organisationId).orElseThrow(() -> new ItemNotFoundException("Organisation not found"));

        ProjectEntity project = projectRepo.findById(projectId).orElseThrow(() -> new ItemNotFoundException("Project not found"));

        OrganisationMember member = validateProjectAndOrganisationAccess(currentUser, project, organisation);

        permissionChecker.checkMemberPermission(member, "INVOICES","viewInvoices");

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<InvoiceDocEntity> invoicePage = invoiceDocRepo.findAllByProject(project, pageable);

        return invoicePage.map(this::mapToSummaryResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public InvoiceDocResponse getInvoiceById(UUID organisationId, UUID invoiceId) throws ItemNotFoundException, AccessDeniedException {

        AccountEntity currentUser = getAuthenticatedAccount();

        OrganisationEntity organisation = organisationRepo.findById(organisationId).orElseThrow(() -> new ItemNotFoundException("Organisation not found"));

        OrganisationMember member = validateOrganisationMemberAccess(currentUser,organisation);

        permissionChecker.checkMemberPermission(member, "INVOICES","viewInvoices");

        InvoiceDocEntity invoice = invoiceDocRepo.findByIdAndOrganisation(invoiceId, organisation)
                .orElseThrow(() -> new ItemNotFoundException("Invoice not found"));


        return mapToInvoiceResponse(invoice, currentUser, null);
    }

    @Override
    @Transactional
    public InvoiceDocResponse updateInvoice(UUID organisationId, UUID invoiceId, UpdateInvoiceDocRequest request) throws ItemNotFoundException, AccessDeniedException {

        AccountEntity currentUser = getAuthenticatedAccount();

        OrganisationEntity organisation = organisationRepo.findById(organisationId).orElseThrow(() -> new ItemNotFoundException("Organisation not found"));

        InvoiceDocEntity invoice = invoiceDocRepo.findByIdAndOrganisation(invoiceId, organisation)
                .orElseThrow(() -> new ItemNotFoundException("Invoice not found"));

        OrganisationMember member = validateProjectAndOrganisationAccess(currentUser, invoice.getProject(), organisation);

        permissionChecker.checkMemberPermission(member, "INVOICES","updateInvoice");

        if (request.getDateOfIssue() != null) {
            invoice.setDateOfIssue(request.getDateOfIssue());
        }

        if (request.getDueDate() != null) {
            invoice.setDueDate(request.getDueDate());
        }

        if (request.getReference() != null) {
            invoice.setReference(request.getReference());
        }

        if (request.getAttachments() != null) {
            updateInvoiceAttachments(invoice, request.getAttachments(), organisation);
        }

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


                    lineItem.calculateLineTotalWithMoney();
                    return lineItem;
                })
                .collect(Collectors.toList());
    }

    private void calculateInvoiceTotals(InvoiceDocEntity invoice, List<UUID> taxIds, BigDecimal creditApplied) throws ItemNotFoundException {
        BigDecimal subtotalBD = invoice.getLineItems().stream()
                .filter(InvoiceLineItemEntity::getTaxable)
                .map(InvoiceLineItemEntity::getLineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        MonetaryAmount subtotalMoney = Money.of(subtotalBD, "TZS");
        invoice.setSubtotalMoney(subtotalMoney);

        MonetaryAmount totalTaxMoney = Money.of(BigDecimal.ZERO, "TZS");
        List<InvoiceTaxDetail> taxDetails = new ArrayList<>();

        if (taxIds != null && !taxIds.isEmpty()) {
            TaxCalculationResult taxResult = calculateTaxesWithMoney(taxIds, subtotalMoney, invoice.getOrganisation());
            taxDetails = taxResult.getTaxDetails();
            totalTaxMoney = taxResult.getTotalTaxMoney();
        }

        invoice.setTaxDetails(taxDetails);
        invoice.setTotalTaxAmount(totalTaxMoney.getNumber().numberValue(BigDecimal.class));

        MonetaryAmount totalMoney = subtotalMoney.add(totalTaxMoney);
        invoice.setTotalAmountMoney(totalMoney);

        MonetaryAmount creditMoney = Money.of(creditApplied != null ? creditApplied : BigDecimal.ZERO, "TZS");
        MonetaryAmount amountDueMoney = totalMoney.subtract(creditMoney);
    }

    private TaxCalculationResult calculateTaxesWithMoney(List<UUID> taxIds, MonetaryAmount subtotalMoney, OrganisationEntity organisation) throws ItemNotFoundException {
        List<UUID> uniqueTaxIds = taxIds != null ?
                taxIds.stream().distinct().collect(Collectors.toList()) :
                new ArrayList<>();

        List<TaxEntity> taxes = validateAndGetTaxes(uniqueTaxIds, organisation);

        List<InvoiceTaxDetail> taxDetails = new ArrayList<>();
        MonetaryAmount totalTaxMoney = Money.of(BigDecimal.ZERO, "TZS");

        for (TaxEntity tax : taxes) {
            if (!tax.getIsActive()) {
                throw new IllegalArgumentException("Tax '" + tax.getTaxName() + "' is not active and cannot be applied");
            }

            MonetaryAmount taxAmountMoney = calculateTaxAmountWithMoney(subtotalMoney, tax.getTaxPercent());

            InvoiceTaxDetail taxDetail = InvoiceTaxDetail.builder()
                    .originalTaxId(tax.getTaxId())
                    .taxName(tax.getTaxName())
                    .taxPercent(tax.getTaxPercent())
                    .taxDescription(tax.getTaxDescription())
                    .taxableAmount(subtotalMoney.getNumber().numberValue(BigDecimal.class))
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

        // Calculate payment amounts
        BigDecimal paidAmount = calculateAmountPaid(invoice.getId());
        BigDecimal amountDue = calculateAmountDue(invoice);

        response.setPaidAmount(paidAmount);
        response.setCreditApplied(creditApplied != null ? creditApplied : BigDecimal.ZERO);
        response.setAmountDue(amountDue);


        response.setFullyPaid(isFullyPaid(invoice));
        response.setOverdue(invoice.isOverdue());
        response.setCanReceivePayment(invoice.getInvoiceStatus().canReceivePayment());
        response.setCalculatedStatus(calculateInvoiceStatus(invoice));
        response.setRemainingAmount(amountDue);
        response.setPaymentStatusDescription(buildPaymentStatusDescription(invoice, paidAmount, amountDue));

        response.setCreatedAt(invoice.getCreatedAt());
        response.setUpdatedAt(invoice.getUpdatedAt());
        response.setCreatedByUserName(currentUser.getUserName());

        // Map line items
        List<InvoiceLineItemResponse> lineItemResponses = invoice.getLineItems().stream()
                .map(this::mapToLineItemResponse)
                .collect(Collectors.toList());
        response.setLineItems(lineItemResponses);

        // Map tax details
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


    private SummaryInvoiceDocResponse mapToSummaryResponse(InvoiceDocEntity invoice) {
        SummaryInvoiceDocResponse response = new SummaryInvoiceDocResponse();
        response.setInvoiceId(invoice.getId());
        response.setInvoiceNumber(invoice.getInvoiceNumber());
        response.setPaidAmount(calculatePaidAmount(invoice.getId()));
        response.setStatus(invoice.getInvoiceStatus());
        response.setTotalAmount(invoice.getTotalAmount());
        response.setProjectName(invoice.getProject().getName());
        response.setClientName(invoice.getClient().getName());
        response.setLineItemCount(invoice.getLineItems().size());
        response.setDateOfIssue(invoice.getDateOfIssue());
        response.setDueDate(invoice.getDueDate());
        return response;
    }

    private void updateInvoiceAttachments(InvoiceDocEntity invoiceDoc, List<UUID> newAttachmentIds, OrganisationEntity organisation)
            throws ItemNotFoundException, AccessDeniedException {

        invoiceDoc.getAttachments().clear();
        invoiceDocRepo.save(invoiceDoc);
        invoiceDocRepo.flush();


        if (!newAttachmentIds.isEmpty()) {
            validateVoucherAttachments(newAttachmentIds, organisation);

            invoiceDoc.setAttachments(new ArrayList<>(newAttachmentIds));
            invoiceDocRepo.save(invoiceDoc);


        }
    }
    private void validateVoucherAttachments(List<UUID> attachmentIds, OrganisationEntity organisation)
            throws ItemNotFoundException, AccessDeniedException {

        for (UUID fileId : attachmentIds) {
            OrgFileEntity file = orgFileRepo.findById(fileId)
                    .orElseThrow(() -> new ItemNotFoundException("Attachment file not found: " + fileId));

            if (!file.getOrganisation().getOrganisationId().equals(organisation.getOrganisationId())) {
                throw new AccessDeniedException("File does not belong to this organisation: " + fileId);
            }

            if (Boolean.TRUE.equals(file.getIsDeleted())) {
                throw new ItemNotFoundException("Cannot attach deleted file: " + fileId);
            }
        }
    }


    private BigDecimal calculatePaidAmount(UUID invoiceId) {
        List<ReceiptEntity> approvedReceipts = receiptRepo.findByInvoiceIdAndStatus(invoiceId, ReceiptStatus.APPROVED);
        return approvedReceipts.stream()
                .map(ReceiptEntity::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }


    private OrganisationMember validateProjectAndOrganisationAccess(AccountEntity account, ProjectEntity project, OrganisationEntity organisation) throws ItemNotFoundException {
        if (!project.getOrganisation().getOrganisationId().equals(organisation.getOrganisationId())) {
            throw new ItemNotFoundException("Project does not belong to this organisation");
        }

        OrganisationMember organisationMember = organisationMemberRepo.findByAccountAndOrganisation(account, project.getOrganisation())
                .orElseThrow(() -> new ItemNotFoundException("Member is not belong to this organisation"));

        if (organisationMember.getStatus() != MemberStatus.ACTIVE) {
            throw new ItemNotFoundException("Member is not active");
        }

        projectTeamMemberRepo.findByOrganisationMemberAndProject(organisationMember, project)
                .orElseThrow(() -> new ItemNotFoundException("Project team member not found"));

        return organisationMember;
    }

    private OrganisationMember validateOrganisationMemberAccess(AccountEntity account, OrganisationEntity organisation) throws ItemNotFoundException {
        OrganisationMember member = organisationMemberRepo.findByAccountAndOrganisation(account, organisation)
                .orElseThrow(() -> new ItemNotFoundException("Member is not belong to this organisation"));

        if (member.getStatus() != MemberStatus.ACTIVE) {
            throw new ItemNotFoundException("Member is not active");
        }

        return member;
    }

    public BigDecimal calculateAmountPaid(UUID invoiceId) {
        return receiptRepo.findByInvoiceIdAndStatus(invoiceId, ReceiptStatus.APPROVED)
                .stream()
                .map(ReceiptEntity::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal calculateAmountDue(InvoiceDocEntity invoice) {
        BigDecimal amountPaid = calculateAmountPaid(invoice.getId());
        return invoice.getTotalAmount().subtract(amountPaid);
    }

    public boolean isFullyPaid(InvoiceDocEntity invoice) {
        return calculateAmountDue(invoice).compareTo(BigDecimal.ZERO) <= 0;
    }

    public InvoiceStatus calculateInvoiceStatus(InvoiceDocEntity invoice) {
        // Don't auto-calculate status if it's in workflow states
        if (invoice.getInvoiceStatus() == InvoiceStatus.DRAFT ||
                invoice.getInvoiceStatus() == InvoiceStatus.PENDING_APPROVAL ||
                invoice.getInvoiceStatus() == InvoiceStatus.REJECTED ||
                invoice.getInvoiceStatus() == InvoiceStatus.CANCELLED) {
            return invoice.getInvoiceStatus(); // Keep current status
        }

        BigDecimal paid = calculateAmountPaid(invoice.getId());
        BigDecimal total = invoice.getTotalAmount();

        if (paid.compareTo(BigDecimal.ZERO) == 0) {
            return InvoiceStatus.APPROVED; // No payments yet
        } else if (paid.compareTo(total) >= 0) {
            return InvoiceStatus.PAID; // Fully paid
        } else {
            return InvoiceStatus.PARTIALLY_PAID; // Partial payment
        }
    }

    private String buildPaymentStatusDescription(InvoiceDocEntity invoice, BigDecimal paidAmount, BigDecimal amountDue) {
        if (isFullyPaid(invoice)) {
            return "Invoice is fully paid";
        } else if (paidAmount.compareTo(BigDecimal.ZERO) == 0) {
            return "No payments received";
        } else {
            return String.format("Partially paid - %s remaining", amountDue);
        }
    }

}