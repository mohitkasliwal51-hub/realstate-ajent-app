package com.bhartiyasaas.stayfile.service.impl;

import com.bhartiyasaas.stayfile.dto.response.FileUploadResponse;
import com.bhartiyasaas.stayfile.entity.enums.FileCategory;
import com.bhartiyasaas.stayfile.exception.BadRequestException;
import com.bhartiyasaas.stayfile.exception.ProviderUnavailableException;
import com.bhartiyasaas.stayfile.exception.ResourceNotFoundException;
import com.bhartiyasaas.stayfile.service.StorageService;
import com.bhartiyasaas.stayfile.service.storage.StorageKeys;
import com.bhartiyasaas.stayfile.service.storage.UploadValidator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@ConditionalOnProperty(name = "app.storage.provider", havingValue = "supabase")
public class SupabaseStorageServiceImpl implements StorageService {

    private final RestClient restClient;
    private final UploadValidator uploadValidator;
    private final String supabaseUrl;
    private final String supabaseKey;
    private final String bucketName;

    public SupabaseStorageServiceImpl(
            RestClient restClient,
            UploadValidator uploadValidator,
            @Value("${app.storage.supabase.url:}") String supabaseUrl,
            @Value("${app.storage.supabase.key:}") String supabaseKey,
            @Value("${app.storage.supabase.bucket:stayfile}") String bucketName) {
        this.restClient = restClient;
        this.uploadValidator = uploadValidator;
        this.supabaseUrl = supabaseUrl != null ? supabaseUrl.replaceAll("/+$", "") : "";
        this.supabaseKey = supabaseKey;
        this.bucketName = bucketName != null && !bucketName.trim().isEmpty() ? bucketName : "stayfile";
    }

    @Override
    public FileUploadResponse storeFile(MultipartFile file, FileCategory category, UUID organizationId) {
        validateConfig();
        UploadValidator.DetectedType detected = uploadValidator.validate(file, category);
        String key = StorageKeys.newKey(category, organizationId, detected.extension());

        try {
            byte[] bytes = file.getBytes();
            String uploadUrl = String.format("%s/storage/v1/object/%s/%s", supabaseUrl, bucketName, key);

            restClient.post()
                    .uri(uploadUrl)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + supabaseKey)
                    .header("apikey", supabaseKey)
                    .contentType(MediaType.parseMediaType(detected.contentType()))
                    .body(bytes)
                    .retrieve()
                    .toBodilessEntity();

            String fileUrl = category.isPublicAccess()
                    ? String.format("%s/storage/v1/object/public/%s/%s", supabaseUrl, bucketName, key)
                    : "/api/v1/files/private/" + key;

            return FileUploadResponse.builder()
                    .url(fileUrl)
                    .key(key)
                    .fileName(file.getOriginalFilename())
                    .fileType(detected.contentType())
                    .size(file.getSize())
                    .isPublic(category.isPublicAccess())
                    .build();
        } catch (IOException e) {
            throw new BadRequestException("Failed to read file bytes for upload: " + e.getMessage());
        } catch (Exception e) {
            log.error("Failed to upload file to Supabase Storage: {}", e.getMessage());
            throw new ProviderUnavailableException("Failed to upload file to cloud storage: " + e.getMessage());
        }
    }

    @Override
    public Resource loadFileAsResource(String key, UUID organizationId) {
        validateConfig();
        StorageKeys.requireValid(key);

        if (!StorageKeys.belongsToOrganization(key, organizationId)) {
            throw new AccessDeniedException("File does not belong to organization");
        }

        try {
            String downloadUrl = String.format("%s/storage/v1/object/authenticated/%s/%s", supabaseUrl, bucketName, key);

            byte[] bytes = restClient.get()
                    .uri(downloadUrl)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + supabaseKey)
                    .header("apikey", supabaseKey)
                    .retrieve()
                    .body(byte[].class);

            if (bytes == null) {
                throw new ResourceNotFoundException("File not found in storage");
            }

            return new ByteArrayResource(bytes) {
                @Override
                public String getFilename() {
                    return key.substring(key.lastIndexOf('/') + 1);
                }
            };
        } catch (Exception e) {
            log.error("Failed to download file from Supabase Storage: {}", e.getMessage());
            throw new ResourceNotFoundException("File not found or unavailable in cloud storage");
        }
    }

    @Override
    public void deleteFile(String key, UUID organizationId) {
        validateConfig();
        StorageKeys.requireValid(key);

        if (!StorageKeys.belongsToOrganization(key, organizationId)) {
            throw new AccessDeniedException("File does not belong to organization");
        }

        try {
            String deleteUrl = String.format("%s/storage/v1/object/%s", supabaseUrl, bucketName);

            restClient.method(HttpMethod.DELETE)
                    .uri(deleteUrl)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + supabaseKey)
                    .header("apikey", supabaseKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("prefixes", List.of(key)))
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception e) {
            log.error("Failed to delete file from Supabase Storage: {}", e.getMessage());
        }
    }

    private void validateConfig() {
        if (supabaseUrl == null || supabaseUrl.trim().isEmpty() || supabaseKey == null || supabaseKey.trim().isEmpty()) {
            throw new ProviderUnavailableException("Supabase Storage is enabled but SUPABASE_URL and SUPABASE_SERVICE_ROLE_KEY are not configured.");
        }
    }
}
