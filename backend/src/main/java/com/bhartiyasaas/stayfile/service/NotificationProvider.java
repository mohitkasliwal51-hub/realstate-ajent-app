package com.bhartiyasaas.stayfile.service;

import java.util.Map;
import java.util.UUID;

public interface NotificationProvider {
    /**
     * Sends a WhatsApp template notification.
     */
    boolean sendWhatsappMessage(UUID organizationId, UUID tenantId, String recipientPhone, String templateName, Map<String, String> parameters);

    /**
     * Sends a PDF document attachment via WhatsApp.
     */
    boolean sendWhatsappPdfDocument(UUID organizationId, UUID tenantId, String recipientPhone, String documentUrl, String caption);

    /**
     * Syncs a newly added property listing to Meta WhatsApp Business Catalog.
     */
    boolean syncPropertyToWhatsappCatalog(UUID propertyId, String name, double monthlyRent, String imageUrl, String propertyUrl);
}
