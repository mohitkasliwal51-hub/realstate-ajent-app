package com.bhartiyasaas.stayfile.service.impl;

import com.bhartiyasaas.stayfile.exception.ProviderUnavailableException;
import com.bhartiyasaas.stayfile.service.PaymentGatewayProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;

@Service
public class CashfreePaymentGatewayServiceImpl implements PaymentGatewayProvider {

    @Value("${stayfile.payment.cashfree.client-id:}")
    private String clientId;

    @Value("${stayfile.payment.cashfree.client-secret:}")
    private String clientSecret;

    @Value("${stayfile.payment.cashfree.env:sandbox}")
    private String environment;

    @Override
    public String createPaymentLink(UUID invoiceId, BigDecimal amount, String customerName, String customerPhone, String customerEmail, String description) {
        if (clientId == null || clientId.trim().isEmpty() || clientSecret == null || clientSecret.trim().isEmpty()) {
            throw new ProviderUnavailableException("Cashfree Payment Gateway API credentials are not configured.");
        }
        throw new ProviderUnavailableException("Cashfree Payment Gateway integration requires live client credentials.");
    }

    @Override
    public boolean verifyWebhookSignature(String rawPayload, String signatureHeader) {
        if (clientSecret == null || clientSecret.trim().isEmpty()) {
            return false; // Safely fail signature check if secret is unconfigured
        }
        return false;
    }
}
