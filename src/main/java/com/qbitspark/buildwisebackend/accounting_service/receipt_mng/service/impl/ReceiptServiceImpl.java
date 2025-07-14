package com.qbitspark.buildwisebackend.accounting_service.receipt_mng.service.impl;

import com.qbitspark.buildwisebackend.accounting_service.bank_mng.entity.BankAccountEntity;
import com.qbitspark.buildwisebackend.accounting_service.bank_mng.repo.BankAccountRepo;
import com.qbitspark.buildwisebackend.accounting_service.documentflow.invoice.entity.InvoiceDocEntity;
import com.qbitspark.buildwisebackend.accounting_service.documentflow.invoice.enums.InvoiceStatus;
import com.qbitspark.buildwisebackend.accounting_service.documentflow.invoice.repo.InvoiceDocRepo;
import com.qbitspark.buildwisebackend.accounting_service.receipt_mng.entity.ReceiptEntity;
import com.qbitspark.buildwisebackend.accounting_service.receipt_mng.enums.ReceiptStatus;
import com.qbitspark.buildwisebackend.accounting_service.receipt_mng.payload.CreateReceiptRequest;
import com.qbitspark.buildwisebackend.accounting_service.receipt_mng.payload.UpdateReceiptRequest;
import com.qbitspark.buildwisebackend.accounting_service.receipt_mng.repo.ReceiptRepo;
import com.qbitspark.buildwisebackend.accounting_service.receipt_mng.service.ReceiptService;
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
import com.qbitspark.buildwisebackend.organisation_service.roles_mng.service.PermissionCheckerService;
import com.qbitspark.buildwisebackend.projectmng_service.entity.ProjectEntity;
import com.qbitspark.buildwisebackend.projectmng_service.entity.ProjectTeamMemberEntity;
import com.qbitspark.buildwisebackend.projectmng_service.enums.TeamMemberRole;
import com.qbitspark.buildwisebackend.projectmng_service.repo.ProjectRepo;
import com.qbitspark.buildwisebackend.projectmng_service.repo.ProjectTeamMemberRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class ReceiptServiceImpl implements ReceiptService {

    private final ReceiptRepo receiptRepo;
    private final InvoiceDocRepo invoiceDocRepo;
    private final BankAccountRepo bankAccountRepo;
    private final OrganisationRepo organisationRepo;
    private final ProjectRepo projectRepo;
    private final OrganisationMemberRepo organisationMemberRepo;
    private final ProjectTeamMemberRepo projectTeamMemberRepo;
    private final AccountRepo accountRepo;
    private final ReceiptNumberService receiptNumberService;
    private final PermissionCheckerService permissionChecker;

    @Override
    public ReceiptEntity createReceipt(UUID organisationId, CreateReceiptRequest request)
            throws ItemNotFoundException, AccessDeniedException {

        AccountEntity currentUser = getAuthenticatedAccount();

        OrganisationEntity organisation = organisationRepo.findById(organisationId).orElseThrow(() -> new ItemNotFoundException("Organisation not found"));

        OrganisationMember member = validateOrganisationMemberAccess(currentUser, organisation);

        permissionChecker.checkMemberPermission(member, "RECEIPTS","createReceipt");

        InvoiceDocEntity invoice = validateInvoice(request.getInvoiceId(), organisation);

        validateInvoiceForReceipt(invoice);

        ProjectEntity project = invoice.getProject();
        ClientEntity client = invoice.getClient();

        validateProjectMemberPermissions(currentUser, project);

        BankAccountEntity bankAccount = null;
        if (request.getBankAccountId() != null) {
            bankAccount = validateBankAccount(request.getBankAccountId(), organisation);
        }

        String receiptNumber = receiptNumberService.generateReceiptNumber(project, organisation);

        ReceiptEntity receipt = new ReceiptEntity();
        receipt.setReceiptNumber(receiptNumber);
        receipt.setReceiptDate(request.getReceiptDate());
        receipt.setOrganisation(organisation);
        receipt.setProject(project);
        receipt.setClient(client);
        receipt.setInvoice(invoice);
        receipt.setBankAccount(bankAccount);
        receipt.setTotalAmount(request.getTotalAmount());
        receipt.setPaymentMethod(request.getPaymentMethod());
        receipt.setReference(request.getReference());
        receipt.setDescription(request.getDescription());
        receipt.setAttachments(request.getAttachments());
        receipt.setCreatedBy(currentUser.getAccountId());

        return receiptRepo.save(receipt);
    }

    @Override
    public Page<ReceiptEntity> getOrganisationReceipts(UUID organisationId, Pageable pageable)
            throws ItemNotFoundException, AccessDeniedException {

        AccountEntity currentUser = getAuthenticatedAccount();

        OrganisationEntity organisation = organisationRepo.findById(organisationId).orElseThrow(() -> new ItemNotFoundException("Organisation not found"));

        OrganisationMember member = validateOrganisationMemberAccess(currentUser, organisation);

        permissionChecker.checkMemberPermission(member, "RECEIPTS","viewReceipts");

        return receiptRepo.findByOrganisation(organisation, pageable);
    }

    @Override
    public ReceiptEntity getReceiptById(UUID organisationId, UUID receiptId)
            throws ItemNotFoundException, AccessDeniedException {

        AccountEntity currentUser = getAuthenticatedAccount();

        OrganisationEntity organisation = organisationRepo.findById(organisationId).orElseThrow(() -> new ItemNotFoundException("Organisation not found"));

        OrganisationMember member = validateOrganisationMemberAccess(currentUser, organisation);

        permissionChecker.checkMemberPermission(member, "RECEIPTS","viewReceipts");


        return receiptRepo.findByReceiptIdAndOrganisation(receiptId, organisation)
                .orElseThrow(() -> new ItemNotFoundException("Receipt not found"));
    }

    @Override
    public ReceiptEntity updateReceipt(UUID organisationId, UUID receiptId, UpdateReceiptRequest request)
            throws ItemNotFoundException, AccessDeniedException {

        AccountEntity currentUser = getAuthenticatedAccount();

        OrganisationEntity organisation = organisationRepo.findById(organisationId).orElseThrow(() -> new ItemNotFoundException("Organisation not found"));

        OrganisationMember member = validateOrganisationMemberAccess(currentUser, organisation);

        permissionChecker.checkMemberPermission(member, "RECEIPTS","updateReceipt");

        ReceiptEntity receipt = receiptRepo.findByReceiptIdAndOrganisation(receiptId, organisation)
                .orElseThrow(() -> new ItemNotFoundException("Receipt not found"));

        validateProjectMemberPermissions(currentUser, receipt.getProject());

        if (receipt.getStatus() != ReceiptStatus.DRAFT) {
            throw new ItemNotFoundException("Only draft receipts can be updated");
        }

        if (request.getTotalAmount() != null) {
            receipt.setTotalAmount(request.getTotalAmount());
        }
        if (request.getPaymentMethod() != null) {
            receipt.setPaymentMethod(request.getPaymentMethod());
        }
        if (request.getReceiptDate() != null) {
            receipt.setReceiptDate(request.getReceiptDate());
        }
        if (request.getBankAccountId() != null) {
            BankAccountEntity bankAccount = validateBankAccount(request.getBankAccountId(), organisation);
            receipt.setBankAccount(bankAccount);
        }
        if (request.getReference() != null) {
            receipt.setReference(request.getReference());
        }
        if (request.getDescription() != null) {
            receipt.setDescription(request.getDescription());
        }
        if (request.getAttachments() != null) {
            receipt.setAttachments(request.getAttachments());
        }

        receipt.setUpdatedBy(currentUser.getAccountId());

        return receiptRepo.save(receipt);
    }

    @Override
    public void confirmReceipt(UUID organisationId, UUID receiptId)
            throws ItemNotFoundException, AccessDeniedException {

        AccountEntity currentUser = getAuthenticatedAccount();

        OrganisationEntity organisation = organisationRepo.findById(organisationId).orElseThrow(() -> new ItemNotFoundException("Organisation not found"));

        OrganisationMember member = validateOrganisationMemberAccess(currentUser, organisation);

        permissionChecker.checkMemberPermission(member, "RECEIPTS","updateReceipt");

        ReceiptEntity receipt = receiptRepo.findByReceiptIdAndOrganisation(receiptId, organisation)
                .orElseThrow(() -> new ItemNotFoundException("Receipt not found"));

        validateProjectMemberPermissions(currentUser, receipt.getProject());

        receipt.setStatus(ReceiptStatus.APPROVED);
        receipt.setUpdatedBy(currentUser.getAccountId());
        receiptRepo.save(receipt);
    }

    @Override
    public void cancelReceipt(UUID organisationId, UUID receiptId)
            throws ItemNotFoundException, AccessDeniedException {

        AccountEntity currentUser = getAuthenticatedAccount();

        OrganisationEntity organisation = organisationRepo.findById(organisationId).orElseThrow(() -> new ItemNotFoundException("Organisation not found"));

        OrganisationMember member = validateOrganisationMemberAccess(currentUser, organisation);

        permissionChecker.checkMemberPermission(member, "RECEIPTS","cancelReceipt");

        ReceiptEntity receipt = receiptRepo.findByReceiptIdAndOrganisation(receiptId, organisation)
                .orElseThrow(() -> new ItemNotFoundException("Receipt not found"));

        validateProjectMemberPermissions(currentUser, receipt.getProject());

        receipt.setStatus(ReceiptStatus.CANCELLED);
        receipt.setUpdatedBy(currentUser.getAccountId());

        ReceiptEntity savedReceipt = receiptRepo.save(receipt);
    }

    @Override
    public List<ReceiptEntity> getInvoicePayments(UUID organisationId, UUID invoiceId)
            throws ItemNotFoundException, AccessDeniedException {

        AccountEntity currentUser = getAuthenticatedAccount();

        OrganisationEntity organisation = organisationRepo.findById(organisationId).orElseThrow(() -> new ItemNotFoundException("Organisation not found"));

        OrganisationMember member = validateOrganisationMemberAccess(currentUser, organisation);

        permissionChecker.checkMemberPermission(member, "RECEIPTS","viewReceipts");

        InvoiceDocEntity invoice = validateInvoice(invoiceId, organisation);
        validateProjectMemberPermissions(currentUser, invoice.getProject());

        return receiptRepo.findByInvoiceOrderByReceiptDateDesc(invoice);
    }

    @Override
    public Page<ReceiptEntity> getProjectReceipts(UUID organisationId, UUID projectId, Pageable pageable)
            throws ItemNotFoundException, AccessDeniedException {

        AccountEntity currentUser = getAuthenticatedAccount();

        OrganisationEntity organisation = organisationRepo.findById(organisationId).orElseThrow(() -> new ItemNotFoundException("Organisation not found"));

        OrganisationMember member = validateOrganisationMemberAccess(currentUser, organisation);

        permissionChecker.checkMemberPermission(member, "RECEIPTS","viewReceipts");

        ProjectEntity project = projectRepo.findById(projectId)
                .orElseThrow(() -> new ItemNotFoundException("Project not found"));

        validateProject(project, organisation);
        validateProjectMemberPermissions(currentUser, project);

        return receiptRepo.findByProject(project, pageable);
    }

    @Override
    public List<ReceiptEntity> getOrganisationReceiptsSummary(UUID organisationId)
            throws ItemNotFoundException, AccessDeniedException {

        AccountEntity currentUser = getAuthenticatedAccount();

        OrganisationEntity organisation = organisationRepo.findById(organisationId).orElseThrow(() -> new ItemNotFoundException("Organisation not found"));

        OrganisationMember member = validateOrganisationMemberAccess(currentUser, organisation);

        permissionChecker.checkMemberPermission(member, "RECEIPTS","viewReceipts");

        return receiptRepo.findByOrganisationAndStatus(organisation, ReceiptStatus.APPROVED);
    }


    private InvoiceDocEntity validateInvoice(UUID invoiceId, OrganisationEntity organisation)
            throws ItemNotFoundException {

        return invoiceDocRepo.findByIdAndOrganisation(invoiceId, organisation)
                .orElseThrow(() -> new ItemNotFoundException("Invoice not found"));

    }

    private void validateInvoiceForReceipt(InvoiceDocEntity invoice) throws ItemNotFoundException {
        InvoiceStatus status = invoice.getInvoiceStatus();

        switch (status) {
            case DRAFT:
                throw new ItemNotFoundException(String.format(
                        "Cannot create receipt for draft invoice %s. Please approve the invoice first.",
                        invoice.getInvoiceNumber()
                ));

            case PENDING_APPROVAL:
                throw new ItemNotFoundException(String.format(
                        "Cannot create receipt for invoice %s. Invoice is pending approval.",
                        invoice.getInvoiceNumber()
                ));

            case REJECTED:
                throw new ItemNotFoundException(String.format(
                        "Cannot create receipt for rejected invoice %s.",
                        invoice.getInvoiceNumber()
                ));

            case CANCELLED:
                throw new ItemNotFoundException(String.format(
                        "Cannot create receipt for cancelled invoice %s.",
                        invoice.getInvoiceNumber()
                ));

            case PAID:
                throw new ItemNotFoundException(String.format(
                        "Cannot create receipt for invoice %s. Invoice is already fully paid.",
                        invoice.getInvoiceNumber()
                ));

            case APPROVED:
            case PARTIALLY_PAID:
            case OVERDUE:
                // These are valid - allow receipt creation
                break;

            default:
                throw new ItemNotFoundException(String.format(
                        "Cannot create receipt for invoice %s. Invalid invoice status: %s",
                        invoice.getInvoiceNumber(), status
                ));
        }
    }


    private BankAccountEntity validateBankAccount(UUID bankAccountId, OrganisationEntity organisation)
            throws ItemNotFoundException {

        return bankAccountRepo.findByBankAccountIdAndOrganisation(bankAccountId, organisation)
                .orElseThrow(() -> new ItemNotFoundException("Bank account not found"));
    }

    private void validateProject(ProjectEntity project, OrganisationEntity organisation)
            throws ItemNotFoundException {

        if (!project.getOrganisation().getOrganisationId().equals(organisation.getOrganisationId())) {
            throw new ItemNotFoundException("Project does not belong to this organisation");
        }
    }

    private void validateProjectMemberPermissions(AccountEntity account, ProjectEntity project)
            throws ItemNotFoundException, AccessDeniedException {

        OrganisationMember organisationMember = organisationMemberRepo.findByAccountAndOrganisation(account, project.getOrganisation())
                .orElseThrow(() -> new ItemNotFoundException("Member not found in organisation"));

        if (organisationMember.getStatus() != MemberStatus.ACTIVE) {
            throw new ItemNotFoundException("Member is not active");
        }

        ProjectTeamMemberEntity projectTeamMember = projectTeamMemberRepo.findByOrganisationMemberAndProject(organisationMember, project)
                .orElseThrow(() -> new ItemNotFoundException("Project team member not found"));

        List<TeamMemberRole> allowedRoles = List.of(TeamMemberRole.ACCOUNTANT, TeamMemberRole.OWNER, TeamMemberRole.PROJECT_MANAGER, TeamMemberRole.MEMBER);

        if (!allowedRoles.contains(projectTeamMember.getRole())) {
            throw new AccessDeniedException("Insufficient permissions for this operation");
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

    private OrganisationMember validateOrganisationMemberAccess(AccountEntity account, OrganisationEntity organisation) throws ItemNotFoundException {
        OrganisationMember member = organisationMemberRepo.findByAccountAndOrganisation(account, organisation)
                .orElseThrow(() -> new ItemNotFoundException("Member is not belong to this organisation"));

        if (member.getStatus() != MemberStatus.ACTIVE) {
            throw new ItemNotFoundException("Member is not active");
        }

        return member;
    }

}