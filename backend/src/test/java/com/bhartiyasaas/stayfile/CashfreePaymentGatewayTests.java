package com.bhartiyasaas.stayfile;

import com.bhartiyasaas.stayfile.service.impl.CashfreePaymentGatewayServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;

class CashfreePaymentGatewayTests {

    @Test
    void verifyWebhookSignature_acceptsKnownHmacVector() throws Exception {
        String secret = "test-secret";
        String timestamp = "1700000000";
        String payload = "{\"type\":\"PAYMENT_SUCCESS_WEBHOOK\",\"data\":{\"payment\":{\"payment_status\":\"SUCCESS\"}}}";

        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        String signature = Base64.getEncoder().encodeToString(
                mac.doFinal((timestamp + payload).getBytes(StandardCharsets.UTF_8)));

        CashfreePaymentGatewayServiceImpl service = new CashfreePaymentGatewayServiceImpl(
                RestClient.builder().build(), "test-client", secret, "sandbox",
                "https://sandbox.cashfree.com/pg", "2023-08-01", "", "");

        assertThat(service.verifyWebhookSignature(timestamp, payload, signature)).isTrue();
        assertThat(service.verifyWebhookSignature(timestamp, payload, "invalid")).isFalse();
    }
}
