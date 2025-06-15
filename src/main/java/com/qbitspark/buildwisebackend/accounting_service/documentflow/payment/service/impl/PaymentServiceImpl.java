package com.qbitspark.buildwisebackend.accounting_service.documentflow.payment.service.impl;

import com.qbitspark.buildwisebackend.accounting_service.coa.entity.JournalEntry;
import com.qbitspark.buildwisebackend.accounting_service.documentflow.payment.entity.PaymentDocEntity;
import com.qbitspark.buildwisebackend.accounting_service.documentflow.payment.entity.PaymentLineItemEntity;
import com.qbitspark.buildwisebackend.accounting_service.documentflow.payment.enums.PaymentStatus;
import com.qbitspark.buildwisebackend.accounting_service.documentflow.payment.enums.PaymentType;
import com.qbitspark.buildwisebackend.accounting_service.documentflow.payment.event.PaymentEvent;
import com.qbitspark.buildwisebackend.accounting_service.documentflow.payment.payload.CreateVoucherPaymentRequest;
import com.qbitspark.buildwisebackend.accounting_service.documentflow.payment.payload.PaymentLineItemResponse;
import com.qbitspark.buildwisebackend.accounting_service.documentflow.payment.payload.PaymentResponse;
import com.qbitspark.buildwisebackend.accounting_service.documentflow.payment.repo.PaymentDocRepo;
import com.qbitspark.buildwisebackend.accounting_service.documentflow.payment.service.PaymentNumberService;
import com.qbitspark.buildwisebackend.accounting_service.documentflow.payment.service.PaymentService;
import com.qbitspark.buildwisebackend.accounting_service.documentflow.voucher.entity.VoucherEntity;
import com.qbitspark.buildwisebackend.accounting_service.documentflow.voucher.entity.VoucherPayeeEntity;
import com.qbitspark.buildwisebackend.accounting_service.documentflow.voucher.enums.VoucherStatus;
import com.qbitspark.buildwisebackend.accounting_service.documentflow.voucher.repo.VoucherRepo;
import com.qbitspark.buildwisebackend.accounting_service.transactions_pipeline.transaction_service.TransactionService;
import com.qbitspark.buildwisebackend.authentication_service.Repository.AccountRepo;
import com.qbitspark.buildwisebackend.authentication_service.entity.AccountEntity;
import com.qbitspark.buildwisebackend.globeadvice.exceptions.AccessDeniedException;
import com.qbitspark.buildwisebackend.globeadvice.exceptions.ItemNotFoundException;
import com.qbitspark.buildwisebackend.organisation_service.organisation_mng.entity.OrganisationEntity;
import com.qbitspark.buildwisebackend.organisation_service.organisation_mng.repo.OrganisationRepo;
import com.qbitspark.buildwisebackend.organisation_service.orgnisation_members_mng.entities.OrganisationMember;
import com.qbitspark.buildwisebackend.organisation_service.orgnisation_members_mng.enums.MemberRole;
import com.qbitspark.buildwisebackend.organisation_service.orgnisation_members_mng.enums.MemberStatus;
import com.qbitspark.buildwisebackend.organisation_service.orgnisation_members_mng.repo.OrganisationMemberRepo;
import com.qbitspark.buildwisebackend.projectmng_service.entity.ProjectEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class PaymentServiceImpl implements PaymentService {

    private final PaymentDocRepo paymentDocRepo;
    private final VoucherRepo voucherRepo;
    private final OrganisationRepo organisationRepo;
    private final AccountRepo accountRepo;
    private final OrganisationMemberRepo organisationMemberRepo;
    private final PaymentNumberService paymentNumberService;
    private final TransactionService transactionService;

    @Override
    public PaymentResponse createVoucherPayment(UUID organisationId, CreateVoucherPaymentRequest request)
            throws ItemNotFoundException, AccessDeniedException {

        AccountEntity currentUser = getAuthenticatedAccount();

        OrganisationEntity organisation = organisationRepo.findById(organisationId)
                .orElseThrow(() -> new ItemNotFoundException("Organisation not found"));

        // Validate user permissions (only admins/owners can create payments)
        OrganisationMember organisationMember = validateOrganisationAccess(currentUser, organisation,
                List.of(MemberRole.OWNER, MemberRole.ADMIN));

        // Validate and get vouchers
        List<VoucherEntity> vouchers = validateAndGetVouchers(request.getVouchers(), organisation);

        // Generate payment number
        String paymentNumber = paymentNumberService.generatePaymentNumber(organisation);

        // Calculate total amount
        BigDecimal totalAmount = vouchers.stream()
                .map(VoucherEntity::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Create payment document
        PaymentDocEntity payment = PaymentDocEntity.builder()
                .paymentNumber(paymentNumber)
                .paymentDate(LocalDate.now())
                .paymentType(PaymentType.VOUCHER)
                .paymentMode(request.getPaymentMode())
                .paymentStatus(PaymentStatus.PENDING)
                .totalAmount(totalAmount)
                .paymentReference(request.getPaymentReference())
                .paymentDescription(request.getPaymentDescription())
                .notes(request.getNotes())
                .createdBy(organisationMember)
                .organisation(organisation)
                .project(getCommonProject(vouchers)) // If all vouchers have same project
                .build();

        // Create payment line items
        List<PaymentLineItemEntity> lineItems = createPaymentLineItems(payment, vouchers, request);
        payment.setLineItems(lineItems);

        PaymentDocEntity savedPayment = paymentDocRepo.save(payment);

        log.info("Payment {} created for {} vouchers with total amount {}",
                savedPayment.getPaymentNumber(), vouchers.size(), totalAmount);

        return mapToPaymentResponse(savedPayment);
    }

    @Override
    public PaymentResponse getPaymentById(UUID organisationId, UUID paymentId)
            throws ItemNotFoundException, AccessDeniedException {

        AccountEntity currentUser = getAuthenticatedAccount();

        OrganisationEntity organisation = organisationRepo.findById(organisationId)
                .orElseThrow(() -> new ItemNotFoundException("Organisation not found"));

        validateOrganisationAccess(currentUser, organisation,
                List.of(MemberRole.OWNER, MemberRole.ADMIN, MemberRole.MEMBER));

        PaymentDocEntity payment = paymentDocRepo.findByIdAndOrganisation(paymentId, organisation)
                .orElseThrow(() -> new ItemNotFoundException("Payment not found"));

        return mapToPaymentResponse(payment);
    }

    @Override
    public List<PaymentResponse> getOrganisationPayments(UUID organisationId)
            throws ItemNotFoundException, AccessDeniedException {

        AccountEntity currentUser = getAuthenticatedAccount();

        OrganisationEntity organisation = organisationRepo.findById(organisationId)
                .orElseThrow(() -> new ItemNotFoundException("Organisation not found"));

        validateOrganisationAccess(currentUser, organisation,
                List.of(MemberRole.OWNER, MemberRole.ADMIN, MemberRole.MEMBER));

        List<PaymentDocEntity> payments = paymentDocRepo.findAllByOrganisationOrderByCreatedAtDesc(organisation);

        return payments.stream()
                .map(this::mapToPaymentResponse)
                .collect(Collectors.toList());
    }

    @Override
    public PaymentResponse processPayment(UUID organisationId, UUID paymentId)
            throws ItemNotFoundException, AccessDeniedException {

        AccountEntity currentUser = getAuthenticatedAccount();

        OrganisationEntity organisation = organisationRepo.findById(organisationId)
                .orElseThrow(() -> new ItemNotFoundException("Organisation not found"));

        // Only admins/owners can process payments
        validateOrganisationAccess(currentUser, organisation,
                List.of(MemberRole.OWNER, MemberRole.ADMIN));

        PaymentDocEntity payment = paymentDocRepo.findByIdAndOrganisation(paymentId, organisation)
                .orElseThrow(() -> new ItemNotFoundException("Payment not found"));

        // Business rule: Can only process pending payments
        if (payment.getPaymentStatus() != PaymentStatus.PENDING) {
            throw new IllegalStateException("Can only process pending payments. Current status: " + payment.getPaymentStatus());
        }

        try {
            // Create accounting entry
            JournalEntry journalEntry = createPaymentAccountingEntry(payment);

            // Update payment status
            payment.setPaymentStatus(PaymentStatus.COMPLETED);
            payment.setProcessedAt(LocalDateTime.now());

            // Update related vouchers to PAID status
            updateVouchersToStatus(payment);

            PaymentDocEntity savedPayment = paymentDocRepo.save(payment);

            log.info("Payment {} processed successfully with journal entry {}",
                    savedPayment.getPaymentNumber(), journalEntry.getId());

            return mapToPaymentResponse(savedPayment);

        } catch (Exception e) {
            log.error("Failed to process payment {}: {}", payment.getPaymentNumber(), e.getMessage(), e);

            // Update payment status to FAILED
            payment.setPaymentStatus(PaymentStatus.FAILED);
            paymentDocRepo.save(payment);

            throw new RuntimeException("Payment processing failed: " + e.getMessage());
        }
    }

    // ==================================================================
    // HELPER METHODS
    // ==================================================================

    private List<VoucherEntity> validateAndGetVouchers(List<CreateVoucherPaymentRequest.VoucherPaymentItem> voucherItems,
                                                       OrganisationEntity organisation) throws ItemNotFoundException {
        List<VoucherEntity> vouchers = new ArrayList<>();

        for (CreateVoucherPaymentRequest.VoucherPaymentItem item : voucherItems) {
            VoucherEntity voucher = voucherRepo.findByIdAndOrganisation(item.getVoucherId(), organisation)
                    .orElseThrow(() -> new ItemNotFoundException("Voucher not found: " + item.getVoucherId()));

            // Business rule: Can only pay approved vouchers
            if (voucher.getStatus() != VoucherStatus.APPROVED) {
                throw new IllegalStateException("Can only pay approved vouchers. Voucher " +
                        voucher.getVoucherNumber() + " status: " + voucher.getStatus());
            }

            vouchers.add(voucher);
        }

        return vouchers;
    }

    private List<PaymentLineItemEntity> createPaymentLineItems(PaymentDocEntity payment,
                                                               List<VoucherEntity> vouchers,
                                                               CreateVoucherPaymentRequest request) {
        List<PaymentLineItemEntity> lineItems = new ArrayList<>();

        for (int i = 0; i < vouchers.size(); i++) {
            VoucherEntity voucher = vouchers.get(i);
            CreateVoucherPaymentRequest.VoucherPaymentItem requestItem = request.getVouchers().get(i);

            PaymentLineItemEntity lineItem = PaymentLineItemEntity.builder()
                    .payment(payment)
                    .voucher(voucher)
                    .amount(voucher.getTotalAmount())
                    .description(requestItem.getDescription() != null ? requestItem.getDescription() :
                            "Payment for " + voucher.getVoucherNumber())
                    .lineOrder(i + 1)
                    .build();

            lineItems.add(lineItem);
        }

        return lineItems;
    }

    private JournalEntry createPaymentAccountingEntry(PaymentDocEntity payment) throws Exception {

        PaymentEvent paymentEvent = new PaymentEvent();
        paymentEvent.setPaymentId(payment.getId());
        paymentEvent.setPaymentNumber(payment.getPaymentNumber());
        paymentEvent.setTotalAmount(payment.getTotalAmount());
        paymentEvent.setPaymentMode(payment.getPaymentMode().toString());
        paymentEvent.setPaymentReference(payment.getPaymentReference());

        // Set event metadata
        paymentEvent.setOrganisationId(payment.getOrganisation().getOrganisationId());
        paymentEvent.setProjectId(payment.getProject() != null ? payment.getProject().getProjectId() : null);
        paymentEvent.setDescription("Payment processing: " + payment.getPaymentDescription());

        // Add voucher payment info
        List<PaymentEvent.VoucherPaymentInfo> voucherPayments = payment.getLineItems().stream()
                .map(this::convertToVoucherPaymentInfo)
                .collect(Collectors.toList());
        paymentEvent.setVoucherPayments(voucherPayments);

        return transactionService.processBusinessEvent(paymentEvent);
    }

    private PaymentEvent.VoucherPaymentInfo convertToVoucherPaymentInfo(PaymentLineItemEntity lineItem) {
        PaymentEvent.VoucherPaymentInfo info = new PaymentEvent.VoucherPaymentInfo();
        info.setVoucherId(lineItem.getVoucher().getId());
        info.setVoucherNumber(lineItem.getVoucher().getVoucherNumber());
        info.setAmount(lineItem.getAmount());
        info.setDescription(lineItem.getDescription());
        return info;
    }

    private void updateVouchersToStatus(PaymentDocEntity payment) {
        for (PaymentLineItemEntity lineItem : payment.getLineItems()) {
            VoucherEntity voucher = lineItem.getVoucher();

            // Update voucher status
            voucher.setStatus(VoucherStatus.PAID);

            // Update all payees in the voucher
            for (VoucherPayeeEntity payee : voucher.getPayees()) {
                payee.setPaymentStatus(com.qbitspark.buildwisebackend.accounting_service.documentflow.voucher.enums.PaymentStatus.PAID);
                payee.setPaidAt(LocalDateTime.now());
                payee.setPaymentReference(payment.getPaymentReference());
            }

            voucherRepo.save(voucher);
        }
    }

    private PaymentResponse mapToPaymentResponse(PaymentDocEntity payment) {
        PaymentResponse response = new PaymentResponse();
        response.setId(payment.getId());
        response.setPaymentNumber(payment.getPaymentNumber());
        response.setPaymentDate(payment.getPaymentDate());
        response.setPaymentType(payment.getPaymentType());
        response.setPaymentMode(payment.getPaymentMode());
        response.setPaymentStatus(payment.getPaymentStatus());
        response.setTotalAmount(payment.getTotalAmount());
        response.setCurrency(payment.getCurrency());
        response.setPaymentReference(payment.getPaymentReference());
        response.setPaymentDescription(payment.getPaymentDescription());
        response.setNotes(payment.getNotes());
        response.setOrganisationId(payment.getOrganisation().getOrganisationId());
        response.setOrganisationName(payment.getOrganisation().getOrganisationName());
        response.setCreatedByName(payment.getCreatedBy().getAccount().getUserName());
        response.setProcessedAt(payment.getProcessedAt());
        response.setCreatedAt(payment.getCreatedAt());
        response.setUpdatedAt(payment.getUpdatedAt());

        if (payment.getProject() != null) {
            response.setProjectId(payment.getProject().getProjectId());
            response.setProjectName(payment.getProject().getName());
        }

        List<PaymentLineItemResponse> lineItemResponses = payment.getLineItems().stream()
                .map(this::mapToLineItemResponse)
                .collect(Collectors.toList());
        response.setLineItems(lineItemResponses);

        return response;
    }

    private PaymentLineItemResponse mapToLineItemResponse(PaymentLineItemEntity lineItem) {
        PaymentLineItemResponse response = new PaymentLineItemResponse();
        response.setId(lineItem.getId());
        response.setVoucherId(lineItem.getVoucher().getId());
        response.setVoucherNumber(lineItem.getVoucher().getVoucherNumber());
        response.setAmount(lineItem.getAmount());
        response.setDescription(lineItem.getDescription());
        response.setLineOrder(lineItem.getLineOrder());
        return response;
    }

    private ProjectEntity getCommonProject(List<VoucherEntity> vouchers) {
        // If all vouchers belong to same project, return it; otherwise return null
        if (vouchers.isEmpty()) return null;

        ProjectEntity firstProject = vouchers.get(0).getProject();
        boolean allSameProject = vouchers.stream()
                .allMatch(v -> v.getProject() != null && v.getProject().equals(firstProject));

        return allSameProject ? firstProject : null;
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

    private OrganisationMember validateOrganisationAccess(AccountEntity account, OrganisationEntity organisation,
                                                          List<MemberRole> allowedRoles) throws ItemNotFoundException, AccessDeniedException {

        OrganisationMember member = organisationMemberRepo.findByAccountAndOrganisation(account, organisation)
                .orElseThrow(() -> new ItemNotFoundException("User is not a member of this organisation"));

        if (member.getStatus() != MemberStatus.ACTIVE) {
            throw new AccessDeniedException("User membership is not active");
        }

        if (!allowedRoles.contains(member.getRole())) {
            throw new AccessDeniedException("User does not have sufficient permissions");
        }

        return member;
    }
}
