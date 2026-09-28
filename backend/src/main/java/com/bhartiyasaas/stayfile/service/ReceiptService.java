package com.bhartiyasaas.stayfile.service;

import java.util.List;
import java.util.UUID;

import com.bhartiyasaas.stayfile.dto.request.ReceiptCreateRequest;
import com.bhartiyasaas.stayfile.dto.response.ReceiptResponse;

public interface ReceiptService {
    ReceiptResponse createReceipt(ReceiptCreateRequest request);
    ReceiptResponse getReceiptById(UUID id, UUID organizationId);
    List<ReceiptResponse> getReceiptsByOrganization(UUID organizationId);
    byte[] getReceiptPdf(UUID receiptId, UUID organizationId);
}
