package com.bhartiyasaas.stayfile.service.impl;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bhartiyasaas.stayfile.entity.Organization;
import com.bhartiyasaas.stayfile.entity.Tenant;
import com.bhartiyasaas.stayfile.entity.WhatsappLog;
import com.bhartiyasaas.stayfile.entity.enums.WhatsappDirection;
import com.bhartiyasaas.stayfile.entity.enums.WhatsappMsgType;
import com.bhartiyasaas.stayfile.entity.enums.WhatsappStatus;
import com.bhartiyasaas.stayfile.exception.ResourceNotFoundException;
import com.bhartiyasaas.stayfile.repository.OrganizationRepository;
import com.bhartiyasaas.stayfile.repository.TenantRepository;
import com.bhartiyasaas.stayfile.repository.WhatsappLogRepository;
import com.bhartiyasaas.stayfile.service.WhatsappService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class WhatsappServiceImpl implements WhatsappService {

    private final WhatsappLogRepository whatsappLogRepository;
    private final OrganizationRepository organizationRepository;
    private final TenantRepository tenantRepository;

    @Override
    @Transactional
    public void sendTemplateMessage(UUID organizationId, UUID tenantId, String recipientPhone, String templateName, String messageBody) {
        Organization org = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found"));

        Tenant tenant = null;
        if (tenantId != null) {
            tenant = tenantRepository.findByIdAndOrganizationId(tenantId, organizationId).orElse(null);
        }

        log.info("Simulating Meta WhatsApp Cloud API template message '{}' to {}", templateName, recipientPhone);

        WhatsappLog logEntry = WhatsappLog.builder()
                .organization(org)
                .tenant(tenant)
                .phoneNumber(recipientPhone)
                .direction(WhatsappDirection.OUTBOUND)
                .type(WhatsappMsgType.TEMPLATE)
                .templateName(templateName)
                .messageBody(messageBody)
                .status(WhatsappStatus.SENT)
                .wamid("wam_mock_" + UUID.randomUUID())
                .build();

        whatsappLogRepository.save(logEntry);
    }
}
