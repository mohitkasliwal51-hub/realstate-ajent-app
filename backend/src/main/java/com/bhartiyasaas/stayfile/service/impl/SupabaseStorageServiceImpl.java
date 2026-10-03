package com.bhartiyasaas.stayfile.service.impl;

import com.bhartiyasaas.stayfile.dto.response.FileUploadResponse;
import com.bhartiyasaas.stayfile.entity.enums.FileCategory;
import com.bhartiyasaas.stayfile.exception.ProviderUnavailableException;
import com.bhartiyasaas.stayfile.service.StorageService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@Service
@ConditionalOnProperty(name = "app.storage.provider", havingValue = "supabase")
public class SupabaseStorageServiceImpl implements StorageService {

    @Value("${app.storage.supabase.url:}")
    private String supabaseUrl;

    @Value("${app.storage.supabase.key:}")
    private String supabaseKey;

    @Override
    public FileUploadResponse storeFile(MultipartFile file, FileCategory category, UUID organizationId) {
        if (supabaseUrl == null || supabaseUrl.trim().isEmpty() || supabaseKey == null || supabaseKey.trim().isEmpty()) {
            throw new ProviderUnavailableException("Supabase Storage provider is selected, but SUPABASE_URL and SUPABASE_SERVICE_ROLE_KEY are not configured.");
        }
        throw new ProviderUnavailableException("Supabase Storage integration requires active Supabase API credentials.");
    }

    @Override
    public Resource loadFileAsResource(String key, UUID organizationId) {
        throw new ProviderUnavailableException("Supabase Storage files are served via Supabase CDN URLs.");
    }

    @Override
    public void deleteFile(String key, UUID organizationId) {
        throw new ProviderUnavailableException("Supabase Storage integration requires active Supabase API credentials.");
    }
}
