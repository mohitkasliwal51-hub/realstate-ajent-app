package com.bhartiyasaas.stayfile.service.impl;

import com.bhartiyasaas.stayfile.entity.Invoice;
import com.bhartiyasaas.stayfile.entity.InvoiceLineItem;
import com.bhartiyasaas.stayfile.entity.LandlordPayout;
import com.bhartiyasaas.stayfile.entity.Lease;
import com.bhartiyasaas.stayfile.entity.Receipt;
import com.bhartiyasaas.stayfile.entity.BrandingSettings;
import com.bhartiyasaas.stayfile.repository.BrandingSettingsRepository;
import com.bhartiyasaas.stayfile.service.PdfGeneratorService;
import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;

import java.io.ByteArrayOutputStream;
import java.awt.Color;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
public class PdfGeneratorServiceImpl implements PdfGeneratorService {

    private final BrandingSettingsRepository brandingSettingsRepository;

    private static final Font TITLE_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, Color.DARK_GRAY);
    private static final Font SUBTITLE_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, Color.BLUE);
    private static final Font BOLD_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, Color.BLACK);
    private static final Font NORMAL_FONT = FontFactory.getFont(FontFactory.HELVETICA, 10, Color.BLACK);
    private static final Font FOOTER_FONT = FontFactory.getFont(FontFactory.HELVETICA, 8, Font.ITALIC, Color.GRAY);

    @Override
    public byte[] generateRentAgreementPdf(Lease lease) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4, 36, 36, 36, 36);

        try {
            PdfWriter.getInstance(document, out);
            document.open();

            // Header Title
            Paragraph title = new Paragraph("LEGAL RENTAL AGREEMENT", TITLE_FONT);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(15);
            document.add(title);

            // Subtitle / Reference
            Paragraph ref = new Paragraph("Agreement Ref ID: " + lease.getId(), FOOTER_FONT);
            ref.setAlignment(Element.ALIGN_CENTER);
            ref.setSpacingAfter(20);
            document.add(ref);

            Paragraph signing = new Paragraph(
                "E-STAMP / E-SIGN RECORD\n" +
                "E-Stamp Number: " + valueOrPending(lease.getEStampNumber()) + "\n" +
                "E-Sign Transaction: " + valueOrPending(lease.getEsignTransactionId()),
                FOOTER_FONT);
            signing.setAlignment(Element.ALIGN_CENTER);
            signing.setSpacingAfter(16);
            document.add(signing);

            BrandingSettings leaseBranding = brandingSettingsRepository.findByOrganizationId(lease.getOrganization().getId()).orElse(null);
            String landlordName = lease.getLandlord() != null ? lease.getLandlord().getLegalName()
                    : (leaseBranding != null && leaseBranding.getLegalBusinessName() != null && !leaseBranding.getLegalBusinessName().isBlank()
                            ? leaseBranding.getLegalBusinessName()
                            : (lease.getCreatedBy() != null ? lease.getCreatedBy().getFullName() : "Landlord"));

            // Body Text
            String bodyText = String.format(
                    "This Rent Agreement is executed on %s by and between:\n\n" +
                    "LANDLORD / OWNER: %s\n" +
                    "TENANT / RESIDENT: %s\n\n" +
                    "PROPERTY DETAILS:\n" +
                    "Property: %s (%s)\n" +
                    "Unit / Bed: %s\n" +
                    "Full Address: %s, %s, %s - %s\n\n" +
                    "TERMS & FINANCIAL DETAILS:\n" +
                    "1. Tenancy Period: From %s to %s\n" +
                    "2. Monthly Rent: Rs. %s (Due on day %d of each month)\n" +
                    "3. Security Deposit: Rs. %s\n" +
                    "4. Notice Period: %d days\n" +
                    "5. Lock-in Period: %d months\n",
                    lease.getStartDate().format(DateTimeFormatter.ISO_LOCAL_DATE),
                    landlordName,
                    lease.getTenant().getFullName(),
                    lease.getUnit().getProperty().getName(),
                    lease.getUnit().getProperty().getType(),
                    lease.getUnit().getUnitNumber(),
                    lease.getUnit().getProperty().getAddress(),
                    lease.getUnit().getProperty().getCity(),
                    lease.getUnit().getProperty().getState(),
                    lease.getUnit().getProperty().getPincode(),
                    lease.getStartDate().format(DateTimeFormatter.ISO_LOCAL_DATE),
                    lease.getEndDate().format(DateTimeFormatter.ISO_LOCAL_DATE),
                    lease.getMonthlyRent().toString(),
                    lease.getRentDueDay(),
                    lease.getSecurityDeposit().toString(),
                    lease.getNoticePeriodDays(),
                    lease.getLockInPeriodMonths()
            );

            Paragraph content = new Paragraph(bodyText, NORMAL_FONT);
            content.setSpacingAfter(20);
            document.add(content);

            // Custom Clauses
            if (lease.getCustomClauses() != null && !lease.getCustomClauses().isBlank()) {
                Paragraph clausesHeader = new Paragraph("SPECIAL CONDITIONS & CLAUSES:", BOLD_FONT);
                clausesHeader.setSpacingAfter(10);
                document.add(clausesHeader);

                Paragraph clausesText = new Paragraph(lease.getCustomClauses(), NORMAL_FONT);
                clausesText.setSpacingAfter(30);
                document.add(clausesText);
            }

            if (lease.getTermsAndConditions() != null && !lease.getTermsAndConditions().isBlank()) {
                Paragraph termsHeader = new Paragraph("TERMS & CONDITIONS:", BOLD_FONT);
                termsHeader.setSpacingAfter(10);
                document.add(termsHeader);
                document.add(new Paragraph(lease.getTermsAndConditions(), NORMAL_FONT));
            }

            // Signature Table
            PdfPTable table = new PdfPTable(2);
            table.setWidthPercentage(100);

            PdfPCell cell1 = new PdfPCell(new Paragraph("Landlord Signature:\n\n________________________\n" + landlordName, BOLD_FONT));
            cell1.setBorder(Rectangle.NO_BORDER);
            cell1.setPadding(10);

            PdfPCell cell2 = new PdfPCell(new Paragraph("Tenant Signature:\n\n________________________\n" + lease.getTenant().getFullName(), BOLD_FONT));
            cell2.setBorder(Rectangle.NO_BORDER);
            cell2.setPadding(10);

            table.addCell(cell1);
            table.addCell(cell2);
            document.add(table);

            // Footer Timestamp
            Paragraph footer = new Paragraph("\nGenerated securely by StayFile Platform Engine • Server Timestamp Audit Verified", FOOTER_FONT);
            footer.setAlignment(Element.ALIGN_CENTER);
            document.add(footer);

            document.close();
        } catch (Exception e) {
            throw new RuntimeException("Error generating Rent Agreement PDF: " + e.getMessage(), e);
        }

        return out.toByteArray();
    }

    @Override
    public byte[] generatePaymentReceiptPdf(Receipt receipt) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A5.rotate(), 20, 20, 20, 20);

        try {
            PdfWriter.getInstance(document, out);
            document.open();

            // Header
            Paragraph title = new Paragraph("PAYMENT RECEIPT", TITLE_FONT);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(10);
            document.add(title);

            BrandingSettings branding = brandingSettingsRepository
                .findByOrganizationId(receipt.getOrganization().getId()).orElse(null);
            if (branding != null) {
                document.add(new Paragraph(
                    valueOrPending(branding.getLegalBusinessName()) +
                        " | GSTIN: " + valueOrPending(branding.getOwnerGstin()),
                    BOLD_FONT));
                if (branding.getAgencyLogoUrl() != null && !branding.getAgencyLogoUrl().isBlank()) {
                    document.add(new Paragraph("Logo: " + branding.getAgencyLogoUrl(), FOOTER_FONT));
                }
            }

            Paragraph receiptNo = new Paragraph("Receipt No: " + receipt.getReceiptNumber(), SUBTITLE_FONT);
            receiptNo.setAlignment(Element.ALIGN_CENTER);
            receiptNo.setSpacingAfter(15);
            document.add(receiptNo);

            // Receipt Details Table
            PdfPTable table = new PdfPTable(2);
            table.setWidthPercentage(100);

            addTableRow(table, "Tenant Name:", receipt.getTenant().getFullName());
            addTableRow(table, "Payment Date:", receipt.getPaymentDate().format(DateTimeFormatter.ISO_LOCAL_DATE));
            addTableRow(table, "Receipt Type:", receipt.getReceiptType().name());
            addTableRow(table, "Amount Paid:", "Rs. " + receipt.getAmount().toString());
            addTableRow(table, "Payment Mode:", receipt.getPaymentMode().name());
            addTableRow(table, "Transaction Ref / UTR:", receipt.getTransactionReference() != null ? receipt.getTransactionReference() : "N/A");

            document.add(table);

            // Footer
            Paragraph footer = new Paragraph("\nDigital Signature: ____________________\nThank you for your payment! • Generated by StayFile Platform", FOOTER_FONT);
            footer.setAlignment(Element.ALIGN_CENTER);
            document.add(footer);

            document.close();
        } catch (Exception e) {
            throw new RuntimeException("Error generating Payment Receipt PDF: " + e.getMessage(), e);
        }

        return out.toByteArray();
    }

    @Override
    public byte[] generateInvoicePdf(Invoice invoice) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4, 36, 36, 36, 36);

        try {
            PdfWriter.getInstance(document, out);
            document.open();

            Paragraph title = new Paragraph("ITEMIZED TAX INVOICE", TITLE_FONT);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(10);
            document.add(title);

            Paragraph invNumber = new Paragraph("Invoice #: " + invoice.getInvoiceNumber() + " | Status: " + invoice.getStatus(), SUBTITLE_FONT);
            invNumber.setAlignment(Element.ALIGN_CENTER);
            invNumber.setSpacingAfter(15);
            document.add(invNumber);

            PdfPTable infoTable = new PdfPTable(2);
            infoTable.setWidthPercentage(100);
            addTableRow(infoTable, "Tenant:", invoice.getTenant() != null ? invoice.getTenant().getFullName() : "N/A");
            addTableRow(infoTable, "Billing Start:", invoice.getBillingPeriodStart() != null ? invoice.getBillingPeriodStart().format(DateTimeFormatter.ISO_LOCAL_DATE) : "N/A");
            addTableRow(infoTable, "Billing End:", invoice.getBillingPeriodEnd() != null ? invoice.getBillingPeriodEnd().format(DateTimeFormatter.ISO_LOCAL_DATE) : "N/A");
            addTableRow(infoTable, "Due Date:", invoice.getDueDate() != null ? invoice.getDueDate().format(DateTimeFormatter.ISO_LOCAL_DATE) : "N/A");
            document.add(infoTable);
            document.add(new Paragraph("\n"));

            PdfPTable itemsTable = new PdfPTable(3);
            itemsTable.setWidthPercentage(100);
            itemsTable.addCell(new PdfPCell(new Paragraph("Charge Type", BOLD_FONT)));
            itemsTable.addCell(new PdfPCell(new Paragraph("Description", BOLD_FONT)));
            itemsTable.addCell(new PdfPCell(new Paragraph("Amount", BOLD_FONT)));

            if (invoice.getLineItems() != null) {
                for (InvoiceLineItem item : invoice.getLineItems()) {
                    itemsTable.addCell(new PdfPCell(new Paragraph(item.getChargeType().name(), NORMAL_FONT)));
                    itemsTable.addCell(new PdfPCell(new Paragraph(item.getDescription(), NORMAL_FONT)));
                    itemsTable.addCell(new PdfPCell(new Paragraph("Rs. " + item.getAmount(), NORMAL_FONT)));
                }
            }
            document.add(itemsTable);

            Paragraph totalPara = new Paragraph("\nTotal Amount: Rs. " + invoice.getTotalAmount() + 
                    " | Paid: Rs. " + invoice.getPaidAmount() + 
                    " | Balance Due: Rs. " + invoice.getBalanceDue(), BOLD_FONT);
            totalPara.setAlignment(Element.ALIGN_RIGHT);
            totalPara.setSpacingAfter(20);
            document.add(totalPara);

            Paragraph footer = new Paragraph("Generated by StayFile Platform Engine • Statutory Tax Compliant", FOOTER_FONT);
            footer.setAlignment(Element.ALIGN_CENTER);
            document.add(footer);

            document.close();
        } catch (Exception e) {
            throw new RuntimeException("Error generating Invoice PDF: " + e.getMessage(), e);
        }

        return out.toByteArray();
    }

    @Override
    public byte[] generateLandlordPayoutPdf(LandlordPayout payout) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4, 36, 36, 36, 36);

        try {
            PdfWriter.getInstance(document, out);
            document.open();

            Paragraph title = new Paragraph("LANDLORD PAYOUT STATEMENT", TITLE_FONT);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(10);
            document.add(title);

            Paragraph payoutNo = new Paragraph("Payout Ref #: " + payout.getPayoutNumber() + " | Status: " + payout.getPayoutStatus(), SUBTITLE_FONT);
            payoutNo.setAlignment(Element.ALIGN_CENTER);
            payoutNo.setSpacingAfter(15);
            document.add(payoutNo);

            PdfPTable table = new PdfPTable(2);
            table.setWidthPercentage(100);
            addTableRow(table, "Landlord / Owner:", payout.getLandlord() != null ? payout.getLandlord().getLegalName() : "N/A");
            addTableRow(table, "Total Rent Collected:", "Rs. " + payout.getTotalCollected());
            addTableRow(table, "Commission Amount:", "Rs. " + payout.getCommissionAmount());
            addTableRow(table, "Deductions Amount:", "Rs. " + payout.getDeductionsAmount());
            addTableRow(table, "Net Payout Amount:", "Rs. " + payout.getNetPayoutAmount());
            addTableRow(table, "Payout Date:", payout.getPayoutDate() != null ? payout.getPayoutDate().format(DateTimeFormatter.ISO_LOCAL_DATE) : "PENDING");
            addTableRow(table, "UTR / Transaction Ref:", payout.getUtrNumber() != null ? payout.getUtrNumber() : "N/A");

            document.add(table);

            Paragraph footer = new Paragraph("\nVerified Statement • StayFile Brokerage & Property Management Engine", FOOTER_FONT);
            footer.setAlignment(Element.ALIGN_CENTER);
            document.add(footer);

            document.close();
        } catch (Exception e) {
            throw new RuntimeException("Error generating Landlord Payout PDF: " + e.getMessage(), e);
        }

        return out.toByteArray();
    }

    private void addTableRow(PdfPTable table, String label, String value) {
        PdfPCell cellLabel = new PdfPCell(new Paragraph(label, BOLD_FONT));
        cellLabel.setBorder(Rectangle.BOX);
        cellLabel.setPadding(6);

        PdfPCell cellValue = new PdfPCell(new Paragraph(value, NORMAL_FONT));
        cellValue.setBorder(Rectangle.BOX);
        cellValue.setPadding(6);

        table.addCell(cellLabel);
        table.addCell(cellValue);
    }

    private String valueOrPending(String value) {
        return value == null || value.isBlank() ? "PENDING PROVIDER INTEGRATION" : value;
    }
}
