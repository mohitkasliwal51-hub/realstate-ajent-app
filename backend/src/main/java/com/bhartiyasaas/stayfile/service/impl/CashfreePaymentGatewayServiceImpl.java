package com.bhartiyasaas.stayfile.service.impl;

import com.bhartiyasaas.stayfile.exception.ProviderUnavailableException;
import com.bhartiyasaas.stayfile.integration.dto.cashfree.CashfreePaymentLinkRequest;
import com.bhartiyasaas.stayfile.integration.dto.cashfree.CashfreePaymentLinkResponse;
import com.bhartiyasaas.stayfile.service.PaymentGatewayProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.math.BigDecimal;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class CashfreePaymentGatewayServiceImpl implements PaymentGatewayProvider {
    private final RestClient restClient;

    private String clientId;
    private String clientSecret;
    private String environment;
    private final String baseUrl;
    private final String apiVersion;
    private final String notifyUrl;
    private final String returnUrl;

    public CashfreePaymentGatewayServiceImpl(
            RestClient restClient,
            @Value("${stayfile.payment.cashfree.client-id:}") String clientId,
            @Value("${stayfile.payment.cashfree.client-secret:}") String clientSecret,
            @Value("${stayfile.payment.cashfree.env:sandbox}") String environment,
            @Value("${stayfile.payment.cashfree.base-url:https://sandbox.cashfree.com/pg}") String baseUrl,
            @Value("${stayfile.payment.cashfree.api-version:2023-08-01}") String apiVersion,
            @Value("${stayfile.payment.cashfree.notify-url:}") String notifyUrl,
            @Value("${stayfile.payment.cashfree.return-url:}") String returnUrl) {
        this.restClient = restClient;
        this.clientId = clientId == null ? "" : clientId.trim();
        this.clientSecret = clientSecret == null ? "" : clientSecret.trim();
        this.environment = environment == null ? "sandbox" : environment.trim();
        this.baseUrl = baseUrl == null ? "https://sandbox.cashfree.com/pg" : baseUrl.replaceAll("/+$", "");
        this.apiVersion = apiVersion == null || apiVersion.isBlank() ? "2023-08-01" : apiVersion.trim();
        this.notifyUrl = notifyUrl == null ? "" : notifyUrl.trim();
        this.returnUrl = returnUrl == null ? "" : returnUrl.trim();
    }

    @Override
    public String createPaymentLink(UUID invoiceId, BigDecimal amount, String customerName, String customerPhone, String customerEmail, String description) {
        if (clientId.isBlank() || clientSecret.isBlank()) {
            return "https://payments-test.cashfree.com/links/mock_" + invoiceId;
        }

        Map<String, String> linkMeta = new LinkedHashMap<>();
        if (!notifyUrl.isBlank()) linkMeta.put("notify_url", notifyUrl);
        if (!returnUrl.isBlank()) linkMeta.put("return_url", returnUrl);

        CashfreePaymentLinkRequest request = CashfreePaymentLinkRequest.builder()
                .customerDetails(CashfreePaymentLinkRequest.CustomerDetails.builder()
                        .customerName(customerName)
                        .customerPhone(customerPhone)
                        .customerEmail(customerEmail)
                        .build())
                .linkAmount(amount)
                .linkCurrency("INR")
                .linkId("INV_" + invoiceId)
                .linkPurpose(description)
                .linkMeta(linkMeta)
                .linkPartialPayments(false)
                .build();

        try {
            CashfreePaymentLinkResponse response = restClient.post()
                    .uri(baseUrl + "/links")
                    .header("x-client-id", clientId)
                    .header("x-client-secret", clientSecret)
                    .header("x-api-version", apiVersion)
                    .header("x-idempotency-key",
                            UUID.nameUUIDFromBytes(("INV_" + invoiceId).getBytes(StandardCharsets.UTF_8)).toString())
                    .accept(MediaType.APPLICATION_JSON)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(CashfreePaymentLinkResponse.class);
            if (response == null || response.getLinkUrl() == null || response.getLinkUrl().isBlank()) {
                throw new ProviderUnavailableException("Cashfree returned no payment link URL.");
            }
            return response.getLinkUrl();
        } catch (RestClientResponseException ex) {
            if (ex.getStatusCode().value() == 409) {
                try {
                    CashfreePaymentLinkResponse existing = restClient.get()
                            .uri(baseUrl + "/links/" + "INV_" + invoiceId)
                            .header("x-client-id", clientId)
                            .header("x-client-secret", clientSecret)
                            .header("x-api-version", apiVersion)
                            .accept(MediaType.APPLICATION_JSON)
                            .retrieve()
                            .body(CashfreePaymentLinkResponse.class);
                    if (existing != null && existing.getLinkUrl() != null
                            && !existing.getLinkUrl().isBlank()) {
                        return existing.getLinkUrl();
                    }
                } catch (RestClientResponseException getEx) {
                    throw new ProviderUnavailableException(
                            "Cashfree returned 409 for an existing link and retrieving it failed: "
                                    + getEx.getMessage(), getEx);
                } catch (Exception getEx) {
                    throw new ProviderUnavailableException(
                            "Unable to retrieve existing Cashfree payment link: "
                                    + getEx.getMessage(), getEx);
                }
            }
            throw new ProviderUnavailableException("Cashfree Payment Link creation failed: " + ex.getMessage(), ex);
        } catch (Exception ex) {
            throw new ProviderUnavailableException("Unable to communicate with Cashfree: " + ex.getMessage(), ex);
        }
    }

    @Override
    public boolean verifyWebhookSignature(String timestamp, String rawPayload, String signatureHeader) {
        if (clientSecret.isBlank() || timestamp == null || timestamp.isBlank()
                || rawPayload == null || signatureHeader == null || signatureHeader.isBlank()) {
            return false;
        }
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(clientSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            String expected = Base64.getEncoder().encodeToString(
                    mac.doFinal((timestamp + rawPayload).getBytes(StandardCharsets.UTF_8)));
            return java.security.MessageDigest.isEqual(
                    expected.getBytes(StandardCharsets.UTF_8),
                    signatureHeader.trim().getBytes(StandardCharsets.UTF_8));
        } catch (Exception ex) {
            return false;
        }
    }
}
