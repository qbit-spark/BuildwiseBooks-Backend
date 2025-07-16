package com.qbitspark.buildwisebackend.accounting_service.documentflow.voucher.service.impl;

import com.qbitspark.buildwisebackend.accounting_service.documentflow.voucher.entity.VoucherBeneficiaryEntity;
import com.qbitspark.buildwisebackend.accounting_service.documentflow.voucher.entity.VoucherDeductionEntity;
import com.qbitspark.buildwisebackend.accounting_service.documentflow.voucher.entity.VoucherEntity;
import com.qbitspark.buildwisebackend.accounting_service.documentflow.voucher.service.VoucherPrintService;
import com.qbitspark.buildwisebackend.accounting_service.documentflow.voucher.utils.NumberToWordsUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.xhtmlrenderer.pdf.ITextRenderer;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
@Slf4j
public class VoucherPrintServiceImpl implements VoucherPrintService {

    private final TemplateEngine templateEngine;

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

            // Clean HTML for XHTML compliance
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
            context.setVariable("createdByName", voucher.getCreatedBy().getAccount().getUserName());

            // Dates
            context.setVariable("voucherDate", voucher.getVoucherDate().format(DATE_FORMATTER));
            context.setVariable("createdAt", voucher.getCreatedAt().format(DATE_TIME_FORMATTER));

            // Description
            context.setVariable("description", voucher.getOverallDescription());

            // Beneficiaries and their net amounts
            context.setVariable("beneficiaries", voucher.getBeneficiaries());

            // Pre-calculate net amounts for each beneficiary to avoid complex SpEL
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

    /**
     * Cleans HTML to ensure XHTML compliance for PDF generation
     */
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

        // Remove any remaining unclosed tags that might cause issues
        html = html.replaceAll("<area([^>]*?)(?<!/)>", "<area$1/>");
        html = html.replaceAll("<base([^>]*?)(?<!/)>", "<base$1/>");
        html = html.replaceAll("<col([^>]*?)(?<!/)>", "<col$1/>");
        html = html.replaceAll("<embed([^>]*?)(?<!/)>", "<embed$1/>");
        html = html.replaceAll("<source([^>]*?)(?<!/)>", "<source$1/>");
        html = html.replaceAll("<track([^>]*?)(?<!/)>", "<track$1/>");
        html = html.replaceAll("<wbr([^>]*?)(?<!/)>", "<wbr$1/>");

        return html;
    }

}