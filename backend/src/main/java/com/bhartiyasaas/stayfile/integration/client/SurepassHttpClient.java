package com.bhartiyasaas.stayfile.integration.client;

import com.bhartiyasaas.stayfile.exception.IntegrationException;
import com.bhartiyasaas.stayfile.exception.ProviderUnavailableException;
import com.bhartiyasaas.stayfile.service.IntegrationLogService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
public class SurepassHttpClient {

    private static final String PROVIDER = "SUREPASS";

    private final RestClient restClient;
    private final IntegrationLogService integrationLogService;
    private final ObjectMapper objectMapper;
    private final String token;
    private final String baseUrl;

    public SurepassHttpClient(
            RestClient restClient,
            IntegrationLogService integrationLogService,
            ObjectMapper objectMapper,
            @Value("${stayfile.kyc.surepass.token:}") String token,
            @Value("${stayfile.kyc.surepass.base-url:https://sandbox.surepass.io}") String baseUrl) {
        this.restClient = restClient;
        this.integrationLogService = integrationLogService;
        this.objectMapper = objectMapper;
        this.token = token != null ? token.trim() : "";
        this.baseUrl = baseUrl != null ? baseUrl.replaceAll("/+$", "") : "https://sandbox.surepass.io";
    }

    public <T> T post(String relativePath, Object requestBody, Class<T> responseType, UUID organizationId) {
        validateCredentials();
        String fullUrl = buildUrl(relativePath);
        String requestJson = toJson(requestBody);
        long startTime = System.currentTimeMillis();

        try {
            log.debug("Calling Surepass POST endpoint: {}", fullUrl);

            String responseBody = restClient.post()
                    .uri(fullUrl)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestJson != null ? requestJson : "{}")
                    .retrieve()
                    .body(String.class);

            int latencyMs = (int) (System.currentTimeMillis() - startTime);
            integrationLogService.log(organizationId, PROVIDER, fullUrl, requestJson, responseBody, 200, latencyMs, null);

            return fromJson(responseBody, responseType);
        } catch (RestClientResponseException ex) {
            int latencyMs = (int) (System.currentTimeMillis() - startTime);
            String errorBody = ex.getResponseBodyAsString();
            String userMsg = extractErrorMessage(errorBody, ex.getStatusText());

            integrationLogService.log(organizationId, PROVIDER, fullUrl, requestJson, errorBody, ex.getStatusCode().value(), latencyMs, userMsg);
            log.error("Surepass POST '{}' failed [HTTP {}]: {}", fullUrl, ex.getStatusCode().value(), userMsg);

            throw new IntegrationException(PROVIDER, ex.getStatusCode().value(), errorBody, userMsg);
        } catch (Exception ex) {
            int latencyMs = (int) (System.currentTimeMillis() - startTime);
            integrationLogService.log(organizationId, PROVIDER, fullUrl, requestJson, null, 502, latencyMs, ex.getMessage());
            log.error("Surepass connection error calling '{}': {}", fullUrl, ex.getMessage());

            throw new IntegrationException(PROVIDER, "Unable to communicate with Surepass: " + ex.getMessage(), ex);
        }
    }

    public <T> T get(String relativePath, Map<String, String> queryParams, Class<T> responseType, UUID organizationId) {
        validateCredentials();
        URI uri = buildUri(relativePath, queryParams);
        String fullUrl = uri.toString();
        long startTime = System.currentTimeMillis();

        try {
            log.debug("Calling Surepass GET endpoint: {}", fullUrl);

            String responseBody = restClient.get()
                    .uri(uri)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                    .retrieve()
                    .body(String.class);

            int latencyMs = (int) (System.currentTimeMillis() - startTime);
            integrationLogService.log(organizationId, PROVIDER, fullUrl, null, responseBody, 200, latencyMs, null);

            return fromJson(responseBody, responseType);
        } catch (RestClientResponseException ex) {
            int latencyMs = (int) (System.currentTimeMillis() - startTime);
            String errorBody = ex.getResponseBodyAsString();
            String userMsg = extractErrorMessage(errorBody, ex.getStatusText());

            integrationLogService.log(organizationId, PROVIDER, fullUrl, null, errorBody, ex.getStatusCode().value(), latencyMs, userMsg);
            log.error("Surepass GET '{}' failed [HTTP {}]: {}", fullUrl, ex.getStatusCode().value(), userMsg);

            throw new IntegrationException(PROVIDER, ex.getStatusCode().value(), errorBody, userMsg);
        } catch (Exception ex) {
            int latencyMs = (int) (System.currentTimeMillis() - startTime);
            integrationLogService.log(organizationId, PROVIDER, fullUrl, null, null, 502, latencyMs, ex.getMessage());
            log.error("Surepass connection error calling '{}': {}", fullUrl, ex.getMessage());

            throw new IntegrationException(PROVIDER, "Unable to communicate with Surepass: " + ex.getMessage(), ex);
        }
    }

    private void validateCredentials() {
        if (token.isEmpty()) {
            throw new ProviderUnavailableException("Surepass API credentials are not configured. Please supply SUREPASS_TOKEN.");
        }
    }

    private String buildUrl(String relativePath) {
        String path = relativePath != null ? relativePath.replaceAll("^/+", "") : "";
        return baseUrl + "/" + path;
    }

    private URI buildUri(String relativePath, Map<String, String> queryParams) {
        String url = buildUrl(relativePath);
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(url);
        if (queryParams != null) {
            queryParams.forEach(builder::queryParam);
        }
        return builder.build().encode().toUri();
    }

    private String toJson(Object obj) {
        if (obj == null) return null;
        if (obj instanceof String s) return s;
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            return obj.toString();
        }
    }

    private <T> T fromJson(String json, Class<T> targetClass) {
        if (json == null || json.trim().isEmpty()) return null;
        if (targetClass.equals(String.class)) {
            return targetClass.cast(json);
        }
        try {
            return objectMapper.readValue(json, targetClass);
        } catch (Exception e) {
            throw new IntegrationException(PROVIDER, 500, json, "Failed to parse Surepass response: " + e.getMessage());
        }
    }

    private String extractErrorMessage(String responseBody, String defaultMsg) {
        if (responseBody == null || responseBody.trim().isEmpty()) {
            return defaultMsg != null ? defaultMsg : "Unknown error";
        }
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            if (root.has("message")) {
                return root.get("message").asText();
            }
            if (root.has("error")) {
                JsonNode err = root.get("error");
                if (err.has("message")) return err.get("message").asText();
                return err.asText();
            }
        } catch (Exception ignored) {
        }
        return responseBody.length() > 200 ? responseBody.substring(0, 200) + "..." : responseBody;
    }
}
