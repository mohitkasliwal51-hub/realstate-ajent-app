package com.bhartiyasaas.stayfile.service;

import java.math.BigDecimal;
import java.util.UUID;

public interface PaymentGatewayProvider {
    /**
     * Generates an online payment link (Cashfree / UPI intent).
     */
    String createPaymentLink(UUID invoiceId, BigDecimal amount, String customerName, String customerPhone, String customerEmail, String description);

    /**
     * Verifies payment status from gateway webhook.
     */
    boolean verifyWebhookSignature(String rawPayload, String signatureHeader);
}
