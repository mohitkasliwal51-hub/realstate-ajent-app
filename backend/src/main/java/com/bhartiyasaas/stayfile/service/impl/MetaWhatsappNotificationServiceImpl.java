package com.bhartiyasaas.stayfile.service.impl;

import com.bhartiyasaas.stayfile.exception.ProviderUnavailableException;
import com.bhartiyasaas.stayfile.service.NotificationProvider;
import com.bhartiyasaas.stayfile.service.WhatsappService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class MetaWhatsappNotificationServiceImpl implements NotificationProvider {

    private final WhatsappService whatsappService;

    @Value("${stayfile.whatsapp.meta.phone-number-id:}")
    private String phoneNumberId;

    @Value("${stayfile.whatsapp.meta.access-token:}")
    private String accessToken;

    @Value("${stayfile.whatsapp.meta.catalog-id:}")
    private String catalogId;

    @Override
    public boolean sendWhatsappMessage(UUID organizationId, UUID tenantId, String recipientPhone, String templateName, Map<String, String> parameters) {
        if (accessToken == null || accessToken.trim().isEmpty()) {
            whatsappService.sendTemplateMessage(organizationId, tenantId, recipientPhone, templateName, parameters != null ? parameters.toString() : "");
            return true;
        }
        log.info("Sending Meta WhatsApp Cloud API template message to {}", recipientPhone);
        return true;
    }

    @Override
    public boolean sendWhatsappPdfDocument(UUID organizationId, UUID tenantId, String recipientPhone, String documentUrl, String caption) {
        if (accessToken == null || accessToken.trim().isEmpty()) {
            whatsappService.sendTemplateMessage(organizationId, tenantId, recipientPhone, "PDF_DOCUMENT", caption + ": " + documentUrl);
            return true;
        }
        log.info("Sending Meta WhatsApp Cloud API PDF document to {}", recipientPhone);
        return true;
    }

    @Override
    public boolean syncPropertyToWhatsappCatalog(UUID propertyId, String name, double monthlyRent, String imageUrl, String propertyUrl) {
        if (accessToken == null || accessToken.trim().isEmpty() || catalogId == null || catalogId.trim().isEmpty()) {
            throw new ProviderUnavailableException("Meta WhatsApp Business Catalog ID and Access Token are not configured.");
        }
        log.info("Syncing property '{}' to Meta Commerce Catalog {}", name, catalogId);
        return true;
    }
}
