package com.bhartiyasaas.stayfile.service;

import java.util.UUID;

public interface IntegrationLogService {
    void log(UUID organizationId, String provider, String endpoint, String requestPayload, String responsePayload, Integer statusCode, Integer executionTimeMs, String errorMessage);
}
