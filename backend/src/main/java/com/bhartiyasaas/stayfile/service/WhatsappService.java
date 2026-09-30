package com.bhartiyasaas.stayfile.service;

import java.util.UUID;

public interface WhatsappService {
    void sendTemplateMessage(UUID organizationId, UUID tenantId, String recipientPhone, String templateName, String messageBody);
}
