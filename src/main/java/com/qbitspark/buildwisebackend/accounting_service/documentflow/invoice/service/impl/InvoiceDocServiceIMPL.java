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
import org.javamoney.moneta.Money;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.money.MonetaryAmount;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
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

        // TODO: Handle attachments later

        // Convert to response
        return mapToInvoiceResponse(savedInvoice, currentUser, request.getCreditApplied());
    }
    private ProjectTeamMemberEntity validateProjectMemberPermissions(AccountEntity account, ProjectEntity project, List<TeamMemberRole> allowedRoles) throws ItemNotFoundException, AccessDeniedException {

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

        return projectTeamMember;
    }

    private void validateProject(ProjectEntity project, OrganisationEntity organisation) throws ItemNotFoundException {
        if (!project.getOrganisation().getOrganisationId().equals(organisation.getOrganisationId())) {
            throw new ItemNotFoundException("Project does not belong to this organisation");
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

    private List<InvoiceLineItemEntity> createLineItems(List<InvoiceLineItemRequest> lineItemRequests, InvoiceDocEntity invoice) {
        return lineItemRequests.stream()
                .map(request -> {
                    InvoiceLineItemEntity lineItem = InvoiceLineItemEntity.builder()
                            .invoice(invoice)
                            .description(request.getDescription())
                            .unitPrice(request.getRate())
                            .quantity(request.getQuantity())
                            .unitOfMeasure(request.getUnitOfMeasure())
                            .lineOrder(request.getLineOrder())
                            .taxable(true)
                            .build();

                    // Use your precise money calculation method
                    lineItem.calculateLineTotalWithMoney();
                    return lineItem;
                })
                .collect(Collectors.toList());
    }

    private void calculateInvoiceTotals(InvoiceDocEntity invoice, List<UUID> taxIds, BigDecimal creditApplied) {
        // Calculate subtotal using BigDecimal then convert to Money for precision
        BigDecimal subtotalBD = invoice.getLineItems().stream()
                .filter(InvoiceLineItemEntity::getTaxable)
                .map(InvoiceLineItemEntity::getLineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        MonetaryAmount subtotalMoney = Money.of(subtotalBD, "TZS");

        invoice.setSubtotalMoney(subtotalMoney);

        // Calculate taxes if any are specified
        MonetaryAmount totalTaxMoney = Money.of(BigDecimal.ZERO, "TZS");
        List<InvoiceTaxDetail> taxDetails = new ArrayList<>();

        if (taxIds != null && !taxIds.isEmpty()) {
            TaxCalculationResult taxResult = calculateTaxesWithMoney(taxIds, subtotalMoney);
            taxDetails = taxResult.getTaxDetails();
            totalTaxMoney = taxResult.getTotalTaxMoney();
        }

        invoice.setTaxDetails(taxDetails);
        invoice.setTotalTaxAmount(totalTaxMoney.getNumber().numberValue(BigDecimal.class));

        // Calculate the total amount using Money API
        MonetaryAmount totalMoney = subtotalMoney.add(totalTaxMoney);
        invoice.setTotalAmountMoney(totalMoney);

        // Calculate the amount due (total - credit applied)
        MonetaryAmount creditMoney = Money.of(creditApplied != null ? creditApplied : BigDecimal.ZERO, "TZS");
        MonetaryAmount amountDueMoney = totalMoney.subtract(creditMoney);
        invoice.setAmountDueMoney(amountDueMoney);
    }

    private TaxCalculationResult calculateTaxesWithMoney(List<UUID> taxIds, MonetaryAmount subtotalMoney) {
        List<TaxEntity> taxes = taxRepo.findAllById(taxIds);
        List<InvoiceTaxDetail> taxDetails = new ArrayList<>();
        MonetaryAmount totalTaxMoney = Money.of(BigDecimal.ZERO, "TZS");

        for (TaxEntity tax : taxes) {
            // For simple cases, entire subtotal is taxable
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

    private MonetaryAmount calculateTaxAmountWithMoney(MonetaryAmount taxableAmount, BigDecimal taxPercent) {
        // Convert percentage to decimal and multiply
        BigDecimal taxRate = taxPercent.divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP);
        return taxableAmount.multiply(taxRate);
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
        response.setCreatedAt(invoice.getCreatedAt()    );
        response.setUpdatedAt(invoice.getUpdatedAt());
        response.setCreatedByUserName(currentUser.getUserName()); // Assuming you have this method

        // Map line items
        List<InvoiceLineItemResponse> lineItemResponses = invoice.getLineItems().stream()
                .sorted((a, b) -> Integer.compare(a.getLineOrder(), b.getLineOrder())) // Sort by line order
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
        response.setRate(lineItem.getUnitPrice()); // Map unitPrice to rate in response
        response.setQuantity(lineItem.getQuantity());
        response.setLineTotal(lineItem.getLineTotal());
        response.setUnitOfMeasure(lineItem.getUnitOfMeasure());
        response.setLineOrder(lineItem.getLineOrder());
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

    // Enhanced helper class for tax calculation results with Money API
    @Data
    @AllArgsConstructor
    private static class TaxCalculationResult {
        private List<InvoiceTaxDetail> taxDetails;
        private MonetaryAmount totalTaxMoney; // Using MonetaryAmount for precision

        public BigDecimal getTotalTaxAmount() {
            return totalTaxMoney.getNumber().numberValue(BigDecimal.class);
        }
    }
}