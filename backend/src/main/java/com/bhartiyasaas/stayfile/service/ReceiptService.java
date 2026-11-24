package com.bhartiyasaas.stayfile.service;

import com.bhartiyasaas.stayfile.dto.request.ReceiptCreateRequest;
import com.bhartiyasaas.stayfile.dto.response.ReceiptResponse;
import com.bhartiyasaas.stayfile.security.SecurityUser;
import com.bhartiyasaas.stayfile.entity.enums.PaymentMode;

import java.util.List;
import java.util.UUID;
import java.math.BigDecimal;
import java.time.LocalDate;

public interface ReceiptService {
    ReceiptResponse createReceipt(ReceiptCreateRequest request, SecurityUser currentUser);
    ReceiptResponse getReceiptById(UUID id, SecurityUser currentUser);
    List<ReceiptResponse> getReceiptsForCurrentUser(SecurityUser currentUser);
    List<ReceiptResponse> getReceiptsByOrganization(SecurityUser currentUser);
    byte[] getReceiptPdf(UUID receiptId, SecurityUser currentUser);
    void recordWebhookReceipt(UUID invoiceId, BigDecimal amount, PaymentMode paymentMode,
                              String transactionReference, LocalDate paymentDate, String notes);
}
