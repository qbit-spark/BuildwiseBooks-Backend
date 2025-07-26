package com.qbitspark.buildwisebackend.accounting_service.documentflow.voucher.service.impl;

import com.qbitspark.buildwisebackend.accounting_service.documentflow.voucher.entity.VoucherBeneficiaryEntity;
import com.qbitspark.buildwisebackend.accounting_service.documentflow.voucher.entity.VoucherDeductionEntity;
import com.qbitspark.buildwisebackend.accounting_service.documentflow.voucher.entity.VoucherEntity;
import com.qbitspark.buildwisebackend.accounting_service.documentflow.voucher.service.VoucherPrintService;
import com.qbitspark.buildwisebackend.accounting_service.documentflow.voucher.utils.NumberToWordsUtil;
import com.qbitspark.buildwisebackend.approval_service.entities.ApprovalInstance;
import com.qbitspark.buildwisebackend.approval_service.entities.ApprovalStepInstance;
import com.qbitspark.buildwisebackend.approval_service.entities.embedings.ApprovalRecord;
import com.qbitspark.buildwisebackend.approval_service.enums.ServiceType;
import com.qbitspark.buildwisebackend.approval_service.enums.StepStatus;
import com.qbitspark.buildwisebackend.approval_service.repo.ApprovalInstanceRepo;
import com.qbitspark.buildwisebackend.approval_service.repo.ApprovalStepInstanceRepo;
import com.qbitspark.buildwisebackend.authentication_service.Repository.AccountRepo;
import com.qbitspark.buildwisebackend.authentication_service.entity.AccountEntity;
import com.qbitspark.buildwisebackend.organisation_service.roles_mng.entity.OrgMemberRoleEntity;
import com.qbitspark.buildwisebackend.organisation_service.roles_mng.repo.OrgMemberRoleRepo;
import com.qbitspark.buildwisebackend.projectmng_service.entity.ProjectTeamRoleEntity;
import com.qbitspark.buildwisebackend.projectmng_service.repo.ProjectTeamRoleRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.xhtmlrenderer.pdf.ITextRenderer;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class VoucherPrintServiceImpl implements VoucherPrintService {

    private final TemplateEngine templateEngine;
    private final ApprovalInstanceRepo approvalInstanceRepo;
    private final ApprovalStepInstanceRepo approvalStepInstanceRepo;
    private final AccountRepo accountRepo;
    private final OrgMemberRoleRepo orgMemberRoleRepo;
    private final ProjectTeamRoleRepo projectTeamRoleRepo;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MMMM dd, yyyy");
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("MMMM dd, yyyy 'at' hh:mm a");
    private static final NumberFormat CURRENCY_FORMATTER = NumberFormat.getNumberInstance(Locale.US);

    static {
        CURRENCY_FORMATTER.setMinimumFractionDigits(2);
        CURRENCY_FORMATTER.setMaximumFractionDigits(2);
    }

    @Override
    public byte[] generateVoucherPdf(VoucherEntity voucher) {
        try {
            log.info("Generating PDF for voucher: {}", voucher.getVoucherNumber());

            String html = generateVoucherHtml(voucher);
            html = cleanHtmlForPdf(html);

            try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
                ITextRenderer renderer = new ITextRenderer();
                renderer.setDocumentFromString(html);
                renderer.layout();
                renderer.createPDF(outputStream);

                byte[] pdfBytes = outputStream.toByteArray();
                log.info("PDF generated successfully. Size: {} bytes", pdfBytes.length);
                return pdfBytes;
            }
        } catch (Exception e) {
            log.error("Error generating PDF for voucher: {}", voucher.getVoucherNumber(), e);
            throw new RuntimeException("Failed to generate PDF", e);
        }
    }

    @Override
    public String generateVoucherHtml(VoucherEntity voucher) {
        try {
            log.info("Generating HTML for voucher: {}", voucher.getVoucherNumber());

            Context context = new Context();

            // Basic voucher information
            context.setVariable("voucher", voucher);
            context.setVariable("voucherNumber", voucher.getVoucherNumber());
            context.setVariable("status", voucher.getStatus());
            context.setVariable("statusClass", getStatusClass(voucher.getStatus()));
            context.setVariable("currency", voucher.getCurrency());

            // Organisation information
            context.setVariable("organisationName", voucher.getOrganisation().getOrganisationName());

            // Project information
            context.setVariable("projectName", voucher.getProject().getName());
            context.setVariable("projectCode", voucher.getProject().getProjectCode());

            // Account information
            context.setVariable("accountCode", voucher.getAccount().getAccountCode());
            context.setVariable("accountName", voucher.getAccount().getName());

            // Creator information
            String creatorFullName = getFullName(voucher.getCreatedBy().getAccount());
            context.setVariable("createdByName", creatorFullName);

            // Dates
            context.setVariable("voucherDate", voucher.getVoucherDate().format(DATE_FORMATTER));
            context.setVariable("createdAt", voucher.getCreatedAt().format(DATE_TIME_FORMATTER));

            // Description
            context.setVariable("description", voucher.getOverallDescription());

            // Beneficiaries and their net amounts
            context.setVariable("beneficiaries", voucher.getBeneficiaries());

            // Pre-calculate net amounts for each beneficiary
            List<BigDecimal> beneficiaryNetAmounts = voucher.getBeneficiaries().stream()
                    .map(this::calculateBeneficiaryNetAmount)
                    .collect(Collectors.toList());
            context.setVariable("beneficiaryNetAmounts", beneficiaryNetAmounts);

            // Financial calculations
            Map<String, BigDecimal> totals = calculateTotals(voucher);
            context.setVariable("totalGrossAmount", formatCurrency(totals.get("grossAmount")));
            context.setVariable("totalDeductions", formatCurrency(totals.get("totalDeductions")));
            context.setVariable("totalNetAmount", formatCurrency(totals.get("netAmount")));
            context.setVariable("amountInWords", convertToWords(totals.get("netAmount")));

            // NEW: Get approval signatures
            List<ApprovalSignature> signatures = getApprovalSignatures(voucher);
            context.setVariable("approvalSignatures", signatures);

            // NEW: Add University and Company logos
            String universityLogo = getUniversityLogoBase64();
            String companyLogo = getCompanyLogoBase64();
            context.setVariable("universityLogoImage", universityLogo);
            context.setVariable("companyLogoImage", companyLogo);

            log.info("University logo set: {}", universityLogo != null ? "Yes" : "No");
            log.info("Company logo set: {}", companyLogo != null ? "Yes" : "No");
            if (universityLogo != null) {
                log.info("University logo length: {} characters", universityLogo.length());
            }
            if (companyLogo != null) {
                log.info("Company logo length: {} characters", companyLogo.length());
            }

            // Watermark for draft status
            context.setVariable("showWatermark", voucher.getStatus().name().equals("DRAFT"));

            // Helper functions for template
            context.setVariable("currencyFormatter", CURRENCY_FORMATTER);
            context.setVariable("dateFormatter", DATE_FORMATTER);

            String html = templateEngine.process("voucher-template", context);
            log.info("HTML generated successfully for voucher: {}", voucher.getVoucherNumber());
            return html;

        } catch (Exception e) {
            log.error("Error generating HTML for voucher: {}", voucher.getVoucherNumber(), e);
            throw new RuntimeException("Failed to generate HTML", e);
        }
    }


    private String getUniversityLogoBase64() {
        try {
            // Load from classpath resources - full path from resources root
            InputStream imageStream = getClass().getResourceAsStream("/must_logo.png");
            if (imageStream != null) {
                byte[] imageBytes = imageStream.readAllBytes();

                // Validate the PNG header
                if (imageBytes.length > 8 &&
                        imageBytes[0] == (byte)0x89 && imageBytes[1] == 0x50 &&
                        imageBytes[2] == 0x4E && imageBytes[3] == 0x47) {

                    String base64 = java.util.Base64.getEncoder().encodeToString(imageBytes);
                    log.info("University logo loaded successfully from /must_logo.png, size: {} bytes", imageBytes.length);
                    return "data:image/png;base64," + base64;
                } else {
                    log.warn("University logo file is not a valid PNG format");
                }
            } else {
                log.warn("University logo not found at /must_logo.png - will use template fallback");
                return null; // Let template handle the fallback
            }

        } catch (Exception e) {
            log.error("Error loading university logo", e);
        }

        return null; // Use template fallback
    }


    private String getCompanyLogoBase64() {
        try {
            // Load from classpath resources - full path from resources root
            InputStream imageStream = getClass().getResourceAsStream("/mcb_logo.png");
            if (imageStream != null) {
                byte[] imageBytes = imageStream.readAllBytes();

                // Validate the PNG header
                if (imageBytes.length > 8 &&
                        imageBytes[0] == (byte)0x89 && imageBytes[1] == 0x50 &&
                        imageBytes[2] == 0x4E && imageBytes[3] == 0x47) {

                    String base64 = java.util.Base64.getEncoder().encodeToString(imageBytes);
                    log.info("Company logo loaded successfully from /mcb_logo.png, size: {} bytes", imageBytes.length);
                    return "data:image/png;base64," + base64;
                } else {
                    log.warn("Company logo file is not a valid PNG format");
                }
            } else {
                log.warn("Company logo not found at /mcb_logo.png - will use template fallback");
                return null; // Let template handle the fallback
            }

        } catch (Exception e) {
            log.error("Error loading company logo", e);
        }

        return null; // Use template fallback
    }

    /**
     * NEW METHOD: Get approval signatures for the voucher
     */
    private List<ApprovalSignature> getApprovalSignatures(VoucherEntity voucher) {
        List<ApprovalSignature> signatures = new ArrayList<>();

        try {
            // Always add the creator signature
            String creatorFullName = getFullName(voucher.getCreatedBy().getAccount());
            signatures.add(ApprovalSignature.builder()
                    .roleTitle("Prepared By")
                    .userName(creatorFullName)
                    .roleName("Requester")
                    .signedDate(voucher.getCreatedAt().format(DATE_FORMATTER))
                    .isSigned(true)
                    .build());

            // Get an approval instance for this voucher
            Optional<ApprovalInstance> approvalInstanceOpt = approvalInstanceRepo
                    .findByServiceNameAndItemId(ServiceType.VOUCHER, voucher.getId());

            if (approvalInstanceOpt.isPresent()) {
                ApprovalInstance instance = approvalInstanceOpt.get();

                // Get all step instances but skip the first approval step
                List<ApprovalStepInstance> steps = approvalStepInstanceRepo
                        .findByApprovalInstanceOrderByStepOrderAsc(instance);

                // Skip the first step (index 0) and process the remaining steps
                for (int i = 1; i < steps.size(); i++) {
                    ApprovalStepInstance step = steps.get(i);
                    ApprovalSignature signature = buildSignatureFromStep(step);
                    if (signature != null) {
                        signatures.add(signature);
                    }
                }
            }

            // If no approval workflow exists or no approvals found, add default placeholders
            if (signatures.size() == 1) { // Only creator
                signatures.add(ApprovalSignature.builder()
                        .roleTitle("Examined By")
                        .userName("")
                        .roleName("Finance Manager")
                        .signedDate("")
                        .isSigned(false)
                        .build());

                signatures.add(ApprovalSignature.builder()
                        .roleTitle("Approved By")
                        .userName("")
                        .roleName("Managing Director")
                        .signedDate("")
                        .isSigned(false)
                        .build());
            }

        } catch (Exception e) {
            log.error("Error getting approval signatures for voucher: {}", voucher.getVoucherNumber(), e);

            // Return minimal signatures on error
            signatures.clear();
            String creatorFullName = getFullName(voucher.getCreatedBy().getAccount());
            signatures.add(ApprovalSignature.builder()
                    .roleTitle("Prepared By")
                    .userName(creatorFullName)
                    .roleName("Requester")
                    .signedDate(voucher.getCreatedAt().format(DATE_FORMATTER))
                    .isSigned(true)
                    .build());
        }

        return signatures;
    }

    /**
     * Build signature from approval step
     */
    private ApprovalSignature buildSignatureFromStep(ApprovalStepInstance step) {
        try {
            String roleName = getRoleName(step.getRoleId(), step.getScopeType());
            String roleTitle = determineRoleTitle(roleName, step.getStepOrder());

            if (step.getStatus() == StepStatus.APPROVED && step.getApprovedBy() != null) {
                // Step is approved - get user full name and date
                AccountEntity approver = accountRepo.findById(step.getApprovedBy()).orElse(null);
                String approverName = getFullName(approver);

                log.debug("Found approver for step {}: ID={}, Name={}", step.getStepOrder(), step.getApprovedBy(), approverName);

                // Try to get the latest approval from history for more details
                ApprovalRecord latestApproval = step.getApprovalHistory().stream()
                        .filter(record -> record.isActive())
                        .findFirst()
                        .orElse(null);

                String signedDate = "";
                if (step.getApprovedAt() != null) {
                    signedDate = step.getApprovedAt().format(DATE_FORMATTER);
                } else if (latestApproval != null && latestApproval.getApprovedAt() != null) {
                    signedDate = latestApproval.getApprovedAt().format(DATE_FORMATTER);
                }

                // If we couldn't get name from step.approvedBy, try to get it from approval history
                if ("Unknown".equals(approverName) && latestApproval != null) {
                    // The approval history stores approvedBy as username/email, let's try to find the account
                    String approvedByFromHistory = latestApproval.getApprovedBy();
                    if (approvedByFromHistory != null && !approvedByFromHistory.isEmpty()) {
                        // Try to find by username first
                        AccountEntity accountFromHistory = accountRepo.findByUserName(approvedByFromHistory).orElse(null);

                        // If not found by username, try by email (if the method exists)
                        if (accountFromHistory == null) {
                            try {
                                accountFromHistory = accountRepo.findByEmail(approvedByFromHistory).orElse(null);
                            } catch (Exception e) {
                                // findByEmail method might not exist, ignore and continue
                                log.debug("Could not search by email, method might not exist");
                            }
                        }

                        if (accountFromHistory != null) {
                            approverName = getFullName(accountFromHistory);
                            log.debug("Found approver from history: {}", approverName);
                        } else {
                            // If we can't find the account, just use the stored name
                            approverName = approvedByFromHistory;
                            log.debug("Using stored name from history: {}", approverName);
                        }
                    }
                }

                return ApprovalSignature.builder()
                        .roleTitle(roleTitle)
                        .userName(approverName)
                        .roleName(roleName)
                        .signedDate(signedDate)
                        .isSigned(true)
                        .build();

            } else {
                // Step is not approved - show placeholder
                return ApprovalSignature.builder()
                        .roleTitle(roleTitle)
                        .userName("")
                        .roleName(roleName)
                        .signedDate("")
                        .isSigned(false)
                        .build();
            }

        } catch (Exception e) {
            log.error("Error building signature from step: {}", step.getStepInstanceId(), e);
            return null;
        }
    }

    /**
     * Get full name from AccountEntity
     * Combines firstName and lastName, falls back to userName if names are not available
     */
    private String getFullName(AccountEntity account) {
        if (account == null) {
            return "Unknown";
        }

        try {
            String firstName = account.getFirstName();
            String lastName = account.getLastName();

            // Build full name if both parts are available
            if (firstName != null && !firstName.trim().isEmpty() &&
                    lastName != null && !lastName.trim().isEmpty()) {
                return (firstName.trim() + " " + lastName.trim()).trim();
            }

            // Use first name only if last name is missing
            if (firstName != null && !firstName.trim().isEmpty()) {
                return firstName.trim();
            }

            // Use last name only if first name is missing
            if (lastName != null && !lastName.trim().isEmpty()) {
                return lastName.trim();
            }

            // Fall back to username if no names are available
            return account.getUserName() != null ? account.getUserName() : "Unknown";

        } catch (Exception e) {
            log.error("Error getting full name for account: {}", account.getId(), e);
            return account.getUserName() != null ? account.getUserName() : "Unknown";
        }
    }

    /**
     * Get role name from role ID and scope type
     */
    private String getRoleName(UUID roleId, com.qbitspark.buildwisebackend.approval_service.enums.ScopeType scopeType) {
        try {
            switch (scopeType) {
                case ORGANIZATION -> {
                    Optional<OrgMemberRoleEntity> orgRole = orgMemberRoleRepo.findById(roleId);
                    return orgRole.map(OrgMemberRoleEntity::getRoleName).orElse("Unknown Role");
                }
                case PROJECT -> {
                    Optional<ProjectTeamRoleEntity> projectRole = projectTeamRoleRepo.findById(roleId);
                    return projectRole.map(ProjectTeamRoleEntity::getRoleName).orElse("Unknown Role");
                }
                default -> {
                    return "Unknown Role";
                }
            }
        } catch (Exception e) {
            log.error("Error getting role name for roleId: {}, scopeType: {}", roleId, scopeType, e);
            return "Unknown Role";
        }
    }

    /**
     * Determine appropriate role title for signature section
     */
    private String determineRoleTitle(String roleName, int stepOrder) {
        // Since we're skipping the first step, adjust the logic
        // stepOrder 2 becomes first approval, stepOrder 3+ becomes subsequent approvals
        if (roleName.toLowerCase().contains("finance")) {
            return "Examined By";
        } else if (roleName.toLowerCase().contains("manager") ||
                roleName.toLowerCase().contains("director") ||
                roleName.toLowerCase().contains("ceo")) {
            return "Approved By";
        } else if (stepOrder == 2) { // This is now the first approval step we show
            return "Examined By";
        } else {
            return "Approved By";
        }
    }


    private Map<String, BigDecimal> calculateTotals(VoucherEntity voucher) {
        BigDecimal grossAmount = voucher.getTotalAmount();

        BigDecimal totalDeductions = voucher.getBeneficiaries().stream()
                .flatMap(beneficiary -> beneficiary.getDeductions().stream())
                .map(VoucherDeductionEntity::getDeductionAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal netAmount = grossAmount.subtract(totalDeductions);

        return Map.of(
                "grossAmount", grossAmount,
                "totalDeductions", totalDeductions,
                "netAmount", netAmount
        );
    }

    private String getStatusClass(Enum<?> status) {
        return switch (status.name()) {
            case "DRAFT" -> "status-draft";
            case "PENDING_APPROVAL" -> "status-pending";
            case "APPROVED" -> "status-approved";
            case "PAID" -> "status-paid";
            case "REJECTED" -> "status-rejected";
            case "CANCELLED" -> "status-cancelled";
            default -> "status-draft";
        };
    }

    private String formatCurrency(BigDecimal amount) {
        return CURRENCY_FORMATTER.format(amount);
    }

    private String convertToWords(BigDecimal amount) {
        return NumberToWordsUtil.convertToWords(amount);
    }

    public BigDecimal calculateBeneficiaryNetAmount(VoucherBeneficiaryEntity beneficiary) {
        BigDecimal deductions = beneficiary.getDeductions().stream()
                .map(VoucherDeductionEntity::getDeductionAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return beneficiary.getAmount().subtract(deductions);
    }

    private String cleanHtmlForPdf(String html) {
        if (html == null) {
            return "";
        }

        // Fix common XHTML issues
        html = html.replaceAll("<br>", "<br/>");
        html = html.replaceAll("<hr>", "<hr/>");
        html = html.replaceAll("<img([^>]*?)(?<!/)>", "<img$1/>");
        html = html.replaceAll("<input([^>]*?)(?<!/)>", "<input$1/>");
        html = html.replaceAll("<meta([^>]*?)(?<!/)>", "<meta$1/>");
        html = html.replaceAll("<link([^>]*?)(?<!/)>", "<link$1/>");

        return html;
    }

    /**
     * Inner class to represent approval signature
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class ApprovalSignature {
        private String roleTitle;    // "Prepared By", "Reviewed By", "Approved By"
        private String userName;     // Actual user name who signed
        private String roleName;     // Role name like "Finance Manager", "CEO"
        private String signedDate;   // Date when signed
        private boolean isSigned;    // Whether this step is actually signed
    }
}