package com.bhartiyasaas.stayfile.controller;

import com.bhartiyasaas.stayfile.entity.enums.PaymentMode;
import com.bhartiyasaas.stayfile.integration.dto.cashfree.CashfreePaymentWebhookPayload;
import com.bhartiyasaas.stayfile.service.PaymentGatewayProvider;
import com.bhartiyasaas.stayfile.service.ReceiptService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/webhooks/cashfree")
@RequiredArgsConstructor
public class PaymentWebhookController {

    private final PaymentGatewayProvider paymentGatewayProvider;
    private final ReceiptService receiptService;
    private final ObjectMapper objectMapper;

    @PostMapping("/payment")
    public ResponseEntity<Void> handlePayment(
            @RequestHeader(value = "x-webhook-timestamp", required = false) String timestamp,
            @RequestHeader(value = "x-webhook-signature", required = false) String signature,
            HttpServletRequest request) {
        String rawPayload;
        try {
            rawPayload = new String(request.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        } catch (Exception ex) {
            log.error("Failed to read Cashfree payment webhook body", ex);
            return ResponseEntity.status(400).build();
        }
        if (!paymentGatewayProvider.verifyWebhookSignature(timestamp, rawPayload, signature)) {
            log.warn("Rejected Cashfree payment webhook with invalid signature");
            return ResponseEntity.status(401).build();
        }

        try {
            CashfreePaymentWebhookPayload payload =
                    objectMapper.readValue(rawPayload, CashfreePaymentWebhookPayload.class);
            CashfreePaymentWebhookPayload.Data data = payload.getData();
            if (data == null) {
                return ResponseEntity.ok().build();
            }

            String status = data.getLinkStatus();
            if (data.getOrder() != null && data.getOrder().getTransactionStatus() != null) {
                status = data.getOrder().getTransactionStatus();
            } else if (data.getPayment() != null && data.getPayment().getPaymentStatus() != null) {
                status = data.getPayment().getPaymentStatus();
            }
            boolean success = "SUCCESS".equalsIgnoreCase(status)
                    || "PAID".equalsIgnoreCase(status)
                    || "PARTIALLY_PAID".equalsIgnoreCase(status);
            if (!success) {
                return ResponseEntity.ok().build();
            }

            String linkId = data.getLinkId();
            if (linkId == null && data.getOrder() != null) {
                linkId = data.getOrder().getOrderId();
            }
            if (linkId == null || !linkId.startsWith("INV_")) {
                log.warn("Cashfree webhook has no recognizable invoice link_id");
                return ResponseEntity.ok().build();
            }

            UUID invoiceId = UUID.fromString(linkId.substring("INV_".length()));
            BigDecimal amount = data.getLinkAmountPaid();
            if (amount == null && data.getPayment() != null) {
                amount = data.getPayment().getPaymentAmount();
            }
            if (amount == null) {
                amount = data.getPaymentAmount();
            }
            if (amount == null && data.getOrder() != null) {
                amount = data.getOrder().getOrderAmount();
            }
            if (amount == null) {
                return ResponseEntity.ok().build();
            }

            String cfPaymentId = data.getOrder() != null
                    ? data.getOrder().getTransactionId() : null;
            if (cfPaymentId == null && data.getPayment() != null) {
                cfPaymentId = data.getPayment().getCfPaymentId();
            }
            if (cfPaymentId == null) {
                cfPaymentId = data.getCfPaymentId();
            }
            if (cfPaymentId == null && data.getOrder() != null) {
                cfPaymentId = data.getOrder().getOrderId();
            }
            if (cfPaymentId == null) {
                cfPaymentId = data.getLinkId();
            }
            if (cfPaymentId == null) {
                cfPaymentId = "INV_" + invoiceId;
            }
            receiptService.recordWebhookReceipt(
                    invoiceId, amount, inferPaymentMode(data), cfPaymentId,
                    LocalDate.now(), "Cashfree payment webhook");
        } catch (Exception ex) {
            log.error("Failed to process Cashfree payment webhook", ex);
            return ResponseEntity.status(500).build();
        }
        return ResponseEntity.ok().build();
    }

    private PaymentMode inferPaymentMode(CashfreePaymentWebhookPayload.Data data) {
        if (data.getPayment() == null || data.getPayment().getPaymentMethod() == null) {
            return PaymentMode.UPI;
        }
        String method = data.getPayment().getPaymentMethod().toString().toUpperCase();
        if (method.contains("CARD")) return PaymentMode.CREDIT_CARD;
        if (method.contains("NET")) return PaymentMode.NET_BANKING;
        return PaymentMode.UPI;
    }
}
