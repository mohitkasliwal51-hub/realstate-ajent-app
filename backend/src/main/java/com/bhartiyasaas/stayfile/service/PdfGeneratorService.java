package com.bhartiyasaas.stayfile.service;

import com.bhartiyasaas.stayfile.entity.Invoice;
import com.bhartiyasaas.stayfile.entity.LandlordPayout;
import com.bhartiyasaas.stayfile.entity.Lease;
import com.bhartiyasaas.stayfile.entity.Receipt;

public interface PdfGeneratorService {
    byte[] generateRentAgreementPdf(Lease lease);
    byte[] generatePaymentReceiptPdf(Receipt receipt);
    byte[] generateInvoicePdf(Invoice invoice);
    byte[] generateLandlordPayoutPdf(LandlordPayout payout);
}

