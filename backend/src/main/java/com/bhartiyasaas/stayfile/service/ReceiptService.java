package com.bhartiyasaas.stayfile.service;

import com.bhartiyasaas.stayfile.dto.request.ReceiptCreateRequest;
import com.bhartiyasaas.stayfile.dto.response.ReceiptResponse;
import com.bhartiyasaas.stayfile.security.SecurityUser;

import java.util.List;
import java.util.UUID;

public interface ReceiptService {
    ReceiptResponse createReceipt(ReceiptCreateRequest request, SecurityUser currentUser);
    ReceiptResponse getReceiptById(UUID id, SecurityUser currentUser);
    List<ReceiptResponse> getReceiptsByOrganization(SecurityUser currentUser);
    byte[] getReceiptPdf(UUID receiptId, SecurityUser currentUser);
}
