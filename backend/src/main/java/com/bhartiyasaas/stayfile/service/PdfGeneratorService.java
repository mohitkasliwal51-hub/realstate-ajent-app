package com.bhartiyasaas.stayfile.service;

import com.bhartiyasaas.stayfile.entity.Lease;
import com.bhartiyasaas.stayfile.entity.Receipt;

public interface PdfGeneratorService {
    byte[] generateRentAgreementPdf(Lease lease);
    byte[] generatePaymentReceiptPdf(Receipt receipt);
}
