package com.bhartiyasaas.stayfile.service.impl;

import com.bhartiyasaas.stayfile.entity.IntegrationLog;
import com.bhartiyasaas.stayfile.entity.Organization;
import com.bhartiyasaas.stayfile.repository.IntegrationLogRepository;
import com.bhartiyasaas.stayfile.repository.OrganizationRepository;
import com.bhartiyasaas.stayfile.service.IntegrationLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class IntegrationLogServiceImpl implements IntegrationLogService {

    private final IntegrationLogRepository integrationLogRepository;
    private final OrganizationRepository organizationRepository;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void log(UUID organizationId, String provider, String endpoint, String requestPayload, String responsePayload, Integer statusCode, Integer executionTimeMs, String errorMessage) {
        try {
            Organization org = null;
            if (organizationId != null) {
                org = organizationRepository.findById(organizationId).orElse(null);
            }

            IntegrationLog entry = IntegrationLog.builder()
                    .organization(org)
                    .provider(provider)
                    .endpoint(endpoint)
                    .requestPayload(requestPayload)
                    .responsePayload(responsePayload)
                    .statusCode(statusCode)
                    .executionTimeMs(executionTimeMs)
                    .errorMessage(errorMessage)
                    .build();

            integrationLogRepository.save(entry);
            log.debug("Logged integration call to provider '{}' endpoint '{}' with status {}", provider, endpoint, statusCode);
        } catch (Exception e) {
            log.error("Failed to persist integration log for provider '{}' endpoint '{}': {}", provider, endpoint, e.getMessage());
        }
    }
}
