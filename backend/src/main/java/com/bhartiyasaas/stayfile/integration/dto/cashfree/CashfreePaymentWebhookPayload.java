package com.bhartiyasaas.stayfile.integration.dto.cashfree;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CashfreePaymentWebhookPayload {
    @JsonProperty("data")
    private Data data;

    @lombok.Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Data {
        @JsonProperty("payment")
        private Payment payment;
        @JsonProperty("order")
        private Order order;
        @JsonProperty("link_id")
        private String linkId;
        @JsonProperty("link_status")
        private String linkStatus;
        @JsonProperty("link_amount")
        private BigDecimal linkAmount;
        @JsonProperty("link_amount_paid")
        private BigDecimal linkAmountPaid;
        @JsonProperty("cf_payment_id")
        private String cfPaymentId;
        @JsonProperty("payment_amount")
        private BigDecimal paymentAmount;
        @JsonProperty("payment_status")
        private String paymentStatus;
    }

    @lombok.Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Payment {
        @JsonProperty("cf_payment_id")
        private String cfPaymentId;
        @JsonProperty("payment_amount")
        private BigDecimal paymentAmount;
        @JsonProperty("payment_status")
        private String paymentStatus;
        @JsonProperty("payment_method")
        private Object paymentMethod;
    }

    @lombok.Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Order {
        @JsonProperty("order_id")
        private String orderId;
        @JsonProperty("order_amount")
        private BigDecimal orderAmount;
        @JsonProperty("transaction_id")
        private String transactionId;
        @JsonProperty("transaction_status")
        private String transactionStatus;
    }
}
